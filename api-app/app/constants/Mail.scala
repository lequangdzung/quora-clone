package constants

import utils.Config

object Mail {
  lazy val HOST = Config.getString("mail.host", "smtp.gmail.com")
  lazy val PORT = Config.getInt("mail.port", 587)
  lazy val AUTH = Config.getBoolean("mail.auth", true)
  lazy val TTLS = Config.getBoolean("mail.ttls", true)
  lazy val USER = Config.getString("mail.user", "dunglqttm@gmail.com")
  lazy val PASS = Config.getString("mail.pass", "ngocminh@224")

  lazy val FROM = Config.getString("mail.from", "dunglqttm@gmail.com")
}
