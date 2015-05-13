package utils

import java.awt.image.BufferedImage
import java.io.File
import java.net._
import javax.imageio.ImageIO

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

import play.api.libs.ws._
import play.api.Play.current
import play.api.libs.json.JsArray

import org.imgscalr.Scalr
import javax.imageio.stream.ImageInputStream
import java.awt.Dimension

object MediaUtil {
  def videoImgUrl(html: String) = {
    UrlData.getVideoUrl(html).flatMap { uri =>
      val VideoRegex = """(//player.vimeo.com/video/|//www.youtube.com/embed/|//www.dailymotion.com/embed/video/)([^/]+)""".r

      try {
        val VideoRegex(baseUrl, videoId) = uri
        val thumbUrl = if(baseUrl == "//www.youtube.com/embed/") {
          "http://img.youtube.com/vi/"+videoId+"/hqdefault.jpg"
        }
        else if(baseUrl == "//player.vimeo.com/video/") {
          val result = Await.result(WS.url("http://vimeo.com/api/v2/video/"+videoId+".json").get().map{ p =>
            println("json is: " + p.json.as[JsArray].value(0) \ "thumbnail_medium")
            (p.json.as[JsArray].value(0) \ "thumbnail_medium").as[String]
          }, 10 seconds)

          result
        }
        else null

        if(thumbUrl != null) Some(thumbUrl) else None
      }
      catch {
        case e: Exception => None
      }
    }
  }

  def resizeImage(url: String, outputPath: String) = {
    val imageURL = new URL(url)
    val image = ImageIO.read(imageURL)

    val resizeImg = Scalr.resize(image, constants.Db.THUMB_SIZE)
    writeImage(outputPath, resizeImg)
  }

  def writeImage(path: String, image: BufferedImage) = {
    // retrieve image
    val outputFile = new File(path);
    ImageIO.write(image, "png", outputFile);
  }

  def renderFitImage(resourceImage: File, orgFilename: String, ext: String) = {
    val fitImage = getFitImage(resourceImage)
    if(fitImage.getWidth > constants.Db.FULL_WIDTH) {
      val image = ImageIO.read(resourceImage)
      val resizeImg = Scalr.resize(image, constants.Db.FULL_WIDTH)
      val newFilename = orgFilename + "_resize_" + constants.Db.FULL_WIDTH + "." + ext
      writeImage(constants.Db.IMAGE_PATH + "/" + newFilename, resizeImg)
      Map(
        "orgfile" -> (orgFilename + "." + ext),
        "filename" -> newFilename,
        "width" -> resizeImg.getWidth,
        "height" -> resizeImg.getHeight
      )
    }
    else
      Map(
        "filename" -> (orgFilename + "." + ext),
        "width" -> fitImage.getWidth,
        "height" -> fitImage.getHeight
      )
  }

  def getFitImage(image: File) = {
    val in = ImageIO.createImageInputStream(image)
    try {
      val readers = ImageIO.getImageReaders(in)
      if (readers.hasNext()) {
        val reader = readers.next()
        try {
          reader.setInput(in)
          new Dimension(reader.getWidth(0), reader.getHeight(0))
        } finally {
          reader.dispose()
        }
      }
      else null
    } finally {
      if (in != null) in.close()
    }
  }
}