/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.ccn2connectivitytester.repositories

import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.UUID.randomUUID

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.ReadPreference.primaryPreferred
import org.mongodb.scala.bson.{BsonBoolean, BsonInt64}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.ACCEPTED
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.ccn2connectivitytester.models
import uk.gov.hmrc.ccn2connectivitytester.models.{SendingStatus, SoapMessageStatus}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport


class SoapMessageStatusRepositoryISpec extends AnyWordSpec with PlayMongoRepositorySupport[SoapMessageStatus] with
  Matchers with BeforeAndAfterEach with GuiceOneAppPerSuite with IntegrationPatience {
  val serviceRepo = repository.asInstanceOf[SoapMessageStatusRepository]

  override implicit lazy val app: Application = appBuilder.build()
  val ccnHttpStatus: Int = 200
  val sentStatusMessage = SoapMessageStatus(randomUUID, "some message ID", SendingStatus.SENT, ccnHttpStatus)
  val failedStatusMessage = SoapMessageStatus(randomUUID, "some message ID", SendingStatus.FAILED, ccnHttpStatus)
  val retryingStatusMessage = SoapMessageStatus(randomUUID, "some message ID", SendingStatus.RETRYING, ccnHttpStatus)
  implicit val materialiser: Materializer = app.injector.instanceOf[Materializer]

  override def beforeEach(): Unit = {
    prepareDatabase()
  }

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}",
        "metrics.enabled" -> false
      )

  override protected def repository: PlayMongoRepository[SoapMessageStatus] = app.injector.instanceOf[SoapMessageStatusRepository]

  "persist" should {
    "insert a sent message when it does not exist" in {
      await(serviceRepo.persist(sentStatusMessage))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred()).find().toFuture())

      fetchedRecords.size shouldBe 1
      fetchedRecords.head shouldBe sentStatusMessage
      fetchedRecords.head.status shouldBe SendingStatus.SENT
    }

    "insert a failed message when it does not exist" in {
      await(serviceRepo.persist(failedStatusMessage))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred).find().toFuture())

      fetchedRecords.size shouldBe 1
      fetchedRecords.head shouldBe failedStatusMessage
      fetchedRecords.head.status shouldBe SendingStatus.FAILED
    }

    "insert a retrying message when it does not exist" in {
      await(serviceRepo.persist(retryingStatusMessage))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred).find().toFuture())

      fetchedRecords.size shouldBe 1
      fetchedRecords.head shouldBe retryingStatusMessage
      fetchedRecords.head.status shouldBe SendingStatus.RETRYING
    }
    "message is persisted with TTL" in {
      await(serviceRepo.persist(sentStatusMessage))

      val Some(ttlIndex) = await(serviceRepo.collection.listIndexes().toFuture()).find(i => i.get("name").get.asString().getValue == "ttlIndex")
      ttlIndex.get("unique") shouldBe None
      ttlIndex.get("background").get shouldBe BsonBoolean(true)
      ttlIndex.get("expireAfterSeconds") shouldBe Some(BsonInt64(60 * 60 * 24 * 30))
    }

    "message is persisted with unique ID" in {
      await(serviceRepo.persist(sentStatusMessage))

      val Some(globalIdIndex) = await(serviceRepo.collection.listIndexes().toFuture()).find(i => i.get("name").get.asString().getValue == "globalIdIndex")
      globalIdIndex.get("unique") shouldBe Some(BsonBoolean(true))
      globalIdIndex.get("background").get shouldBe BsonBoolean(true)
    }

    "create index on message ID" in {
      await(serviceRepo.persist(sentStatusMessage))

      val Some(globalIdIndex) = await(serviceRepo.collection.listIndexes().toFuture()).find(i => i.get("name").get.asString().getValue == "messageIdIndex")
      globalIdIndex.get("unique") shouldBe None
      globalIdIndex.get("background").get shouldBe BsonBoolean(true)
    }

    "fail when a message with the same ID already exists" in {
      await(serviceRepo.persist(sentStatusMessage))

      val exception: MongoWriteException = intercept[MongoWriteException] {
        await(serviceRepo.persist(sentStatusMessage))
      }

      exception.getMessage should include("E11000 duplicate key error collection")
    }
  }

  "updateStatus" should {
    "update the message to have a status of FAILED" in {
      await(serviceRepo.persist(retryingStatusMessage))
      val Some(returnedStatusMessage) = await(serviceRepo.updateSendingStatus(retryingStatusMessage.messageId, SendingStatus.FAILED))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred).find.toFuture())
      fetchedRecords.size shouldBe 1
      fetchedRecords.head.status shouldBe SendingStatus.FAILED
      fetchedRecords.head.isInstanceOf[SoapMessageStatus] shouldBe true
      returnedStatusMessage.status shouldBe SendingStatus.FAILED
      returnedStatusMessage.isInstanceOf[SoapMessageStatus] shouldBe true
    }

    "update the message to have a status of SENT" in {
      await(serviceRepo.persist(retryingStatusMessage))
      val Some(returnedStatusMessage) = await(serviceRepo.updateSendingStatus(retryingStatusMessage.messageId, SendingStatus.SENT))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred).find.toFuture())
      fetchedRecords.size shouldBe 1
      fetchedRecords.head.status shouldBe SendingStatus.SENT
      fetchedRecords.head.isInstanceOf[SoapMessageStatus] shouldBe true
      returnedStatusMessage.status shouldBe SendingStatus.SENT
      returnedStatusMessage.isInstanceOf[SoapMessageStatus] shouldBe true
    }
    "update the message to have a status of CoD" in {
      await(serviceRepo.persist(retryingStatusMessage))
      val Some(returnedStatusMessage) = await(serviceRepo.updateSendingStatus(retryingStatusMessage.messageId, SendingStatus.COD))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred).find.toFuture())
      fetchedRecords.size shouldBe 1
      fetchedRecords.head.status shouldBe SendingStatus.COD
      fetchedRecords.head.isInstanceOf[SoapMessageStatus] shouldBe true
      returnedStatusMessage.status shouldBe SendingStatus.COD
      returnedStatusMessage.isInstanceOf[SoapMessageStatus] shouldBe true
    }
    "update the message to have a status of CoE" in {
      await(serviceRepo.persist(retryingStatusMessage))
      val Some(returnedStatusMessage) = await(serviceRepo.updateSendingStatus(retryingStatusMessage.messageId, SendingStatus.COE))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred).find.toFuture())
      fetchedRecords.size shouldBe 1
      fetchedRecords.head.status shouldBe SendingStatus.COE
      fetchedRecords.head.isInstanceOf[SoapMessageStatus] shouldBe true
      returnedStatusMessage.status shouldBe SendingStatus.COE
      returnedStatusMessage.isInstanceOf[SoapMessageStatus] shouldBe true
    }
  }

  "findById" should {
    "find a message that exists" in {
      await(serviceRepo.persist(sentStatusMessage))

      val Some(returnedMessage) = await(serviceRepo.findById(sentStatusMessage.messageId))

      returnedMessage.messageId shouldBe sentStatusMessage.messageId
    }

    "not find a message that does not exist" in {
      val result = await(serviceRepo.findById(sentStatusMessage.messageId))

      result shouldBe Option.empty
    }
  }

  "retrieveMessagesMissingConfirmation" should {
    "only retrieve messages with expired confirmation wait" in {
      val unconfirmedGlobalId = UUID.randomUUID()
      val unconfirmedMessageId = "second message ID"
      val unconfirmedCreated = java.time.Instant.now().minus(Period.ofDays(5))
      await(serviceRepo.persist(sentStatusMessage))
      await(serviceRepo.persist(new models.SoapMessageStatus(unconfirmedGlobalId, unconfirmedMessageId, SendingStatus.SENT, ACCEPTED, unconfirmedCreated)))

      val retrieved = await(serviceRepo.retrieveMessagesMissingConfirmation.runWith(Sink.seq[SoapMessageStatus]))

      retrieved.size shouldBe 1
      retrieved.head.globalId shouldBe unconfirmedGlobalId
      retrieved.head.messageId shouldBe unconfirmedMessageId
      retrieved.head.ccnHttpStatus shouldBe ACCEPTED
      retrieved.head.createDateTime shouldBe unconfirmedCreated.truncatedTo(ChronoUnit.MILLIS)
    }

    "only retrieve messages with a status of SENT " in {
      val unconfirmedMessageId = "message to ignore"
      val unconfirmedCreated = java.time.Instant.now().minus(Period.ofDays(5))
      await(serviceRepo.persist(sentStatusMessage))
      await(serviceRepo.persist(new models.SoapMessageStatus(UUID.randomUUID(), s"${unconfirmedMessageId}-1", SendingStatus.FAILED, ACCEPTED, unconfirmedCreated)))
      await(serviceRepo.persist(new models.SoapMessageStatus(UUID.randomUUID(), s"${unconfirmedMessageId}-2", SendingStatus.RETRYING, ACCEPTED, unconfirmedCreated)))
      await(serviceRepo.persist(new models.SoapMessageStatus(UUID.randomUUID(), s"${unconfirmedMessageId}-3", SendingStatus.COE, ACCEPTED, unconfirmedCreated)))
      await(serviceRepo.persist(new models.SoapMessageStatus(UUID.randomUUID(), s"${unconfirmedMessageId}-4", SendingStatus.COD, ACCEPTED, unconfirmedCreated)))

      val retrieved = await(serviceRepo.retrieveMessagesMissingConfirmation.runWith(Sink.seq[SoapMessageStatus]))

      retrieved.size shouldBe 0
    }
  }
}


