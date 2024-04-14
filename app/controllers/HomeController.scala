package controllers

import models.EncryptorService
import models.BeatportClientTrait
import javax.inject._
import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.collection.Seq
import models.SpotifyClientTrait
import models.structures._
import scala.concurrent.blocking

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
  val RATE_LIMIT = 10
  val PLAYLIST_NAME = "Beatport Top 100!"
  val PLAYLIST_DESC = "Automatically updated playlist."
  val PLAYLIST_ID = "3ELv6AKPfb836HREVat4fX"

  def index() = Action.async { implicit request: Request[AnyContent] =>
    val spotifyAccessToken = request.session.get("access_token")
    val spotifyRefToken = request.session.get("refresh_token")
    
    if (spotifyAccessToken.isEmpty && spotifyRefToken.isEmpty) {
      Future.successful(Redirect(sc.authenticateURL))
    }
    else {
      sc.setTokens(enc.decrypt(spotifyAccessToken.get), enc.decrypt(spotifyRefToken.get))

      sc.getUserInfo().flatMap {
        userInfo => {
          val userId = (userInfo \ "id").get.as[String]
          println(userId)
          for {
            top100 : List[(String, String)] <- bc.getTop100()
            top100Spotify : List[SpotifyTrackResult] <- Future.sequence(top100.map 
              (track =>
                blocking {
                  sc.searchTrack(s"\"${track._1}\" \"${track._2}\"").map {
                    optionValue => {
                      Thread.sleep(500) //For escaping the api limit
                        new SpotifyTrackResult (optionValue.getOrElse(Json.parse("""{"uri" : "No song found."}"""))("uri").as[String], 
                      optionValue.getOrElse(Json.parse("""{"id" : "No song found."}"""))("id").as[String],
                      optionValue.getOrElse(Json.parse("""{"name" : "No song found."}"""))("name").as[String],
                      optionValue.getOrElse(Json.parse(""" { "artists" : [] }"""))("artists").as[List[JsValue]].map(value => value("name").as[String]))
                    }
                    }
                }
              ))
            audioFeatures : List[scala.collection.Seq[(String, JsValue)]] <- sc.getAudioFeatures(top100Spotify.map(trackResult => trackResult.id))
            // updatingPlaylist: JsValue <- sc.updatePlaylist(top100Spotify.map(trackResult => trackResult.uri), PLAYLIST_ID)
          } yield Ok(views.html.index(APP_TITLE, 
          features = audioFeatures, tracks = top100Spotify))
        }
      } 
    }
  }
}
