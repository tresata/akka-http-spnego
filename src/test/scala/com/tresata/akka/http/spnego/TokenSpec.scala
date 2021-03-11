package com.tresata.akka.http.spnego

import java.nio.charset.StandardCharsets.UTF_8
import org.scalatest.funspec.AnyFunSpec

class TokenSpec extends AnyFunSpec {
  describe("Token") {
    val tokens = new Tokens(3600 * 1000, "secret".getBytes(UTF_8))

    val token = tokens.create("HTTP/someserver.example.com@EXAMPLE.COM")

    it("should not be expired immediately"){
      assert(token.expired === false)
    }

    it("should rountrip serialization"){
      assert(token === tokens.parse(tokens.serialize(token)))
      assert(token.expired === false)
    }

    it("should throw an exception when parsing a tokenString with an incorrect signature"){
      intercept[TokenParseException] {
        tokens.parse(List(token.principal, token.expiration, tokens.sign(token) + "a").mkString("&"))
      }
    }

    it("should throw an exception when serializing an incomplete tokenString"){
      intercept[TokenParseException] {
        tokens.parse("a&1")
      }
    }

    it("should throw an exception when serializing an illegal tokenString"){
      intercept[TokenParseException] {
        tokens.parse("a&b&c")
      }
    }
  }
}
