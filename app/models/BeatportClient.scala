package models

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.model.Element
import play.api.libs.json.JsLookupResult.jsLookupResultToJsLookup
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import javax.inject._

class BeatportClient @Inject()(implicit ec: ExecutionContext) extends BeatportClientTrait {

  private val browser = JsoupBrowser()

  override def getTop100() : Future[List[(String, String)]] = {
    val page = browser.get("https://www.beatport.com/top-100")
    val props : Element = page >> element("script#__NEXT_DATA__");
    val propValues : JsValue = Json.parse(props.innerHtml);
    val tracks : JsLookupResult = propValues \ "props" \ "pageProps" \ "dehydratedState" \ "queries" \ 0 \ "state" \ "data" \ "results";

    val names = tracks.get.asInstanceOf[JsArray].value.map(_ \ "name");
    val artists = tracks.get.asInstanceOf[JsArray].value.map(_ \ "artists" \\ "name");
    Future {
      names.zip(artists).toList.map(nameAndArtists => (nameAndArtists._1.get.toString(), nameAndArtists._2.mkString(" ")))
    }
  }
}
