package com.tresata.akka.http.spnego

import com.typesafe.config.{ Config, ConfigFactory }

import akka.http.scaladsl.server.Directive1

trait SpnegoDirectives {
  def spnegoAuthenticate(config: Config = ConfigFactory.load()): Directive1[Token] = SpnegoAuthenticator.spnegoAuthenticate(config)
}

object SpnegoDirectives extends SpnegoDirectives
