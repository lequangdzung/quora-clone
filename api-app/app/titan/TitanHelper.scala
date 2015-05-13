package titan

import com.thinkaurelius.titan.core._

import constants.Db._
import constants.Graph._
import play.Logger
import java.util.{Date, UUID}
import com.tinkerpop.blueprints.{Direction, Vertex}

object TitanHelper {
  var db: TitanGraph = _

  def setup = {
    val config = TitanFactory.build()

    config.set("storage.backend", "cassandrathrift")
    config.set("storage.hostname", TITAN_CASSANDRA_HOST)
    config.set("storage.cassandra.keyspace", TITAN_CASSANDRA_KEYSPACE)

    config.set("cache.db-cache", true)
    config.set("cache.db-cache-clean-wait", 20)
    config.set("cache.db-cache-time", 180000)
    config.set("cache.db-cache-size", 0.25)

    config.set("index." + TITAN_ES_INDEX + ".backend","elasticsearch")
    config.set("index." + TITAN_ES_INDEX + ".hostname", TITAN_ES_HOST)
    config.set("index." + TITAN_ES_INDEX + ".index-name", TITAN_ES_INDEX)
    config.set("index." + TITAN_ES_INDEX + ".elasticsearch.client-only", true)

    // now, it's safe to create a new titan instance
    try {
      db = config.open()

      val mgmt = db.getManagementSystem
      val trackEdge = mgmt.makeEdgeLabel(RELATION_TRACK).multiplicity(Multiplicity.SIMPLE).make()
      val untrackEdge = mgmt.makeEdgeLabel(RELATION_UNTRACK).multiplicity(Multiplicity.SIMPLE).make()
      val createByEdge = mgmt.makeEdgeLabel(RELATION_CREATE_BY).multiplicity(Multiplicity.SIMPLE).make()
      val aboutEdge = mgmt.makeEdgeLabel(RELATION_ABOUT).multiplicity(Multiplicity.SIMPLE).make()
      val ofEdge = mgmt.makeEdgeLabel(RELATION_OF).multiplicity(Multiplicity.SIMPLE).make()
      val tagEdge = mgmt.makeEdgeLabel(RELATION_TAG).multiplicity(Multiplicity.SIMPLE).make()
      val commentOnEdge = mgmt.makeEdgeLabel(RELATION_COMMENT_ON).multiplicity(Multiplicity.SIMPLE).make()
      val voteUpEdge = mgmt.makeEdgeLabel(RELATION_VOTE_UP).multiplicity(Multiplicity.SIMPLE).make()
      val voteDownEdge = mgmt.makeEdgeLabel(RELATION_VOTE_DOWN).multiplicity(Multiplicity.SIMPLE).make()

      mgmt.makeVertexLabel(LABEL_CONTENT).make()
      mgmt.makeVertexLabel(LABEL_COMMENT).make()
      mgmt.makeVertexLabel(LABEL_TAG).make()
      mgmt.makeVertexLabel(LABEL_USER).make()

      val id = mgmt.makePropertyKey("_id").dataType(classOf[String]).make()
      val time = mgmt.makePropertyKey("created").dataType(classOf[java.lang.Long]).make()

      mgmt.buildIndex("byId", classOf[Vertex]).addKey(id).buildCompositeIndex()
      mgmt.buildIndex("byCreated", classOf[Vertex]).addKey(time).buildCompositeIndex()

//      mgmt.buildEdgeIndex(trackEdge, "trackByTime", Direction.BOTH, Order.DESC, time)
//      mgmt.buildEdgeIndex(untrackEdge, "untrackByTime", Direction.BOTH, Order.DESC, time)
//      mgmt.buildEdgeIndex(createByEdge, "createByByTime", Direction.BOTH, Order.DESC, time)
//      mgmt.buildEdgeIndex(aboutEdge, "aboutByTime", Direction.BOTH, Order.DESC, time)
//      mgmt.buildEdgeIndex(ofEdge, "ofByTime", Direction.BOTH, Order.DESC, time)
//      mgmt.buildEdgeIndex(tagEdge, "ofByTime", Direction.BOTH, Order.DESC, time)
//      mgmt.buildEdgeIndex(commentOnEdge, "commentOnByTime", Direction.BOTH, Order.DESC, time)
//      mgmt.buildEdgeIndex(voteUpEdge, "voteUpByTime", Direction.BOTH, Order.DESC, time)
//      mgmt.buildEdgeIndex(voteDownEdge, "voteDownByTime", Direction.BOTH, Order.DESC, time)

      mgmt.commit()
    }
    catch {
      case e: java.lang.Exception => {
        Logger.error("Message Origin : " + e.getMessage)
        throw e
      }
    }
  }

