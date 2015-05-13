package utils

import java.util.UUID
import scala.collection.JavaConversions._

import constants._
import constants.Table

import database.Cassandra
import com.datastax.driver.core.querybuilder.QueryBuilder
import models.User
import play.api.mvc.RequestHeader
import scala.collection.mutable

object PrefixHelper {
  def questionEffectKeys(userId: UUID, tags: List[UUID]) = {
    val tagsKeys = tags.map(p =>  Content.TAG_PREFIX  + p.toString)
    val userKeys = Cassandra.session.execute(
      QueryBuilder.select("user_id")
        .from(Table.TAG_USER)
        .where(QueryBuilder.in("tag_id", tags.toSeq:_*))
    ).all().map(row => Content.USER_PREFIX + row.getUUID("user_id").toString)

    val myKeys = Set(Content.MY_PREFIX + userId.toString)
    val myContentKeys = Set(Content.USER_PREFIX + userId.toString)

    val keys = Set(Content.ALL_CONTENT) ++ userKeys ++ tagsKeys ++ myKeys ++ myContentKeys

    keys
  }

  def questionKeys(userId: UUID, topicIds: List[UUID]) = {
    Set(Content.ALL_CONTENT) ++ Set(Content.USER_PREFIX + "_" + userId.toString) ++ topicIds.map(Content.TOPIC_PREFIX + "_" + _.toString).toSet
  }

  def questionKeys(userIds: mutable.Buffer[UUID]) = {
    userIds.map(Content.USER_PREFIX + "_" + _.toString).toSet
  }

  def questionFollowKey(userId: UUID) = {
    Content.USER_PREFIX + "_follow_" + userId.toString
  }

  def answerKeys(questionId: UUID, userId: UUID) = {
    Set("question_" + questionId.toString, "user_" + userId.toString)
  }

  def targetKeys(parent: String, parentId: UUID, userId: UUID) = {
    Set(parent + "_" + parentId.toString, "user_" + userId.toString)
  }

  def getTopicKey(topicId: UUID) = Content.TOPIC_PREFIX + "_" + topicId.toString

  def getUserKey(userId: UUID) = Content.USER_PREFIX + "_" + userId.toString
}

object KeyHelper {
  def contentEffectKeys(userId: UUID, tags: Set[UUID])(implicit request: RequestHeader) = {
    val tagsKeys = tags.map(p =>  Content.TAG_PREFIX + p.toString)
    val userKeys = Cassandra.session.execute(
      QueryBuilder.select("user_id")
        .from(Table.TAG_USER)
        .where(QueryBuilder.in("tag_id", tags.toSeq:_*))
    ).all().map(row => Content.USER_PREFIX + row.getUUID("user_id").toString)

    val myKeys = Set(Content.MY_PREFIX + userId.toString, Content.MY_PREFIX + userId.toString)
    val myContentKeys = Set(Content.USER_PREFIX + userId.toString, Content.USER_PREFIX + userId.toString)

    val keys = Set(Content.ALL_CONTENT) ++ userKeys ++ tagsKeys ++ myKeys ++ myContentKeys

    keys
  }

  def questionEffectKeys(userId: UUID, tags: Set[UUID])(implicit request: RequestHeader) = {
    val tagsKeys = tags.map(p =>  Content.TAG_PREFIX + p.toString)
    val userKeys = Cassandra.session.execute(
      QueryBuilder.select("user_id")
        .from(Table.TAG_USER)
        .where(QueryBuilder.in("tag_id", tags.toSeq:_*))
    ).all().map(row => Content.USER_PREFIX + row.getUUID("user_id").toString)

    val myKeys = Set(Content.MY_PREFIX + userId.toString, Content.MY_PREFIX + userId.toString)
    val myContentKeys = Set(Content.USER_PREFIX + userId.toString, Content.USER_PREFIX + userId.toString)

    val keys = Set(Content.ALL_CONTENT) ++ userKeys ++ tagsKeys ++ myKeys ++ myContentKeys

    keys
  }

  def userContents(userId: UUID)(implicit request: RequestHeader) = Content.USER_PREFIX + "_" + userId.toString
  def userContents(userId: String)(implicit request: RequestHeader) = Content.USER_PREFIX + userId
  def userContents(user: User)(implicit request: RequestHeader) = Content.USER_PREFIX + user.id.toString

  def creatorContents(creatorId: UUID)(implicit request: RequestHeader) = Content.MY_PREFIX + creatorId.toString
  def creatorContents(creatorId: String)(implicit request: RequestHeader) = Content.MY_PREFIX + creatorId

  def allContents(implicit request: RequestHeader) = Content.ALL_CONTENT

  def tagContents(tagId: UUID)(implicit request: RequestHeader) = Content.TAG_PREFIX + tagId.toString
  def tagContents(tagId: String)(implicit request: RequestHeader) = Content.TAG_PREFIX + tagId

  def bookmarkContents(userId: UUID)(implicit request: RequestHeader) = Content.BOOKMARK_PREFIX + userId.toString
  def bookmarkContents(userId: String)(implicit request: RequestHeader) = Content.BOOKMARK_PREFIX + userId

  def commentEffectKeys(contentId: UUID, user: User)(implicit request: RequestHeader) = {
    List(contentComments(contentId), userComments(user.id), allComments)
  }

  def userComments(userId: UUID)(implicit request: RequestHeader) = Comment.USER_PREFIX + userId.toString
  def userComments(userId: String)(implicit request: RequestHeader) = Comment.USER_PREFIX + userId

  def contentComments(contentId: UUID)(implicit request: RequestHeader) = Comment.CONTENT_PREFIX + contentId.toString
  def contentComments(contentId: String)(implicit request: RequestHeader) = Comment.CONTENT_PREFIX + contentId

  def allComments(implicit request: RequestHeader) = Comment.ALL_COMMENT

  def getUrl(request: RequestHeader) = request.headers("Origin").toString.stripPrefix("http://").split("""\.""").head
}
