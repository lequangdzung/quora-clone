package models.v2

import java.util.{Date, UUID}
import scala.collection.JavaConversions._

import com.datastax.driver.core._
import com.datastax.driver.core.utils.UUIDs
import com.datastax.driver.mapping
import com.datastax.driver.mapping.annotations._
import com.datastax.driver.mapping.annotations.Table
import com.datastax.driver.mapping.Result

import constants.Db
import database.Cassandra.manager

@Table(keyspace = "sitest", name = "mail")
class Mail {
  @PartitionKey
  var id: UUID = _

  @Column(name = "mail_from")
  var from: String = _

  @Column(name = "mail_to")
  var to: java.util.Set[String] = _

  @Column(name = "mail_cc")
  var cc: java.util.Set[String] = _

  @Column(name = "mail_bcc")
  var bcc: java.util.Set[String] = _

  var subject: String = _

  var content: String = _

  @Column(name = "send_time")
  var sendTime: Date = _

  @Column(name = "deliver_time")
  var deliverTime: Date = _

  var deliver: java.lang.Boolean = _

  def getId = id
  def setId(id: UUID) = this.id = id

  def getFrom = from
  def setFrom(from: String) = this.from = from

  def getTo = to
  def setTo(to: java.util.Set[String]) = this.to = to

  def getCc = cc
  def setCc(cc: java.util.Set[String]) = this.cc = cc

  def getBcc = bcc
  def setBcc(bcc: java.util.Set[String]) = this.bcc = bcc

  def getSubject = subject
  def setSubject(subject: String) = this.subject = subject

  def getContent = content
  def setContent(content: String) = this.content = content

  def getSendTime = sendTime
  def setSendTime(sendTime: Date) = this.sendTime = sendTime

  def getDeliverTime = deliverTime
  def setDeliverTime(deliverTime: Date) = this.deliverTime = deliverTime

  def getDeliver = deliver
  def setDeliver(deliver: java.lang.Boolean) = this.deliver = deliver
}

@Accessor
trait MailAccessor {
//  @Query("SELECT * FROM token WHERE key=:key AND token=:token")
//  def getOne(@Param("key") key: String, @Param("token") token: UUID): Token
}

object Mail {
  val accessor = manager.createAccessor(classOf[MailAccessor])
  val mapper = manager.mapper(classOf[Mail])

  def apply(from: String, to: Set[String], subject: String, content: String) = {
    val mail = new Mail
    mail.id = UUIDs.timeBased()
    mail.from = from
    mail.to = to
    mail.subject = subject
    mail.content = content
    mail.deliver = false
    mail.sendTime = new Date()

    mail
  }

  def create(mail: Mail) = {
    mapper.save(mail)
  }

  def updateDeliver(mail: Mail) = {
    mail.setDeliver(true)
    mail.setDeliverTime(new Date)

    println("mail is: " + mail)
    mapper.save(mail)
  }
}