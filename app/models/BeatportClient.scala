package models

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import org.jsoup._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import javax.inject._

class BeatportClient @Inject()(implicit ec: ExecutionContext) extends BeatportClientTrait {

  val browser = JsoupBrowser()

  override def getTop100() : Future[List[(String, String)]] = {
    val page = browser.get("https://www.beatport.com/top-100")
    Future { 
        val tracks = page >> elementList(".buk-track-meta-parent") 
        tracks.map(element => (element.select(".buk-track-title").head.text, element.select(".buk-track-artists").head.text))
    } 
  }

  
}
