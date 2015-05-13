CREATE KEYSPACE sitest WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};

USE sitest;

CREATE TABLE user (
 	id timeuuid PRIMARY KEY,
 	email text,
 	password text,
 	display text,
 	avatar text,
 	created timestamp,
 	status boolean,
 	permissions set<text>,
 	info map<text, text>,
 	unread bigint,
 	score map<text, bigint>,
 	follower_count bigint
);

ALTER TABLE user ADD alias text;
ALTER TABLE user ADD fbid text;

CREATE INDEX user_email ON user(email) ;
CREATE INDEX user_password ON user(password) ;
CREATE INDEX user_status ON user(status)  ;


CREATE TABLE access_token (
    id text PRIMARY KEY,
    refresh_token timeuuid,
    auth_info_id timeuuid,
    scope text,
    expires_in bigint,
    created_at timestamp
);

CREATE TABLE access_token_expire (
    key text,
    expire timestamp,
    token_id text,
    PRIMARY KEY (key, expire)
);

CREATE TABLE auth_info (
    id timeuuid PRIMARY KEY,
    user_id timeuuid,
    client_id text,
    scope text,
    redirect_uri text
);

CREATE TABLE user_content_action (
    user_id timeuuid,
    content_id timeuuid,
    vote text,
    bookmark text,
    follow text,
    PRIMARY KEY (user_id, content_id)
);

CREATE TABLE notify_track (
    key text,
    user_id uuid,
    notify_id uuid,
    PRIMARY KEY (key, user_id)
);

CREATE TABLE notify (
    id timeuuid PRIMARY KEY,
    user_id uuid,
    type text,
    target_id uuid,
    title text,
    link text,
    action text,
    users map<uuid, bigint>,
    updated timestamp,
    status boolean
);

CREATE TABLE alias (
    key text,
    alias text,
    data text,
    PRIMARY KEY (key, alias)
);

CREATE TABLE active_token (
    key text,
    token_id uuid,
    target_id uuid,
    PRIMARY KEY (key, token_id)
);

CREATE TABLE mail (
    id uuid PRIMARY KEY,
    mail_from text,
    mail_to set<text>,
    mail_cc set<text>,
    mail_bcc set<text>,
    subject text,
    content text,
    send_time timestamp,
    deliver_time timestamp,
    deliver boolean
);

CREATE TABLE string_unique (
    key text,
    value text,
    target_id timeuuid,
    PRIMARY KEY (key, value)
);

CREATE TABLE counter (
    key text,
    id uuid,
    value counter,
    PRIMARY KEY (key, id)
);

CREATE TABLE long_ordered (
    key text,
    value bigint,
    id uuid,
    action text,
    f text,
    fid uuid,
    answer_id uuid,
    updated timestamp,
    PRIMARY KEY (key, value, id)
);

CREATE TABLE long_ordered_track(
    key text,
    id uuid,
    value bigint,
    action text,
    f text,
    fid uuid,
    answer_id uuid,
    updated timestamp,
    PRIMARY KEY (key, id)
);

CREATE TABLE date_ordered (
    key text,
    value timestamp,
    id uuid,
    action text,
    f text,
    fid uuid,
    answer_id uuid,
    updated timestamp,
    PRIMARY KEY (key, value, id)
);

CREATE TABLE date_ordered_track(
    key text,
    id uuid,
    value timestamp,
    action text,
    f text,
    fid uuid,
    answer_id uuid,
    updated timestamp,
    PRIMARY KEY (key, id)
);

CREATE TABLE double_ordered (
    key text,
    value double,
    id uuid,
    action text,
    f text,
    fid uuid,
    answer_id uuid,
    updated timestamp,
    PRIMARY KEY (key, value, id)
);

CREATE TABLE double_ordered_track(
    key text,
    id uuid,
    value double,
    action text,
    f text,
    fid uuid,
    answer_id uuid,
    updated timestamp,
    PRIMARY KEY (key, id)
);

CREATE TABLE question (
	id timeuuid PRIMARY KEY,
	creator_id uuid,
	title text,
	alias text,
	description text,
	short_description text,
	topics List<uuid>,
	created timestamp,
	updated timestamp,
	status int,
	vote_up bigint,
	vote_down bigint,
	vote bigint,
	view bigint,
    score bigint,
    comment_count bigint,
    answer_count bigint,
    follower_count bigint,
    user_count bigint,
    promote_answer_id uuid,
    best_answer_id uuid,
    confidence double
);

CREATE TABLE question_user (
    question_id uuid,
    user_id uuid,
    time counter,
    PRIMARY KEY (question_id, user_id)
);

CREATE TABLE answer (
	id timeuuid PRIMARY KEY,
	creator_id uuid,
	question_id uuid,
	body text,
	short_body text,
	created timestamp,
	updated timestamp,
	status int,
	vote_up bigint,
	vote_down bigint,
	vote bigint,
    score bigint,
    confidence double,
    comment_count bigint,
    follower_count bigint
);

CREATE TABLE follow (
    key text,
    follower_id uuid,
    updated timestamp,
    PRIMARY KEY (key, follower_id)
);

CREATE TABLE user_follow (
    user_id uuid,
    target text,
    target_id uuid,
    updated timestamp,
    about text,
    PRIMARY KEY (user_id, target, target_id)
);

CREATE TABLE topic (
    id timeuuid PRIMARY KEY,
    creator_id uuid,
    created timestamp,
    updated timestamp,
    name text,
    alias text,
    thumb text,
    description text,
    status boolean,
    follower_count bigint,
    question_count bigint
);

CREATE TABLE user_action (
    user_id uuid,
    target text,
    target_id uuid,
    vote text,
    follow text,
    answer text,
    comment text,
    bookmark text,
    hide text,
    promote text,
    PRIMARY KEY (user_id, target, target_id)
);

CREATE TABLE comment (
	id timeuuid PRIMARY KEY,
	creator_id uuid,
	parent_id uuid,
	body text,
	short_body text,
	created timestamp,
	updated timestamp,
	status int,
	vote_up bigint,
	vote_down bigint,
    confidence double,
    comment_count bigint
);

TRUNCATE question;
TRUNCATE answer ;
TRUNCATE long_ordered;
TRUNCATE long_ordered_track ;
TRUNCATE double_ordered;
TRUNCATE double_ordered_track ;
TRUNCATE date_ordered;
TRUNCATE date_ordered_track ;
TRUNCATE topic ;
TRUNCATE user_follow ;
TRUNCATE follow;
TRUNCATE question_user ;
TRUNCATE user_action ;
