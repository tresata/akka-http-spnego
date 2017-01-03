package com.tresata.akka.http.spnego

import javax.security.auth.login.{ Configuration, AppConfigurationEntry }

import scala.collection.JavaConverters._

case class KerberosConfiguration(keytab: String, principal: String, debug: Boolean) extends Configuration {
  val ticketCache = Option(System.getenv("KRB5CCNAME"))

  override def getAppConfigurationEntry(name: String): Array[AppConfigurationEntry] = Array(
    new AppConfigurationEntry(
      "com.sun.security.auth.module.Krb5LoginModule",
      AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
      (Map(
        "keyTab" -> keytab,
        "principal" -> principal,
        "useKeyTab" -> "true",
        "storeKey" -> "true",
        "doNotPrompt" -> "true",
        "useTicketCache" -> "true",
        "renewTGT" -> "true",
        "isInitiator" -> "false",
        "refreshKrb5Config" -> "true",
        "debug" -> debug.toString
      ) ++ ticketCache.map{ x => Map("ticketCache" -> x) }.getOrElse(Map.empty)).asJava
    )
  )
}
