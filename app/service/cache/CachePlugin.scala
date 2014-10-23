package service.cache

import play.api.{Logger, Application}
import play.api.cache.CacheAPI
import scala.volatile
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import concurrent.duration._
import akka.actor.Cancellable
import util.Salat.mongoCollection
import com.mongodb.casbah.WriteConcern

class CachePlugin(implicit app: Application) extends play.api.cache.CachePlugin {
  override def onStart(): Unit = {
    logger.info("Initializing CachePlugin")

    val coll = mongoCollection("cache", writeConcern = WriteConcern.Safe)
    val cache = new MongoCache(collection = coll)
    api = cache

    // scheduling cleanup
    task = Akka.system.scheduler.schedule(3.seconds, 60.seconds) {
      val cleanedNr = cache.clean()
      logger.debug(s"[CachePlugin] performing cleanup ($cleanedNr removed keys)")
    }
  }

  override def onStop(): Unit = {
    if (task != null) task.cancel()
  }

  @volatile var api: CacheAPI = null
  @volatile var task: Cancellable = null

  override val enabled = true
  lazy val logger = Logger("application")
}


