package constants

object PubSub {
  val TOPIC_QUESTION = "topic_question"
  val TOPIC_ANSWER = "topic_answer"
  val TOPIC_TOPIC = "topic_topic"
  val TOPIC_CONTENT = "topic_content"
  val TOPIC_COMMENT = "topic_comment"
  val TOPIC_TAG = "topic_tag"
  val TOPIC_ACCESS_TOKEN = "topic_access_token"
  val TOPIC_TOKEN = "topic_token"
  val TOPIC_MAIL = "topic_mail"
  val TOPIC_USER = "topic_user"
  val TOPIC_FOLLOW = "topic_follow"
  val TOPIC_VOTE = "topic_vote"
  val TOPIC_VOTING = "topic_voting"
  val TOPIC_VOTE_COMMENT = "topic_vote_comment"
  val TOPIC_VOTE_UP = "topic_vote_up"
  val TOPIC_VOTE_DOWN = "topic_vote_down"
  val TOPIC_ELATICSEAERCH = "topic_elasticsearch"
  val TOPIC_GRAPH = "topic_graph"
  val TOPIC_NOTIFY = "topic_notify"
  val TOPIC_SCORE = "topic_score"
  val TOPIC_ALIAS = "topic_alias"

  val TOPIC_CONTENT_SCORE = "topic_content_score"

  val TASK_CREATE = "create"
  val TASK_VIEW = "view"
  val TASK_UPDATE = "update"
  val TASK_UNPUBLISHED = "unpublished"
  val TASK_DELETE = "delete"
  val TASK_BOOKMARK = "bookmark"

  val TASK_VOTE = "vote"
  val TASK_VOTE_UP = "vote_up"
  val TASK_DELETE_VOTE_UP = "delete_vote_up"
  val TASK_VOTE_DOWN = "vote_down"
  val TASK_DELETE_VOTE_DOWN = "delete_vote_down"
}


object Task {
  val CREATE = "create"
  val UPDATE = "update"
  val DELETE = "delete"
}

object Content {
  val ALL_CONTENT = "all"
  val HUB_PREFIX = "HUB"
  val USER_PREFIX = "user"
  val TAG_PREFIX = "TAG"
  val TOPIC_PREFIX = "topic"
  val MY_PREFIX = "my"
  val BOOKMARK_PREFIX = "bookmark"
}

object Comment {
  val ALL_COMMENT = "COMMENTS"
  val CONTENT_PREFIX = "CONTENT"
  val USER_PREFIX = "USER"
  val TAG_PREFIX = "TAG"
}

object Hub {
  val ALL_HUB = "HUB"
  val CONTENT_PREFIX = "CONTENT"
  val USER_PREFIX = "USER"
  val TAG_PREFIX = "TAG"
}

object Tag {
  val ALL_TAG = "ALL"
}