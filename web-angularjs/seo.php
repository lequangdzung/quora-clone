<?php
$url = $_GET['url'];

$url_params = parse_url("?" . $url);
if(array_key_exists('query', $url_params)) {
	parse_str($url_params['query']);
}

if(startsWith($url, 'contents/') || startsWith($url, '/contents/')) {
	include 'seo/content.php';
}
else if(startsWith($url, 'users/') || startsWith($url, '/users/')){
	$arr = explode("/", $url);
	$alias = end($arr);
	include 'seo/user.php';
}
else if(startsWith($url, 'tags/') || startsWith($url, '/tags/')){
	$arr = explode("/", $url);
	$alias = end($arr);
	include 'seo/tag.php';
}
else {
	include 'seo/home.php';	
}

function startsWith($haystack, $needle)
{
    return $needle === "" || strpos($haystack, $needle) === 0;
}
?>