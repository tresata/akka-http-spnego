package com.tresata.akka.http.spnego

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import scala.util.{Try, Success}

import org.apache.commons.codec.binary.Base64

case class TokenParseException(message: String) extends Exception(message)

class Tokens(tokenValidity: Long, signatureSecret: Array[Byte]) {
  private def newExpiration: Long = System.currentTimeMillis + tokenValidity

  private[spnego] def sign(token: Token): String = {
    val md = MessageDigest.getInstance("SHA")
    md.update(token.principal.getBytes(UTF_8))
    val bb = ByteBuffer.allocate(8)
    bb.putLong(token.expiration)
    md.update(bb.array)
    md.update(signatureSecret)
    new Base64(0).encodeToString(md.digest)
  }

  def create(principal: String): Token = Token(principal, newExpiration)

  def parse(tokenString: String): Token = tokenString.split("&") match {
    case Array(principal, expirationString, signature) => Try(expirationString.toLong) match {
      case Success(expiration) => {
        val token = Token(principal, expiration)
        if (sign(token) != signature) throw TokenParseException("incorrect signature")
        token
      }
      case _ => throw TokenParseException("expiration not a long")
    }
    case _ => throw TokenParseException("incorrect number of fields")
  }

  def serialize(token: Token): String = List(token.principal, token.expiration, sign(token)).mkString("&")
}

case class Token private[spnego] (principal: String, expiration: Long) {
  def expired: Boolean = System.currentTimeMillis > expiration
}
