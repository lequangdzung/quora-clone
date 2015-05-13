package models.v2

import java.util.UUID
import scala.collection.JavaConversions._

import com.datastax.driver.core._
import com.datastax.driver.core.utils.UUIDs
import com.datastax.driver.mapping
import com.datastax.driver.mapping.annotations._
import com.datastax.driver.mapping.annotations.Table
import com.datastax.driver.mapping.Result

import constants.Db
import database.Cassandra.manager

@Table(keyspace = "balloontest", name = "active_token")
class Token {
  @PartitionKey
  var key: String = _

  @ClusteringColumn
  @Column(name = "token_id")
  var token: UUID = _

  @Column(name = "target_id")
  var targetId: UUID = _

  def setKey(key: String) = this.key = key

  def getKey = key

  def setToken(token: UUID) = this.token = token

  def getToken = token

  def getTargetId = targetId

  def setTargetId(targetId: UUID) = this.targetId = targetId
}

@Accessor
trait TokenAccessor {
//  @Query("SELECT * FROM token WHERE key=? AND token=?")
//  def getOne(key: String, token: UUID): Token
}

object Token {
  val accessor = manager.createAccessor(classOf[TokenAccessor])
  val mapper = manager.mapper(classOf[Token])

  def apply(key: String, token: UUID, targetId: UUID) = {
    val tk = new Token
    tk.setKey(key)
    tk.setToken(token)
    tk.setTargetId(targetId)

    tk
  }

  def get(key: String, token_id: UUID) = mapper.get(key, token_id)

  def create(tk: Token) = {
    mapper.save(tk)
  }
}