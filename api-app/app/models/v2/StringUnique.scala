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

@Table(keyspace = "sitest", name = "string_unique")
class StringUnique {
  @PartitionKey
  var key: String = _

  @ClusteringColumn
  var value: String = _

  @Column(name = "target_id")
  var targetId: UUID = _

  def setKey(key: String) = this.key = key

  def getKey = key

  def setValue(value: String) = this.value = value

  def getValue = value

  def getTargetId = targetId

  def setTargetId(targetId: UUID) = this.targetId = targetId
}

@Accessor
trait StringUniqueAccessor {
    @Query("SELECT * FROM string_unique WHERE key=:key AND value=:value")
    def getOne(key: String, value: String): StringUnique
}

object StringUnique {
  val accessor = manager.createAccessor(classOf[StringUniqueAccessor])
  val mapper = manager.mapper(classOf[StringUnique])

  def apply(key: String, value: String, targetId: UUID) = {
    val sq = new StringUnique
    sq.setKey(key)
    sq.setValue(value)
    sq.setTargetId(targetId)

    sq
  }

  def create(sq: StringUnique) = {
    mapper.save(sq)
  }

  def isUnique(table: String, field: String, value: String) = {
    accessor.getOne(table + "_" + field, value) == null
  }

  def getTargetId(table: String, field: String, value: String) = {
    val row = accessor.getOne(table + "_" + field, value)

    if(row == null) None
    else Some(row.getTargetId)
  }
}