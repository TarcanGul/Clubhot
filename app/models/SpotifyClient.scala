package models

import play.api.libs.json._
import play.api.libs.ws._

import javax.inject._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import com.typesafe.config.ConfigFactory
import play.api.Configuration

import scala.collection.Seq
import java.net.InetAddress
import java.util.Base64
import java.util.concurrent.TimeUnit
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration

class SpotifyClient @Inject()(ws: WSClient, config: Configuration)(implicit ec: ExecutionContext) extends SpotifyClientTrait {
  private val clientID = ConfigFactory.load().getString("spotify.api.clientid")
  private val clientSecret = ConfigFactory.load().getString("spotify.api.secret")
  private val baseURI = "https://api.spotify.com/v1"
  private val accountURI = "https://accounts.spotify.com"
  private val callbackURI = ConfigFactory.load().getString("spotify.api.callback")

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
          case _ => "Access token cannot be retrieved";
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
          case 429 => {
            println("Hit 429 from searchTrack, waiting to call again in 1 second.")
            val callFuture = Future {
              return searchTrack(query);
            }

            Await.result(callFuture, Duration.apply(1, TimeUnit.SECONDS))
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

    println("Updating playlist " + playlistID)

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

  def getAudioFeatures(tracks: List[String]): Future[List[Seq[(String, JsValue)]]] = {
    val req : WSRequest = generateSpotifyRequest("/audio-features")
      .addQueryStringParameters("ids" -> tracks.mkString(","))

    println("Getting all the features of the tracks...")
    // analysis_url
    val wantedValues = Set(
      "danceability",
      "energy",
      "instrumentalness",
      "liveness",
      "loudness",
      "speechiness",
      "valence"
    )

    req.get().flatMap {
      r => {
        r.status match {
          case 200 => {
            val allTrackFeaturesMap = (r.body(readableAsJson) \ "audio_features")
              .get
              .as[List[JsValue]] // The result is an array of 100 audio features

            val filteredFeatures : List[Seq[(String, JsValue)]] = allTrackFeaturesMap
              .map(jsValue => jsValue.as[JsObject])
              .map(jsObject => JsObject(jsObject.fields.filter {
                case (key, _) => wantedValues.contains(key)
              }))
              .map(jsObject => jsObject.fields)

            Future.successful(filteredFeatures)
          }
          case 401 => {
            getNewAccessToken().flatMap {
              newToken => getAudioFeatures(tracks)
            }
          }
          case 429 => {
            println("Hit 429 from getAudioFeatures, waiting to call again in 1 second.")
            val callFuture = Future {
              return getAudioFeatures(tracks);
            }

            Await.result(callFuture, Duration.apply(1, TimeUnit.SECONDS))
          }
          case _ => Future.failed(new Exception())
        }
      }
    }
  }
}

