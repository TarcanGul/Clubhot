package models

import play.api.libs.json._
import play.api.libs.ws._

import javax.inject._
import play.api.mvc.Cookies
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import com.typesafe.config.ConfigFactory
import java.util.Base64

class SpotifyClient @Inject()(ws: WSClient)(implicit ec: ExecutionContext) extends SpotifyClientTrait {
  val clientID = ConfigFactory.load().getString("spotify.api.clientid")
  val clientSecret = ConfigFactory.load().getString("spotify.api.secret")
  val baseURI = "https://api.spotify.com/v1"
  val accountURI = "https://accounts.spotify.com"
  val callbackURI = "http://localhost:9000/oauth2/callback"

  var accessToken: String = ""
  var refreshToken: String = ""

  val authenticateURL: String = 
    ws.url(accountURI + "/authorize")
    .addQueryStringParameters("client_id" -> clientID)
    .addQueryStringParameters("response_type" -> "code")
    .addQueryStringParameters("redirect_uri" -> callbackURI)
    .addQueryStringParameters("scope" -> "playlist-modify-private")
    .uri.toString()

  def setTokens(aToken: String, rToken: String) : Unit = {
    accessToken = aToken
    refreshToken = rToken
  }

  def getToken(code: String) : Future[WSResponse] = {
    val tokenReq : WSRequest = ws.url(accountURI + "/api/token")
      .addHttpHeaders("Authorization" ->  s"Basic ${Base64.getEncoder.encodeToString((clientID + ':' + clientSecret).getBytes)}")
      .addHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded")
    tokenReq.post(Map("code" -> code, "grant_type" -> "authorization_code", "redirect_uri" -> callbackURI))
  }

  private def getNewAccessToken(): Future[String] = {
    val req : WSRequest = ws.url(accountURI + "/api/token")
      .addHttpHeaders("Authorization" ->  s"Basic ${Base64.getEncoder.encodeToString((clientID + ':' + clientSecret).getBytes)}")
      .addHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded")
    req.post(Map("grant_type" -> "refresh_token", "refresh_token" -> refreshToken)).map {
      r => {
        r.status match {
          case 200 => (r.json \ "access_token").as[String]
        }
      }
    }
  }

  private def generateSpotifyRequest(path: String) : WSRequest =
    ws.url(baseURI + path)
      .addHttpHeaders("Authorization" -> s"Bearer ${accessToken}")
      .addHttpHeaders("Content-Type"-> "application/json")

  def getUserInfo() : Future[JsValue]  = {
    val req : WSRequest = generateSpotifyRequest("/me")
    req.get().flatMap {
      r => {
        r.status match {
          case 200 => Future.successful(r.body(readableAsJson))
          case 401 => {
            getNewAccessToken().flatMap {
              newToken => getUserInfo()
            }
          }
          case _ => Future.successful(r.body(readableAsJson))
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

  def searchTrack(query: String) : Future[Option[JsValue]] = {
    val req : WSRequest = generateSpotifyRequest("/search")
      .addQueryStringParameters("query" -> query)
      .addQueryStringParameters("type" -> "track")
      .addQueryStringParameters("limit" -> "1")
    
    req.get().flatMap {
      r => {
        r.status match {
          case 200 => Future.successful((r.body(readableAsJson) \ "tracks" \ "items").as[List[JsValue]].headOption)
          case 401 => {
            getNewAccessToken().flatMap {
              newToken => searchTrack(query)
            }
          }
          case _ => Future.failed(throw new Exception(r.body(readableAsString)))
        }
      }
    }
  }

  def createPlaylist(userId : String, playlistName: String, description: String): Future[JsValue] = {
    val req : WSRequest = generateSpotifyRequest(s"/users/${userId}/playlists")

    val body = Json.obj(
      ("name" -> playlistName),
      ("description" -> description),
      ("public" -> false)
    )

    req.post(body).flatMap {
      r => {
        r.status match {
          case 201 => Future.successful((r.body(readableAsJson)))
          case 401 => {
            getNewAccessToken().flatMap {
              newToken => createPlaylist(userId, playlistName, description)
            }
          }
          case _ => Future.successful((r.body(readableAsJson)))
        }
      }
    }
  } 

  def addTracksToPlaylist(tracks: List[String], playlistID: String): Future[JsValue] = {
    val req : WSRequest = generateSpotifyRequest(s"/playlists/${playlistID}/tracks")

    val body = Json.obj(
      ("uris" -> Json.toJson(tracks))
    )

    req.post(body).flatMap {
      r => {
        r.status match {
          case 201 => Future.successful((r.body(readableAsJson)))
          case 401 => {
            getNewAccessToken().flatMap {
              newToken => addTracksToPlaylist(tracks, playlistID)
            }
          }
          case _ => Future.successful((r.body(readableAsJson)))
        }
      }
    }
  }

  def updatePlaylist(tracks: List[String], playlistID: String): Future[JsValue] = {
    val req : WSRequest = generateSpotifyRequest(s"/playlists/${playlistID}/tracks")

    val body = Json.obj(
      ("uris" -> Json.toJson(tracks))
    )

    req.put(body).flatMap {
      r => {
        r.status match {
          case 200 => Future.successful((r.body(readableAsJson)))
          case 401 => {
            getNewAccessToken().flatMap {
              newToken => updatePlaylist(tracks, playlistID)
            }
          }
          case _ => Future.successful((r.body(readableAsJson)))
        }
      }
    }
  }

  def getAudioFeatures(tracks: List[String]): Future[JsValue] = {
    val req : WSRequest = generateSpotifyRequest("/audio-features")
      .addQueryStringParameters("ids" -> tracks.mkString(","))

    req.get().flatMap {
      r => {
        r.status match {
          case 200 => Future.successful((r.body(readableAsJson)))
          case 401 => {
            getNewAccessToken().flatMap {
              newToken => getAudioFeatures(tracks)
            }
          }
          case _ => Future.successful((r.body(readableAsJson)))
        }
      }
    }
  }
}

