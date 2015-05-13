<?php
require(__DIR__ . '/httpful/bootstrap.php');
include('config.php');

use \Httpful\Request;

$uri = $USER_API_URI . $alias;

if(isset($id) && isset($before)) {
	$uri = $uri . "?id=" . $id . "&before=" . $before;
}

$request = Request::get($uri)
	->addHeaders(array(
      'Origin' => $_SERVER['HTTP_HOST']
  ))
	->send();

$contents = $request->body->contents;
if(count($contents) == 5) {
	$last = $contents[4];
}
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title></title>
    </head>
  <body>
  	<?php foreach($contents as $content): ?>
    <div class="content-row" id="content-<?php echo $content->id; ?>">
      <div class="content-item">
      	<?php if($content->format == 'image' || $content->format=='video'): ?>
        <div class="pull-right">
          <a href="/#!/contents/<?php echo $content->alias; ?>">
          	<img src="<?php echo $content->thumb; ?>" alt="<?php echo $content->title; ?>" />
          </a>
        </div>
        <?php endif; ?>
        <h2>
          <a class="title" href="/#!/contents/<?php echo $content->alias; ?>" title="<?php echo $content->title; ?>"><?php echo $content->title; ?></a>
        </h2>
        <div class="info">
           by <div class="author"><a href="/users/<?php echo $content->creator->alias; ?>" title="<?php echo $content->creator->display; ?>"><?php echo $content->creator->display; ?></a></div> | <span>2 days ago</span></div>
        <div class="short-desc"><?php echo $content->body; ?>... <a href="/#!/contents/<?php echo $content->alias; ?>" title="<?php echo $content->title; ?>">See More</a></div>
        <ul class="social-buttons clearfix pull-left">
          <li class="comments"><a href="/#!/contents/<?php echo $content->alias; ?>"><span class="fa fa-comment-o"></span><?php echo $content->commentCount; ?></a></li>
        </ul>
        <ul class="tags clearfix pull-right">
        	<?php foreach($content->tags as $tag): ?>
          <li>
            <div class="tag" ><a class="tag-item" title="<?php echo $tag->title; ?>" href="/#!/tags/<?php echo $tag->alias; ?>"><?php echo $tag->title; ?></a></div>
          </li>
        <?php endforeach; ?>
        </ul>
      </div>
    </div>
  <?php endforeach; ?>
  <?php if(isset($last)): ?>
  	<div class="view-more">
	    <a class="get-more" href="/#!/users/<?php echo $alias; ?>?id=<?php echo $last->id; ?>&before=<?php echo $last->score; ?>"> View More Content </a>
	  </div>
  <?php endif; ?>
  </body>
</html>