  def addVertex(id: UUID, created: Date, label: String) = {
    val tx = db.newTransaction()
    val vertex = tx.addVertexWithLabel(label)
    vertex.setProperty("_id", id)
    vertex.setProperty("created", created.getTime)

    tx.commit()
  }

  def addVertex(tx: TitanTransaction, id: UUID, created: Date, label: String) = {
    val vertex = tx.addVertexWithLabel(label)
    vertex.setProperty("_id", id)
    vertex.setProperty("created", created.getTime)

    vertex
  }

  def addEdge(id1: UUID, id2: UUID, label: String) = {
    val tx = db.newTransaction()
    val created = new Date()
    val vertex1 = tx.getVertices("_id", id1)
    val vertex2 = tx.getVertices("_id", id2)

    if(vertex1.iterator().hasNext && vertex2.iterator().hasNext) {
      val v1 = vertex1.iterator().next()
      val v2 = vertex2.iterator().next()

      val edge = v1.addEdge(label, v2)
      edge.setProperty("created", created.getTime)
    }

    tx.commit()
  }

  def addEdge(tx: TitanTransaction, id1: UUID, id2: UUID, label: String) = {
    val created = new Date()
    val vertex1 = tx.getVertices("_id", id1)
    val vertex2 = tx.getVertices("_id", id2)

    if(vertex1.iterator().hasNext && vertex2.iterator().hasNext) {
      val v1 = vertex1.iterator().next()
      val v2 = vertex2.iterator().next()

      val edge = v1.addEdge(label, v2)
      edge.setProperty("created", created.getTime)
    }
  }

  def addEdge(tx: TitanTransaction, vertex1: Vertex, vertex2: Vertex, label: String) = {
    val created = new Date()
    val edge = vertex1.addEdge(label, vertex2)
    edge.setProperty("created", created.getTime)
    edge
  }

  def getVertex(tx: TitanTransaction, id: UUID) = {
    val vertexOpt = tx.getVertices("_id", id).iterator()
    if(vertexOpt.hasNext) Some(vertexOpt.next())
    else None
  }

  def createContent(id: UUID, created: Date, creatorId: UUID, tags: List[UUID]) = {
    val tx = db.newTransaction()

    val content = addVertex(tx, id, created, LABEL_CONTENT)
    // get user
    getVertex(tx, creatorId).map { user =>
      addEdge(tx, content, user, RELATION_CREATE_BY)
    }

    tags.map { tagId =>
      getVertex(tx, tagId).map { tag =>
        addEdge(tx, content, tag, RELATION_TAG)
      }
    }

    tx.commit()
  }

  def createUser(id: UUID, created: Date) = {
    val tx = db.newTransaction()
    addVertex(tx, id, created, LABEL_USER)

    tx.commit()
  }

  def createTag(id: UUID, created: Date, creatorId: UUID) = {
    val tx = db.newTransaction()
    val tag = addVertex(tx, id, created, LABEL_TAG)

    getVertex(tx, creatorId).map { user =>
      addEdge(tx, user, tag, RELATION_CREATE_BY)
    }
    tx.commit()
  }

  def createComment(id: UUID, created: Date, contentId: UUID, creatorId: UUID) = {
    val tx = db.newTransaction()
    val comment = addVertex(tx, id, created, LABEL_COMMENT)

    var creator: Option[Vertex] = None
    getVertex(tx, contentId).map { content =>
      addEdge(tx, comment, content, RELATION_COMMENT_FOR)
      getVertex(tx, creatorId).map { user =>
        addEdge(tx, user, content, RELATION_COMMENT_ON)
        creator = Some(user)
      }
    }

    creator.map { user =>
      addEdge(tx, comment, user, RELATION_CREATE_BY)
    }

    tx.commit()
  }
}