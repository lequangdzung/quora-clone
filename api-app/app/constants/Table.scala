package constants

object Table {
  //TAG
  lazy val TAG = "tag"
  lazy val TOPIC = "topic"
  lazy val SCORE_COUNTER = "score_counter"
  lazy val TAG_COUNTER = "tag_counter"
  lazy val USER_COUNTER = "user_counter"
  lazy val TAG_USER = "tag_user"
  //END TAG

  //USER
  lazy val USER = "user"
  lazy val USER_TAG = "user_tag"
  lazy val USER_USER = "user_user"
  lazy val USER_VOTE = "user_vote"
  lazy val USER_VOTE_COMMENT = "user_vote_comment"
  lazy val USER_ACTION = "user_action"
  lazy val USER_CONTENT_ACTION = "user_content_action"
  lazy val USER_STRING_UNIQUE = "user_string_unique"
  //END USER

  //CONTENT
  lazy val QUESTION = "question"
  lazy val QUESTION_USER = "question_user"
  lazy val ANSWER = "answer"
  lazy val CONTENT = "content"
  lazy val ARTICLE = "article"

  lazy val CONTENT_COUNT = "content_counter"
  lazy val CONTENT_SCORE = "content_score"
  lazy val CONTENT_SCORE_TRACK = "content_score_track"
  lazy val CONTENT_VOTE = "content_vote"
  lazy val CONTENT_VOTE_TRACK = "content_vote_track"
  lazy val CONTENT_DATE = "content_date"
  lazy val CONTENT_DATE_TRACK = "content_date_track"
  //END CONTENT

  lazy val FOLLOWER = "follower"
  lazy val FOLLOW = "follow"
  lazy val USER_FOLLOW = "user_follow"

  //NOTIFY
  lazy val NOTIFY = "notify"
  lazy val NOTIFY_TRACK = "notify_track"
  lazy val NOTIFY_DATE = "notify_date"
  lazy val NOTIFY_DATE_TRACK = "notify_date_track"
  //END NOTIFY

  lazy val ES_TAG = "tag"

  lazy val ALIAS = "alias"

  //HUB
  lazy val HUB = "hub"
  lazy val HUB_SCORE = "hub_score"
  lazy val HUB_SCORE_TRACK = "hub_score_track"

  lazy val HUB_DATE = "hub_date"
  lazy val HUB_DATE_TRACK = "hub_date_track"
  //END HUB

  //COMMENT
  lazy val COMMENT = "comment"

  lazy val COMMENT_COUNT = "comment_counter"
  lazy val COMMENT_SCORE = "comment_score"
  lazy val COMMENT_SCORE_TRACK = "comment_score_track"
  lazy val COMMENT_VOTE = "comment_vote"
  lazy val COMMENT_VOTE_TRACK = "comment_vote_track"
  lazy val COMMENT_DATE = "comment_date"
  lazy val COMMENT_DATE_TRACK = "comment_date_track"
  //END COMMENT
}
