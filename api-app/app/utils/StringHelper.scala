package utils

import org.jsoup.Jsoup
import java.util.UUID

import org.apache.commons.codec.binary.Base64

object StringHelper {
  def stripHtml(in: String) = {
    val text = Jsoup.parse(in).text()

    getShortText(text, math.min(text.length -1, 200))
  }

  def getShortText(in: String, number: Int) = {
    val stripedText = in.substring(0, number - 1)
    val words = stripedText.split("""\s+""")

    words.slice(0, words.length).mkString(" ")
  }

  def toCassKey(in: String) = {
    val charReg = """[\"\'`\.,:;\{\}\[\]\+=\*\^&%\$#\!\~\(\)\?<>/\\|]"""
    var ret = in.replaceAll(charReg, "")

    val dashReg = """[_\s]"""
    ret = ret.replaceAll(dashReg, "-")

    ret
  }

  def genUniqueFilename(userId: UUID, filename: String) = {
    Base64.encodeBase64URLSafeString((userId.toString + filename + System.currentTimeMillis().toString).getBytes)
//    val md = java.security.MessageDigest.getInstance("SHA-1")
//    val ha = new sun.misc.BASE64Encoder().encode(md.digest((userId.toString + filename + System.currentTimeMillis().toString).getBytes))
//    ha
  }
}
