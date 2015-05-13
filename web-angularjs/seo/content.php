<?php
require(__DIR__ . '/httpful/bootstrap.php');
use \Httpful\Request;

$uri = 'http://backend.com/api/contents/fdfdff?limit=5';
$request = Request::get($uri)
	->addHeaders(array(
      'Origin' => 'http://baby.contentalk.com',              // Or add multiple headers at once
  ))
	->send();

$content = $request->body->content;
$comments = $content->comments;
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title><?php echo $content->title; ?></title>
    </head>
  <body>
    <h1><?php echo $content->title; ?></h1>
    <div class="info">
      By <div class="author"><a href="/#!/users/<?php echo $content->creator->alias; ?>" title="<?php echo $content->creator->display; ?>"><?php echo $content->creator->display; ?></a> | <span><?php echo gmdate("Y-m-d H:i:s", ($content->updated/1000)); ?></span></div>
    </div>
    <article><?php echo $content->body; ?></article>
    <ul class="comments">
    	<?php foreach($comments as $comment): ?>
    		<li>
    			<div class="reply">
	          <p><strong><a href="/#!/users/<?php echo $comment->creator->alias; ?>" title="<?php echo $comment->creator->display; ?>"><?php echo $comment->creator->display; ?></a></strong> <?php echo $comment->body; ?></p>
	          <p class="reply-time">created: <?php echo gmdate("Y-m-d H:i:s", ($comment->created/1000)); ?></p>
	        </div>
    		</li>
    	<?php endforeach; ?>
    </ul>
  </body>
</html>