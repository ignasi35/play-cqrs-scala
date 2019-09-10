/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.scaladsl.cqrs

import org.specs2.mutable._
import akka.persistence.typed.PersistenceId

class TaggerSpec extends Specification {

  "A Tagger" should {

    val persistenceId = "dummy-test-id"

    "return a sharded tag" in {
      val tagger      = Tagger[TestEvent].addTagGroup("TagA", 10)
      val shardedTags = tagger.tagFunction(persistenceId)(TestEventA)
      shardedTags must beEqualTo(Set("TagA9"))
    }

    "return sharded tags starting from 0" in {
      val tagger = Tagger[TestEvent].addTagGroup("TagA", 3)
      val tags   = tagger.allShardedTags("TagA")
      tags must haveSize(3)
      tags must_== Set("TagA0", "TagA1", "TagA2")
    }

    "return a non-sharded tag when no shard number is provided" in {
      val tagger         = Tagger[TestEvent].addTagGroup("TagA")
      val allShardedTags = tagger.tagFunction(persistenceId)(TestEventA)
      allShardedTags must beEqualTo(Set("TagA"))
    }

    "return set of sharded tags when declaring different tag groups" in {
      val taggers = Tagger[TestEventA.type].addTagGroup("TagA", 10).addTagGroup("TagB", 6).addTagGroup("TagC")

      val tags = taggers.tagFunction(persistenceId)(TestEventA)
      tags must haveSize(3)
      tags must_== Set("TagA9", "TagB3", "TagC")
    }

    "hounour each tag group predicate" in {

      val predicateForA: TestEvent => Boolean = {
        case TestEventA => true
        case _          => false
      }

      val predicateForB: TestEvent => Boolean = {
        case TestEventB => true
        case _          => false
      }

      val taggers = Tagger[TestEvent]
        .addTagGroup("TagA", 10, predicateForA) // only tag TestEventA
        .addTagGroup("TagB", 6, predicateForB)  // only tag TestEventB

      val tagsForA = taggers.tagFunction(persistenceId)(TestEventA)
      tagsForA must haveSize(1)
      tagsForA must_== Set("TagA9")

      val tagsForB = taggers.tagFunction(persistenceId)(TestEventB)
      tagsForB must haveSize(1)
      tagsForB must_== Set("TagB3")
    }

    "fail when passed a negative shard number" in {
      Tagger[TestEvent].addTagGroup("TagA", -10) must throwA[IllegalArgumentException]
    }

    "fail when passed an empty string" in {
      Tagger[TestEvent].addTagGroup("") must throwA[IllegalArgumentException]
      Tagger[TestEvent].addTagGroup("  ") must throwA[IllegalArgumentException]
    }

    "return a sharded tag (with custom tag group)" in {
      val tagger      = Tagger[TestEvent].addTagGroup(customTagGroup)
      val shardedTags = tagger.tagFunction(persistenceId)(TestEventA)
      shardedTags must beEqualTo(Set("MyTag-0"))

    }

    "'allShardedTags' hounour custom tagger format" in {
      val tagger         = Tagger[TestEvent].addTagGroup(customTagGroup)
      val allShardedTags = tagger.allShardedTags(customTagGroup.originalTag)
      allShardedTags must beEqualTo(Set("MyTag-0", "MyTag-1", "MyTag-2"))
    }
  }

  sealed trait TestEvent
  case object TestEventA extends TestEvent
  case object TestEventB extends TestEvent

  val customTagGroup = new TagGroup[TestEvent] {
    val numOfShards: Int                = 3
    val originalTag: String             = "MyTag"
    val predicate: TestEvent => Boolean = _ => true

    final def shardTag(shardNum: Int): String =
      if (numOfShards > 1) s"$originalTag-$shardNum"
      else originalTag

    final def tagFunction(persistenceId: String): TestEvent => Option[String] =
      evt => {
        if (predicate(evt)) {
          val tag =
            if (numOfShards > 1) shardTag(Math.abs(persistenceId.hashCode % numOfShards))
            else originalTag
          Some(tag)
        } else None
      }
  }
}
