package database

import utils.Config


trait db {
  def insert(table: String, names: Array[String], values: Array[AnyRef])

  def getTimeId : AnyRef
}
