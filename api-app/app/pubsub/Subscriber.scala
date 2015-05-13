package pubsub

import scala.reflect.ClassTag
import akka.actor.{Props, Actor}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{Subscribe, SubscribeAck, Publish}
import akka.cluster.Cluster

import play.api.libs.concurrent.Akka
import play.api.Play.current

abstract class Subscriber(topic: String) extends Actor {
  val mediator = DistributedPubSubExtension(context.system).mediator

  mediator ! Subscribe(topic, self)

  def receive = {
    case SubscribeAck(Subscribe(topic, None, `self`)) => context become ready
  }

  def ready: Actor.Receive = {
    case data: Any => process(data)
  }

  def process(data: Any)
}

class Publisher extends Actor {
  // activate the extension
  val mediator = DistributedPubSubExtension(context.system).mediator

  def receive = {
    case (topic: String, data: Any) => mediator ! Publish(topic, data)
  }
}

object PubSubHelper {
  val publisher = Akka.system.actorOf(Props[Publisher], "publisher")

  def setup = {
    val address = Cluster(Akka.system).selfAddress
    Cluster(Akka.system).join(address)
  }

  def register[T <:Actor : ClassTag](actName: String) = {
    Akka.system.actorOf(Props[T], name = actName)
  }

  def publish(topic: String, data: Any) = publisher ! (topic, data)
}