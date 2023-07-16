package models

import com.google.inject.ImplementedBy
import scala.concurrent.Future

@ImplementedBy(classOf[BeatportClient])
trait BeatportClientTrait {
  def getTop100() : Future[List[(String, String)]]
}
