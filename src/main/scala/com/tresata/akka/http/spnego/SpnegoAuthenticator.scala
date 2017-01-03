package com.tresata.akka.http.spnego

import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.security.{ PrivilegedAction, PrivilegedExceptionAction, PrivilegedActionException }
import javax.security.auth.Subject
import javax.security.auth.login.LoginContext
import javax.security.auth.kerberos.KerberosPrincipal
import scala.concurrent.{ ExecutionContext, Future }
import scala.collection.JavaConverters._

import com.typesafe.config.{ Config, ConfigFactory }

import org.ietf.jgss.{ GSSManager, GSSCredential }

import akka.event.LoggingAdapter

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.{ Cookie, HttpCookie, HttpChallenge, RawHeader }
import akka.http.scaladsl.server.{ Directive0, Directive1, RequestContext, Rejection, AuthenticationFailedRejection, MalformedHeaderRejection }
import akka.http.scaladsl.server.AuthenticationFailedRejection.{ CredentialsMissing, CredentialsRejected }
import akka.http.scaladsl.server.directives.CookieDirectives.setCookie
import akka.http.scaladsl.server.directives.BasicDirectives.{ extractExecutionContext, extractLog, extract, provide }
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.http.scaladsl.server.directives.FutureDirectives.onSuccess

import org.apache.commons.codec.binary.Base64

object SpnegoAuthenticator {
  private val cookieName = "akka.http.spnego"
  private val Authorization = "authorization"
  private val negotiate = "Negotiate"
  private val wwwAuthenticate = "WWW-Authenticate"

  def apply(config: Config = ConfigFactory.load())(implicit ec: ExecutionContext, log: LoggingAdapter): SpnegoAuthenticator = {
    val principal = config.getString("tresata.akka.http.spnego.kerberos.principal")
    val keytab = config.getString("tresata.akka.http.spnego.kerberos.keytab")
    val debug = config.getBoolean("tresata.akka.http.spnego.kerberos.debug")
    val domain = Some(config.getString("tresata.akka.http.spnego.cookie.domain")).flatMap{ x => if (x == "") None else Some(x) }
    val path = Some(config.getString("tresata.akka.http.spnego.cookie.path")).flatMap{ x => if (x == "") None else Some(x) }
    val tokenValidity = config.getDuration("tresata.akka.http.spnego.token.validity")
    val signatureSecret = config.getString("tresata.akka.http.spnego.signature.secret")

    log.info("principal {}", principal)
    log.info("keytab {}", keytab)
    log.info("debug {}", debug)
    log.info("domain {}", domain)
    log.info("path {}", path)
    log.info("token validity {}", tokenValidity)

    val tokens = new Tokens(tokenValidity.toMillis, signatureSecret.getBytes(UTF_8))
    new SpnegoAuthenticator(principal, keytab, debug, domain, path, tokens)
  }

  def spnegoAuthenticate(config: Config = ConfigFactory.load()): Directive1[Token] = {
    extractExecutionContext.flatMap{ implicit ec =>
      extractLog.flatMap{ implicit log =>
        log.debug("creating spnego authenticator")
        val spnego: SpnegoAuthenticator = SpnegoAuthenticator(config)
        extract(ctx => Future(spnego.apply(ctx))).flatMap(onSuccess(_)).flatMap{
          case Left(rejection) => reject(rejection)
          case Right(token) => provide(token) & spnego.setSpnegoCookie(token)
        }
      }
    }
  }
}

class SpnegoAuthenticator(principal: String, keytab: String, debug: Boolean, domain: Option[String], path: Option[String], tokens: Tokens)(implicit log: LoggingAdapter) {
  import SpnegoAuthenticator._

  private val subject = new Subject(false, Set(new KerberosPrincipal(principal)).asJava, Set.empty[AnyRef].asJava, Set.empty[AnyRef].asJava)
  private val kerberosConfiguration = new KerberosConfiguration(keytab, principal, debug)
  
