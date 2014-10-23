package service.cache

import java.io._
import scala.concurrent.duration._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import java.util.TimeZone

/**
 * Caches keys with values in MongoDB, implementing the play.api.cache.CacheAPI interface.
 *
 * Internal representation for the stored documents:
 *
 *  - key: String (unique)
 *  - value: Binary (serialized by means of ObjectOutputStream)
 *  - expiryTS: Long (Unix Timestamp in milliseconds, after which the value is considered expired)
 *
 * @param collection is a non-capped MongoDB collection.
 */
final class MongoCache(collection: MongoCollection) extends play.api.cache.CacheAPI {
  import MongoCache.Internals._
  import MongoCache.Keys._

  require(!collection.isCapped, "The given Mongo collection shouldn't be capped")

  collection.ensureIndex(MongoDBObject(Key -> 1), "cache_key", unique = true)
  collection.ensureIndex(MongoDBObject(ExpiryTS -> 1), "cache_expiresTS", unique = false)

  /**
   * Fetches a value based on the given key, if available
   * in MongoDB and is not expired.
   */
  def get(key: String): Option[Any] = {
    for (doc <- getDocument(key); if !doc.isExpired) yield
      doc.value getOrElse null
  }

  /**
   * Sets a (key, value) in the cache. If the key already exists,
   * then it is overwritten. Is atomic.
   */
  def set(key: String, value: Any, expirySecs: Int): Unit =
    saveDocument(key, serialize(value), getExpiryTSFromSecs(expirySecs))

  private[cache] def getDocument(key: String): Option[CacheEntry] = {
    val query = collection.findOne(MongoDBObject(Key -> key))

    for (document <- query) yield {
      val expiryTS = document.get(ExpiryTS).asInstanceOf[Long]
      val value = Option(document.get(Value)).map(_.asInstanceOf[Array[Byte]]).flatMap(deserialize)
      CacheEntry(key, value, expiryTS)
    }
  }

  /**
   * Sets a (key, value) in the cache. If the key already exists,
   * then it is overwritten. Is atomic.
   */
  private[cache] def saveDocument(key: String, value: Array[Byte], expiryTS: Long): Unit = {
    val doc = MongoDBObject(Key -> key, Value -> value, ExpiryTS -> expiryTS)

    collection.findAndModify(
      query = MongoDBObject(Key -> key),
      fields = MongoDBObject.empty,
      sort = MongoDBObject.empty,
      update = doc,
      remove = false,
      returnNew = false,
      upsert = true
    )
  }


  /**
   * Cleans up the cache of expired items. Task must run periodically.
   *
   * @return number of elements removed from collection
   */
  def clean(): Int = {
    val res = collection.remove(ExpiryTS $lte currentTimeMillisUTC)
    Option(res).map(_.getField(UpdatedItemsNR).asInstanceOf[Int]).getOrElse(0)
  }

  /**
   * Removes the given key from the cache, if it exists.
   */
  def remove(key: String): Unit =
    collection.findAndRemove(MongoDBObject(Key -> key))
}

object MongoCache {

  private[cache] object Keys {
    val Key = "key"
    val ExpiryTS = "expiryTS"
    val Value = "value"
  }

  private[cache] object Internals {
    val UpdatedItemsNR = "n"

    /**
     * Returns a Unix Timestamp in milliseconds, using UTC as time-zone
     * (freaking System.currentTimeMillis returns number of millis since
     * 1970-01-01 00:00:00 in local time).
     */
    def currentTimeMillisUTC = {
      System.currentTimeMillis() - TimeZone.getDefault.getRawOffset
    }

    /**
     * Turns an expiry specified in seconds into an Unix Timestamp (millis since Unix Epoch).
     */
    def getExpiryTSFromSecs(expirySecs: Int) =
      if (expirySecs == 0)
        currentTimeMillisUTC + 365.days.toMillis
      else
        currentTimeMillisUTC + expirySecs.seconds.toMillis

    /**
     * Used for deserializing an object from a Binary field.
     */
    def deserialize(value: Array[Byte]): Option[Any] =
      if (value == null)
        None
      else {
        val buf = new ObjectInputStream(new ByteArrayInputStream(value))
        try {
          Option(buf.readObject())
        }
        finally {
          buf.close()
        }
      }

    /**
     * Used for serializing an object to a Binary field.
     */
    def serialize(value: Any): Array[Byte] = {
      val buf = new ByteArrayOutputStream()
      try {
        val out = new ObjectOutputStream(buf)
        try {
          out.writeObject(value)
          out.flush()
          buf.toByteArray
        }
        finally {
          out.close()
        }
      }
      finally {
        buf.close()
      }
    }

    /**
     * For representing stored documents.
     */
    case class CacheEntry(key: String, value: Option[Any], expiryTS: Long) {
      def isExpired =
        currentTimeMillisUTC >= expiryTS
    }
  }
}
