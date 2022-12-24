package controllers

import javax.inject._
import play.api._
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents, spotifyClient: SpotifyClient) extends BaseController {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    if(request.session.isEmpty) {
      Redirect("/auth")
    }
    else {
      request.session.data.foreach( p => println(s"${p._1} : ${p._2}"))
      var spotifyAccessToken = request.session.get("access_token")
      if(spotifyAccessToken.isDefined) {
        val userInfo = spotifyClient.getUserInfo(spotifyAccessToken.get)
        val playlists = spotifyClient.getCurrentPlaylists(spotifyAccessToken.get)
        Ok(views.html.index(userInfo, playlists))
      } else
        NotFound("Token doesn't exist")
    }
  }
}
