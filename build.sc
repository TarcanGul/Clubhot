import mill._
import $ivy.`com.lihaoyi::mill-contrib-playlib:`,  mill.playlib._

object clubhot extends PlayModule with SingleModule {

  def scalaVersion = "2.13.10"
  def playVersion = "2.8.18"
  def twirlVersion = "1.5.1"

  object test extends PlayTests
}
