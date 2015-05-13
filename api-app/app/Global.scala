import play.api._
import play.api.mvc._
import play.api.http.HeaderNames._

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import database._
import pubsub._
import security.oauth2.TokenStorage
import titan.TitanHelper

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    // try to connect with cassandra
    Cassandra.connect("localhost")

    // setup pub-sub akka cluster
    PubSubHelper.setup

    // register subscriber
    PubSubHelper.register[UserSubscriber]("UserSubscriber")
    PubSubHelper.register[AccessTokenSubscriber]("TokenSubscriber")
    PubSubHelper.register[ElasticSubscriber]("ElasticSubscriber")
    PubSubHelper.register[NotifySubscriber]("NotifySubscriber")
    PubSubHelper.register[AliasSubscriber]("AliasSubscriber")
    PubSubHelper.register[TokenSubscriber]("ActiveTokenSubscriber")
    PubSubHelper.register[MailSubscriber]("MailSubscriber")
    PubSubHelper.register[QuestionSubscriber]("QuestionSubscriber")
    PubSubHelper.register[FollowSubscriber]("FollowSubscriber")
    PubSubHelper.register[AnswerSubscriber]("AnswerSubscriber")
    PubSubHelper.register[CommentSubscriber]("CommentSubscriber")
    PubSubHelper.register[TopicSubscriber]("TopicSubscriber")

    TokenStorage.init

    ElasticSearch.start

    //TitanHelper.setup

    Logger.info("Application has started")
  }

  override def onStop(app: Application) {
    ElasticSearch.stop

    //AkkaSystem.stop
    Logger.info("Application shutdown...")
  }

  /**
   * Global action composition.
   */
  override def doFilter(action: EssentialAction): EssentialAction = EssentialAction { request =>
    action.apply(request).map(_.withHeaders("Access-Control-Allow-Origin" -> "*", "Access-Control-Expose-Headers" -> "WWW-Authenticate"))
  }
}