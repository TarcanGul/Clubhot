package controllers

import play.api.mvc._
import play.api._
import play.api.libs.ws._

import scala.concurrent.{Await, ExecutionContext, Future}
import akka.http.scaladsl.model.HttpHeader

import javax.inject._
import scala.util.{Failure, Success, Using}
import java.io.FileInputStream
import scala.io.Source
import play.api.libs.json.{JsValue, Json}
import com.typesafe.config.ConfigFactory
import play.twirl.api.Html

import java.util.Base64
import java.nio.charset.StandardCharsets
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

@Singleton
class SpotifyClient @Inject()(ws: WSClient, val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext) extends BaseController {
  val clientID = ConfigFactory.load().getString("spotify.api.clientid")
  val clientSecret = ConfigFactory.load().getString("spotify.api.secret")
  val baseURI = "https://api.spotify.com/v1"
  val accountURI = "https://accounts.spotify.com"
  val callbackURI = "http://localhost:9000/oauth2/callback"

  def authenticate: String  = {
    val authReq : WSRequest = ws.url(accountURI + "/authorize")
    .addQueryStringParameters("client_id" -> clientID)
    .addQueryStringParameters("response_type" -> "code")
    .addQueryStringParameters("redirect_uri" -> callbackURI)

    authReq.uri.toString
  }

  def getToken(code: String) : Future[WSResponse] = {
    val tokenReq : WSRequest = ws.url(accountURI + "/api/token")
      .addHttpHeaders("Authorization" ->  s"Basic ${Base64.getEncoder.encodeToString((clientID + ':' + clientSecret).getBytes)}")
      .addHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded")
    tokenReq.post(Map("code" -> code, "grant_type" -> "authorization_code", "redirect_uri" -> callbackURI))
  }

  //TODO: implement
  def getRefreshToken(): Unit = {

  }

  def callback() = Action.async {implicit request: Request[AnyContent] =>
    val authCode = request.getQueryString("code")
    println("callback called")
    getToken(authCode.get).map { response => {
      println(response.json.toString())
      val accessToken = (response.json \ "access_token").as[String]
      val refreshToken = (response.json \ "refresh_token").as[String]
      Redirect("/").withSession(("access_token" -> accessToken), ("refresh_token" -> refreshToken), ("spotify_auth" -> "true"))
      }
    }
  }

  def init() = Action { implicit request: Request[AnyContent] =>
    Redirect(authenticate)
  }

  private def generateSpotifyRequest(path: String, token: String) : WSRequest =
    ws.url(baseURI + path)
      .addHttpHeaders("Authorization" -> s"Bearer ${token}")
      .addHttpHeaders("Content-Type"-> "application/json")

  def getUserInfo(token: String) : String = {
    val req : WSRequest = generateSpotifyRequest("/me", token)
    val res = req.get().map {
      r => r.body
    }
    Await.result(res, atMost = 10 seconds)
  }

  def getCurrentPlaylists(token: String) : JsValue = {
    val req : WSRequest = generateSpotifyRequest("/me/playlists", token)
    val res = req.get().map {
      r => r.json
    }
    Await.result(res, atMost = 10 seconds)
  }

  /* Gets tracks' analysis */
  def extractInfoInPlaylist(token: String) = Action { implicit request: Request[AnyContent] =>
    /*TODO*/
    Ok("TODO")
  }
    
}

