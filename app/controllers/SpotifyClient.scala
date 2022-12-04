package controllers

import play.api.mvc._
import play.api._
import play.api.libs.ws._
import scala.concurrent.Future
import akka.http.scaladsl.model.HttpHeader
import javax.inject._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Using}
import java.io.FileInputStream
import scala.io.Source
import play.api.libs.json.Json
import com.typesafe.config.ConfigFactory

@Singleton
class SpotifyClient @Inject()(ws: WSClient, val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext) extends BaseController {
  val clientID = ConfigFactory.load().getString("spotify.api.clientid")
  val clientSecret = ConfigFactory.load().getString("spotify.api.secret")
  val baseURI = "https://api.spotify.com/v1"
  val accountURI = "https://accounts.spotify.com"
  val callbackURI = "http://localhost:8084/callback"

  def setup : Unit = {
    authenticate.onComplete {
        case Success(value) => 
        case Failure(error) =>
    }
  }

  def authenticate : Future[WSResponse] = {
    val authReq : WSRequest = ws.url(accountURI + "/authorize")
    .addQueryStringParameters("client_id" -> clientID)
    .addQueryStringParameters("response_type" -> "code")
    .addQueryStringParameters("redirect_uri" -> callbackURI)

    val res : Future[WSResponse] = authReq.get()
    res
  }

  /*def getToken : Future[WSResponse] = {
    val tokenReq : WSRequest = ws.url(accountURI + "/api/token")
  }*/

  def test() = Action {implicit request: Request[AnyContent] => 
    Ok
  }
}
