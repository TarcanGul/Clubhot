package models

import play.api.libs.json.JsValue
import play.api.libs.ws.{WSRequest, WSResponse, readableAsJson, readableAsString}

import java.util.Base64
import javax.crypto.Cipher
import scala.concurrent.Future
import com.google.inject.ImplementedBy

@ImplementedBy(classOf[SpotifyClient])
trait SpotifyClientTrait {

  var accessToken: String
  var refreshToken: String

  val authenticateURL : String

  def setTokens(aToken: String, rToken: String) : Unit

  def getToken(code: String) : Future[WSResponse]

  def getUserInfo() : Future[JsValue]

  def getCurrentPlaylists() : Future[JsValue]

  def searchTrack(query: String) : Future[Option[JsValue]]

  def createPlaylist(userId: String, playlistName: String, description: String) : Future[JsValue]

  def updatePlaylist(tracks: List[String], playlistID: String) : Future[JsValue]

  //String of URI's.
  def addTracksToPlaylist(tracks: List[String], playlistID: String) : Future[JsValue]

  //String of track ids.
  def getAudioFeatures(tracks: List[String]) : Future[List[JsValue]]
}
