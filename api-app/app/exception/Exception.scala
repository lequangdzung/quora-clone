package exception

case class ConfigException(name: String, msg: String = "") extends Exception
case class CassandraInValidIn(msg: String = "") extends Exception
case class CassandraInValidPrependAll(msg: String = "") extends Exception
case class CassandraInValidAppendAll(msg: String = "") extends Exception
case class CassandraInValidAddAll(msg: String = "") extends Exception
case class CassandraInValidRemoveAll(msg: String = "") extends Exception
case class CassandraInValidDiscardAll(msg: String = "") extends Exception
case class CassandraInValidPut(msg: String = "") extends Exception
case class CassandraInvalidDelete(msg: String = "") extends Exception
case class BadData(field: String, msg: String) extends Exception

case class InvalidData(tables: List[String]) extends Exception
class NeedImplement extends Exception