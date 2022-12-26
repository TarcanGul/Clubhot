package controllers

import models.SpotifyClient

import javax.inject._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents, spotifyClient: SpotifyClient)(implicit ec: ExecutionContext) extends BaseController {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */

    val APP_TITLE = "ClubHot"

  def index()  = Action.async { implicit request: Request[AnyContent] =>
    if(request.session.isEmpty) {
      Future.successful(Redirect(spotifyClient.authenticate))
    }
    else {
      request.session.data.foreach( p => println(s"${p._1} : ${p._2}"))
      val spotifyAccessToken = request.session.get("access_token")
      val spotifyRefToken = request.session.get("refresh_token")

      if(spotifyAccessToken.isDefined) {
        val userInfo = spotifyClient.getUserInfo(spotifyAccessToken.get, spotifyRefToken.get)
        userInfo.flatMap {
          userInfo => {
            spotifyClient.getCurrentPlaylists(spotifyAccessToken.get, spotifyRefToken.get).flatMap {
              currentPlaylists => Future.successful(Ok(views.html.index(userInfo, APP_TITLE, currentPlaylists)))
            }
          }
        }(ec)
      } else
        Future.successful(InternalServerError("Token doesn't exist"))
    }
  }
}
