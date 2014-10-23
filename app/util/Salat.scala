package util

import play.api._
import se.radley.plugin.salat.SalatPlugin
import com.mongodb.casbah.MongoCollection
import com.mongodb.WriteConcern
import com.mongodb.util.JSON
import com.mongodb.casbah.Imports._

object Salat {
  val DefaultWriteConcern: WriteConcern =
    WriteConcern.SAFE

  /**
   * Returns a MongoCollection
   * @param collectionName The MongoDB collection name
   * @param sourceName The configured source name
   * @return MongoCollection
   */
  def mongoCollection(collectionName: String, sourceName:String = "default", writeConcern: WriteConcern = DefaultWriteConcern)(implicit app: Application): MongoCollection = {
    val coll = app.plugin[SalatPlugin].map(_.collection(collectionName, sourceName))
      .getOrElse(throw new PlayException("SalatPlugin is not registered.",
        "You need to register the plugin with \"500:se.radley.plugin.salat.SalatPlugin\" in conf/play.plugins"))
    coll.setWriteConcern(writeConcern)
    coll
  }

  /**
   * Returns a capped MongoCollection
   * @param collectionName The MongoDB collection name
   * @param size The capped collection size
   * @param max the capped collection max number of documents
   * @param sourceName The configured source name
   * @return MongoCollection
   */
  def mongoCappedCollection(collectionName: String, size: Int, max: Option[Long] = None, sourceName:String = "default", writeConcern: WriteConcern = DefaultWriteConcern)(implicit app: Application): MongoCollection = {
    val coll = app.plugin[SalatPlugin].map(_.cappedCollection(collectionName, size, max, sourceName))
      .getOrElse(throw new PlayException("SalatPlugin is not registered.",
        "You need to register the plugin with \"500:se.radley.plugin.salat.SalatPlugin\" in conf/play.plugins"))
    coll.setWriteConcern(writeConcern)
    coll
  }
}
