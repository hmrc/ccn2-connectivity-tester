/*
 * Copyright 2023 HM Revenue & Customs
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

import java.time.Instant
import java.time.Instant.now
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import akka.NotUsed
import akka.stream.alpakka.mongodb.scaladsl.MongoSource
import akka.stream.scaladsl.Source
import org.mongodb.scala.ReadPreference.primaryPreferred
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters.{equal, or, _}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model._
import org.mongodb.scala.result.InsertOneResult

import play.api.Logging
import play.api.libs.json.Format
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import uk.gov.hmrc.ccn2connectivitytester.config.AppConfig
import uk.gov.hmrc.ccn2connectivitytester.models.SendingStatus._
import uk.gov.hmrc.ccn2connectivitytester.models.{SendingStatus, _}

@Singleton
class SoapMessageStatusRepository @Inject() (mongoComponent: MongoComponent, appConfig: AppConfig)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[SoapMessageStatus](
      collectionName = "messages",
      mongoComponent = mongoComponent,
      domainFormat = SoapMessageStatus.formatter,
      indexes = Seq(
        IndexModel(ascending("globalId"), IndexOptions().name("globalIdIndex").background(true).unique(true)),
        IndexModel(ascending("messageId"), IndexOptions().name("messageIdIndex").background(true).unique(false)),
        IndexModel(
          ascending("createDateTime"),
          IndexOptions().name("ttlIndex").background(true)
            .expireAfter(60 * 60 * 24 * 30, TimeUnit.SECONDS)
        )
      )
    ) with Logging {
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  def persist(entity: SoapMessageStatus): Future[InsertOneResult] = {
    collection.insertOne(entity).toFuture()
  }

  def findById(searchForId: String): Future[Option[SoapMessageStatus]] = {
    val findQuery = or(Document("messageId" -> searchForId), Document("globalId" -> searchForId))
    collection.find(findQuery).headOption()
  }

  def updateSendingStatus(messageId: String, newStatus: SendingStatus): Future[Option[SoapMessageStatus]] = {
    collection.withReadPreference(primaryPreferred())
      .findOneAndUpdate(
        filter = equal("messageId", Codecs.toBson(messageId)),
        update = set("status", Codecs.toBson(newStatus.toString())),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      ).toFutureOption()
  }

  def retrieveMessagesMissingConfirmation: Source[SoapMessageStatus, NotUsed] = {
    MongoSource(collection.withReadPreference(primaryPreferred())
      .find(filter = and(equal("status", SENT.toString()), and(lte("createDateTime", now().minus(appConfig.confirmationWaitDuration))))))
  }

  def retrieveMessagesInErrorState: Source[SoapMessageStatus, NotUsed] = {
    val errorStates = List(FAILED.toString(), COE.toString())
    MongoSource(collection.withReadPreference(primaryPreferred())
      .find(filter = and(in("status", errorStates: _*), and(lte("createDateTime", now().minus(appConfig.confirmationWaitDuration))))))
  }
}
