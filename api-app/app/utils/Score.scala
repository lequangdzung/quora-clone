package utils

import java.util.Date
import java.lang.Math._

object Score {
  def score(up: Long, down: Long) = up - down

  def hot(up: Long, down: Long, time: Date) = {
    _hot(up, down, time.getTime/1000)
  }

  def _hot(up: Long, down: Long, time: Double) = {
    val s = score(up, down)
    val order = log10(max(abs(s), 1))

    val sign = if(s > 0) 1 else if(s < 0) -1 else 0

    val seconds = time - 1134028003
    round(sign * order + seconds / 45000)
  }

  def hot(followers: Long, date: Date) = {
    val time = date.getTime/1000
    val s = followers
    val order = log10(max(abs(s), 1))

    val sign = if(s > 0) 1 else if(s < 0) -1 else 0

    val seconds = time - 1134028003
    round(sign * order + seconds / 45000)
  }

  def _confidence(ups: Long, downs: Long) = {
    // The confidence sort. http://www.evanmiller.org/how-not-to-sort-by-average-rating.html
    val n = ups + downs

    if (n == 0) 0D
    else {
      val z = 1.281551565545
      val p = ups.toDouble/n

      val left = p + (1D/(2*n)*z*z)

      val right = z*sqrt(p*(1-p)/n + z*z/(4*n*n))
      val under = 1 + 1D/n*z*z

      val result = (left - right) / under

      result
    }
  }

  def confidence(ups: Long, downs: Long) = {
    val up_range = 400
    val down_range = 100
    if(ups + downs == 0) {
      0D
    }
//    else if(ups < up_range && downs < down_range) {
//      _confidence(ups, downs)
//    }
    else {
      _confidence(ups, downs)
    }
  }
}
