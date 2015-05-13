package utils

import play.api.libs.json.{JsValue, Json}

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

import utils.JsonHelper._

object UrlData {
  def get(url: String): Either[Exception, Map[String, Any]] = {
    try {
      val doc = Jsoup.connect(url).get()
      val title = doc.title()
      val uri = doc.baseUri()
      val desc = getMetaTag(doc, "description")
      val ogImage = getMetaTag(doc, "og:image")

      println("ogImage: " + ogImage)

      val data = Map(
        "title" -> title,
        "desc" -> desc,
        "baseUri" -> uri,
        "image" -> ogImage
      )

      Right(data)
    }
    catch {
      case e: Exception => throw e
    }
  }

  def getMetaTag(document: Document, attr: String): String = {
    var elements = document.select("meta[name=" + attr + "]")

    var size = elements.size()
    for(a <- 0 to size -1) {
      val element = elements.get(a)
      val s: String = element.attr("content")
      if(s != null) return s
    }

    elements = document.select("meta[property=" + attr + "]")
    size = elements.size()
    for(a <- 0 to size - 1) {
      val element = elements.get(a)
      val s: String = element.attr("content")
      if(s != null) return s
    }

    return null
  }

  def proNCT(doc: Document) = {
    val embedded = doc.select(".embed-share-button input")
    val embeddedCode = embedded.`val`()

    Map(
      "embed" -> embeddedCode
    )
  }

  def getText(html: String) = {
    Jsoup.parse("<div>" + html + "</div>").text()
  }

  def getImageUrl(html: String): Option[String] = {
    val doc = Jsoup.parse("<div>" + html + "</div>")
    val images = doc.select("img[src]")
    if(images.size() > 0) Some(images.iterator().next().attr("src"))
    else None
  }

  def getVideoUrl(html: String): Option[String] = {
    val doc = Jsoup.parse("<div>" + html + "</div>")
    val videos = doc.select("iframe[src~=(//player.vimeo.com/video/|//www.youtube.com/embed/|//www.dailymotion.com/embed/video/)]")
    if(videos.size() > 0) Some(videos.first().attr("src"))
    else None
  }
}