  private val loginContext = new LoginContext("", subject, null, kerberosConfiguration)
  loginContext.login()

  private val gssManager = Subject.doAs(loginContext.getSubject, new PrivilegedAction[GSSManager] {
    override def run: GSSManager = GSSManager.getInstance
  })

  private def cookieToken(ctx: RequestContext): Option[Either[Rejection, Token]] = try {
    ctx.request.headers.collectFirst{
      case Cookie(cookies) => cookies.find(_.name == cookieName).map{ cookie => log.debug("cookie found"); cookie }
    }.flatten.flatMap{ cookie =>
      Some(tokens.parse(cookie.value)).filter(!_.expired).map{ token => log.debug("spnego token inside cookie not expired"); token }
    }.map(Right(_))
  } catch {
    case e: TokenParseException => Some(Left(MalformedHeaderRejection(s"Cookie: ${cookieName}", e.getMessage, Some(e)))) // malformed token in cookie
  }

  private def clientToken(ctx: RequestContext): Option[Array[Byte]] = ctx.request.headers.collectFirst{
    case HttpHeader(Authorization, value) => value
  }.filter(_.startsWith(negotiate)).map{ authHeader => 
    log.debug("authorization header found")
    new Base64(0).decode(authHeader.substring(negotiate.length).trim)
  }

  private def challengeHeader(maybeServerToken: Option[Array[Byte]] = None): HttpHeader = RawHeader(
    wwwAuthenticate,
    negotiate + maybeServerToken.map(" " + new Base64(0).encodeToString(_)).getOrElse("")
  )

  private def kerberosCore(clientToken: Array[Byte]): Either[Rejection, Token] = {
    try {
      val (maybeServerToken, maybeToken) = Subject.doAs(loginContext.getSubject, new PrivilegedExceptionAction[(Option[Array[Byte]], Option[Token])] {
        override def run: (Option[Array[Byte]], Option[Token]) = {
          val gssContext = gssManager.createContext(null: GSSCredential)
          try {
            (
              Option(gssContext.acceptSecContext(clientToken, 0, clientToken.length)),
              if (gssContext.isEstablished) Some(tokens.create(gssContext.getSrcName.toString)) else None
            )
          } catch {
            case e: Throwable =>
              log.error(e, "error in establishing security context")
              throw e
          } finally {
            gssContext.dispose()
          }
        }
      })
      if (log.isDebugEnabled)
        log.debug("maybeServerToken {} maybeToken {}", maybeServerToken.map(new Base64(0).encodeToString(_)), maybeToken)
      maybeToken.map{ token => 
        log.debug("received new token")
        Right(token) 
      }.getOrElse{
        log.debug("no token received but if there is a serverToken then negotiations are ongoing")
        Left(AuthenticationFailedRejection(CredentialsMissing, HttpChallenge(challengeHeader(maybeServerToken).value, null)))
      }
    } catch {
      case e: PrivilegedActionException => e.getException match {
        case e: IOException => throw e // server error
        case e: Throwable =>
          log.error(e, "negotiation failed")
          Left(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge(challengeHeader().value, null))) // rejected
      }
    }
  }

  private def kerberosNegotiate(ctx: RequestContext): Option[Either[Rejection, Token]] = clientToken(ctx).map(kerberosCore)

  private def initiateNegotiations: Either[Rejection, Token] = {
    log.debug("no negotiation header found, initiating negotiations")
    Left(AuthenticationFailedRejection(CredentialsMissing, HttpChallenge(challengeHeader().value, null)))
  }

  def apply(ctx: RequestContext): Either[Rejection, Token] = cookieToken(ctx).orElse(kerberosNegotiate(ctx)).getOrElse(initiateNegotiations)

  def setSpnegoCookie(token: Token): Directive0 = setCookie(HttpCookie(cookieName, tokens.serialize(token), domain = domain, path = path))
}

