name := """si-rms"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.4"

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

libraryDependencies ++= {
  val elasticsearchVersion = "1.3.4"
  val scrimageVersion = "1.4.1"
  val akkaVersion = "2.3.4"
  val datastaxVersion = "2.1.2"
  val neo4jVersion = "2.1.5"
  val imgscalr = "4.2"
  val apacheCommonsIoVersion = "2.1"
  val blueprintsVersion = "2.5.0"
  val titanVersion = "0.5.1"
  val gremlinVersion = "2.6.0"
  Seq(
    jdbc,
    anorm,
    cache,
    ws,
    "com.datastax.cassandra" % "cassandra-driver-core" % datastaxVersion,
    "com.datastax.cassandra" % "cassandra-driver-mapping" % datastaxVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-kernel" % akkaVersion,
    "com.typesafe.akka" %% "akka-contrib" % akkaVersion ,
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    "org.jsoup" % "jsoup" % "1.7.3",
    "org.elasticsearch" % "elasticsearch" % elasticsearchVersion,
    "org.imgscalr" % "imgscalr-lib" % imgscalr,
    "commons-io" % "commons-io" % apacheCommonsIoVersion,
    "commons-codec" % "commons-codec" % "1.9",
    "me.lessis" %% "courier" % "0.1.3",
    "com.tinkerpop.blueprints" % "blueprints-core" % blueprintsVersion,
    "com.thinkaurelius.titan" % "titan-core" % titanVersion,
    "com.thinkaurelius.titan" % "titan-cassandra" % titanVersion excludeAll(ExclusionRule(organization = "org.slf4j")),
    "com.thinkaurelius.titan" % "titan-berkeleyje" % titanVersion,
    "com.thinkaurelius.titan" % "titan-es" % titanVersion,
    "javax.persistence" % "persistence-api" % "1.0",
    "com.tinkerpop.gremlin" % "gremlin-java" % gremlinVersion
  )
}