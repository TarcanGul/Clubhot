 package models.structures

 import play.api.libs.json.JsValue

 case class SpotifyTrackResult(uri: String, id: String, name: String, artists: List[String]) 
