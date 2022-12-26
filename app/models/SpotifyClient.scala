package models

import com.typesafe.config.ConfigFactory
import play.api.libs.json.JsValue
import play.api.libs.ws._

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject._
import play.api.mvc.Cookies
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

class SpotifyClient @Inject()(ws: WSClient)(implicit ec: ExecutionContext) {
  val clientID = ConfigFactory.load().getString("spotify.api.clientid")
  val clientSecret = ConfigFactory.load().getString("spotify.api.secret")
  val baseURI = "https://api.spotify.com/v1"
  val accountURI = "https://accounts.spotify.com"
  val callbackURI = "http://localhost:9000/oauth2/callback"

  val encryptionKeyBytes = ConfigFactory.load().getString("token.encryption.key").getBytes()
  val secretKey = new SecretKeySpec(encryptionKeyBytes, "AES")

  var accessToken = ""
  var refToken = ""

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
  def getNewAccessToken(): Future[String] = {
    val req : WSRequest = ws.url(accountURI + "/api/token")
      .addHttpHeaders("Authorization" ->  s"Basic ${Base64.getEncoder.encodeToString((clientID + ':' + clientSecret).getBytes)}")
      .addHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded")
    req.post(Map("grant_type" -> "refresh_token", "refresh_token" -> decrypt(refToken))).map {
      r => {
        r.status match {
          case 200 => (r.json \ "access_token").as[String]
        }
      }
    }
  }

  private def generateSpotifyRequest(path: String) : WSRequest =
    ws.url(baseURI + path)
      .addHttpHeaders("Authorization" -> s"Bearer ${decrypt(accessToken)}")
      .addHttpHeaders("Content-Type"-> "application/json")

  //can you implement this without getting tokens as parameter?
  def getUserInfo() : Future[String]  = {
    val req : WSRequest = generateSpotifyRequest("/me")
    req.get().flatMap {
      r => {
        r.status match {
          case 200 => Future.successful(r.body(readableAsString))
          case 401 => {
            getNewAccessToken().flatMap {
              newToken => getUserInfo()
            }
          }
          case _ => Future.successful(r.body)
        }
      }
    }
    }

  def getCurrentPlaylists() : Future[JsValue] = {
    val req : WSRequest = generateSpotifyRequest("/me/playlists")
    req.get().flatMap {
      r => {
        r.status match {
          case 200 => Future.successful(r.body(readableAsJson))
          case 401 => {
            getNewAccessToken().flatMap {
              newToken => getCurrentPlaylists()
            }
          }
          case _ => Future.successful(r.body(readableAsJson))
        }
      }
    }
  }

  def encrypt(plaintext: String): String = {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    cipher.doFinal(plaintext.getBytes).toString
  }

  def decrypt(ciphertext: String): String = {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, secretKey)
    new String(cipher.doFinal(ciphertext.getBytes))
  }

}

