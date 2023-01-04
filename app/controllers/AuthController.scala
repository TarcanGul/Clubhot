package controllers

import models.SpotifyClientTrait
import models.EncryptorService
import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Cookie, Request, Result}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class AuthController @Inject()(val controllerComponents: ControllerComponents, spotifyClient: SpotifyClientTrait, enc: EncryptorService)(implicit ec: ExecutionContext) extends BaseController {

  /**
   Route: /oauth2/callback
   */
  def callback() = Action.async {implicit request: Request[AnyContent] =>
    val authCode = request.getQueryString("code")
    println("callback called")
    spotifyClient.getToken(authCode.get).map { response => {
      val accessToken = (response.json \ "access_token").as[String]
      val refreshToken = (response.json \ "refresh_token").as[String]

      Redirect("/").withSession(("access_token" -> enc.encrypt(accessToken)), ("refresh_token" -> enc.encrypt(refreshToken)))
    }
    }
  }
}