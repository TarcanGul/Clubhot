package controllers

import models.EncryptorService
import models.BeatportClientTrait
import javax.inject._
import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import models.SpotifyClientTrait

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents, val sc: SpotifyClientTrait, val bc: BeatportClientTrait, enc: EncryptorService)(implicit ec: ExecutionContext) extends BaseController {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */

  val APP_TITLE = "ClubHot"

  def index() = Action.async { implicit request: Request[AnyContent] =>
    val spotifyAccessToken = request.session.get("access_token")
    val spotifyRefToken = request.session.get("refresh_token")
    
    if (spotifyAccessToken.isEmpty && spotifyRefToken.isEmpty) {
      Future.successful(Redirect(sc.authenticateURL))
    }
    else {   
      sc.setTokens(enc.decrypt(spotifyAccessToken.get), enc.decrypt(spotifyRefToken.get))
      for {
        userInfo : String <- sc.getUserInfo()
        top100 : List[(String, String)] <- bc.getTop100()
        top100Spotify : List[JsValue] <- Future.sequence(top100.map (track => sc.searchTrack(s"\"${track._1}\" ${track._2}").map {track => (track \ "tracks" \ "items").as[List[JsValue]].head("external_urls")("spotify")}))
      } yield Ok(views.html.index(userInfo, APP_TITLE, top100Spotify))
    }
  }
}
