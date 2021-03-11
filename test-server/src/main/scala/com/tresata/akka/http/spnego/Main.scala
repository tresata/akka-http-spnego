package com.tresata.akka.http.spnego

import java.io.FileInputStream
import java.security.{ KeyStore, SecureRandom }
import javax.net.ssl.{ KeyManagerFactory, TrustManagerFactory, SSLContext }

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.{ ConnectionContext, Http }
import akka.http.scaladsl.server.Directives._

import com.tresata.akka.http.spnego.SpnegoDirectives._

object Main extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val route = logRequestResult("debug") {
    spnegoAuthenticate(){ token =>
      get{
        path("ping") {
          complete(s"pong for user ${token.principal}")
        }
      }
    }
  }

  // very painful ssl stuff
  val keystore = System.getProperty("javax.net.ssl.keyStore")
  val password = System.getProperty("javax.net.ssl.keyStorePassword")
  val ks = KeyStore.getInstance("JKS")
  val fis = new FileInputStream(keystore)
  try ks.load(fis, password.toCharArray)
  finally fis.close()
  val kmf = KeyManagerFactory.getInstance("SunX509")
  kmf.init(ks, password.toCharArray)
  val tmf = TrustManagerFactory.getInstance("SunX509")
  tmf.init(ks)
  val sslContext = SSLContext.getInstance("TLS")
  sslContext.init(kmf.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
  val connectionContext = ConnectionContext.httpsServer(sslContext)

  Http()
    .newServer("0.0.0.0", 12345)
    .enableHttps(connectionContext)
    .bind(route)
}
