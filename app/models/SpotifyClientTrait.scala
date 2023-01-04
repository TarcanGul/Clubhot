package models

import play.api.libs.json.JsValue
import play.api.libs.ws.{WSRequest, WSResponse, readableAsJson, readableAsString}

import java.util.Base64
import javax.crypto.Cipher
import scala.concurrent.Future

trait SpotifyClientTrait {

  var accessToken: String
  var refreshToken: String

  def authenticate : String

  def setTokens(aToken: String, rToken: String) : Unit

  def getToken(code: String) : Future[WSResponse]

  def getNewAccessToken(): Future[String]

  def getUserInfo() : Future[String]

  def getCurrentPlaylists() : Future[JsValue]
}
