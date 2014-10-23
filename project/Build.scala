import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "play-mongo-cache"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    "se.radley" %% "play-plugins-salat" % "1.4.0"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
