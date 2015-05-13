package utils

import com.typesafe.config.ConfigFactory


object Config {
  val config = ConfigFactory.load()

  def getInt(name: String, default: Int) = try {
    config.getInt(name)
  } catch {
    case e: Exception => default
  }

  def getString(name: String, default: String) = try {
    config.getString(name)
  } catch {
    case e: Exception => default
  }

  def getBoolean(name: String, default: Boolean) = try {
    config.getBoolean(name)
  } catch {
    case e: Exception => default
  }
}