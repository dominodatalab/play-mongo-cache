package service.cache


import com.mongodb.casbah.WriteConcern
import org.specs2.mutable.Specification
import util.Salat._
import play.api.test.FakeApplication

import scala.util.Random

class MongoCacheSpec extends Specification {
  val cache = makeCache

  "get" should {
    "return None if not found" in {
      val key = "test-" + randomAlphanumeric

      val result = cache.get(key)
      result must beNone
    }

    "not return expired values" in {
      val key = "test-" + randomAlphanumeric

      cache.set(key, randomAlphanumeric, -1)
      val result = cache.get(key)

      result must beNone
    }
  }

  "set" should {
    "store a new value" in {
      val key = "test-" + randomAlphanumeric
      val expected = randomAlphanumeric

      cache.set(key, expected, 60)

      val result = cache.get(key)
      result must beSome
      val actual = result.get
      actual must_== expected
    }

    "update a value" in {
      val key = "test-" + randomAlphanumeric
      val first = randomAlphanumeric
      val expected = randomAlphanumeric

      cache.set(key, first, 60)
      cache.set(key, expected, 60)

      val result = cache.get(key)
      result must beSome
      val actual = result.get
      actual must_== expected
    }

    "store Option[String]" in {
      val key = "test-" + randomAlphanumeric
      val expected = randomAlphanumeric

      cache.set(key, Some(expected), 60)

      val result = cache.get(key)
      result must beSome
      val actual = result.get.asInstanceOf[Option[String]]
      actual.get must_== expected
    }
  }

  "remove" should {
    "delete a key/value pair" in {
      val key = "test-" + randomAlphanumeric
      val expected = randomAlphanumeric

      cache.set(key, expected, 60)
      cache.remove(key)

      val result = cache.get(key)
      result must beNone
    }

    "delete a non-existing key" in {
      val key = "test-" + randomAlphanumeric

      cache.remove(key)

      val result = cache.get(key)
      result must beNone
    }
  }

  private def makeCache = {
    implicit val app = new FakeApplication()
    val coll = mongoCollection("cache", writeConcern = WriteConcern.Safe)
    new MongoCache(collection = coll)
  }

  private def randomAlphanumeric = Random.alphanumeric.take(10).mkString

  private case class CacheTestObject(name: String, age: Int, active: Boolean)
}

