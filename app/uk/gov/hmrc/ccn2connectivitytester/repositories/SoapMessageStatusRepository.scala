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

import java.time.Instant
import java.time.Instant.now

import akka.NotUsed
import akka.stream.alpakka.mongodb.scaladsl.MongoSource
import akka.stream.scaladsl.Source
import org.mongodb.scala.model.Filters._
import play.api.libs.json.Format
import uk.gov.hmrc.ccn2connectivitytester.models.SendingStatus
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import java.util.concurrent.TimeUnit

import javax.inject.{Inject, Singleton}
import org.mongodb.scala.ReadPreference.primaryPreferred
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters.{equal, or}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model._
import org.mongodb.scala.result.InsertOneResult
import play.api.Logging
import uk.gov.hmrc.ccn2connectivitytester.config.AppConfig
import uk.gov.hmrc.ccn2connectivitytester.models.SoapMessageStatus
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SoapMessageStatusRepository @Inject()(mongoComponent: MongoComponent, appConfig: AppConfig)
                                           (implicit ec: ExecutionContext)
  extends PlayMongoRepository[SoapMessageStatus](
    collectionName = "messages",
    mongoComponent = mongoComponent,
    domainFormat = SoapMessageStatus.formatter,
    indexes = Seq(IndexModel(ascending("globalId"),
      IndexOptions().name("globalIdIndex").background(true).unique(true)),
      IndexModel(ascending("messageId"),
        IndexOptions().name("messageIdIndex").background(true).unique(false)),
      IndexModel(ascending("createDateTime"),
        IndexOptions().name("ttlIndex").background(true)
          .expireAfter(60 * 60 * 24 * 30, TimeUnit.SECONDS))),
    /*extraCodecs = Seq(
      new UuidCodec(UuidRepresentation.STANDARD),
      Codecs.playFormatCodec(SoapMessageStatus.formatter)/*,
      Codecs.playFormatCodec(MongoFormatter.instantFormat)*/
    )*/)
    with Logging {
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  /*override lazy val collection: MongoCollection[SoapMessageStatus] =
    CollectionFactory
      .collection(mongoComponent.database, collectionName, domainFormat)
      .withCodecRegistry(
        fromRegistries(
          fromCodecs(
            Codecs.playFormatCodec(domainFormat),
            Codecs.playFormatCodec(MongoFormatter.messageStatusFormatter),
            Codecs.playFormatCodec(MongoFormatter.instantFormat)
//            Codecs.playFormatCodec(MongoFormatter.dateTimeFormat)
          ),
          MongoClient.DEFAULT_CODEC_REGISTRY
        )
      )*/

  def persist(entity: SoapMessageStatus): Future[InsertOneResult] = {
    collection.insertOne(entity).toFuture()
  }

  def findById(searchForId: String): Future[Option[SoapMessageStatus]] = {
    val findQuery = or(Document("messageId" -> searchForId), Document("globalId" -> searchForId))
    collection.find(findQuery).headOption()
      .recover {
        case e: Exception =>
          logger.warn(s"error finding message - ${e.getMessage}")
          None
      }
  }

  def updateSendingStatus(messageId: String, newStatus: SendingStatus): Future[Option[SoapMessageStatus]] = {
    collection.withReadPreference(primaryPreferred())
      .findOneAndUpdate(filter = equal("messageId", Codecs.toBson(messageId)),
        update = set("status", Codecs.toBson(newStatus.entryName)),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      ).toFutureOption()
  }

  def retrieveMessagesMissingConfirmation: Source[SoapMessageStatus, NotUsed] = {
    MongoSource(collection.withReadPreference(primaryPreferred())
      .find(filter = and(equal("status", SendingStatus.SENT.entryName),
        and(lte("createDateTime", now().minus(appConfig.confirmationWaitDuration)))))
      .map(_.asInstanceOf[SoapMessageStatus]))
  }

  /*def updateNextRetryTime(globalId: UUID, newRetryDateTime: DateTime): Future[Option[RetryingOutboundSoapMessage]] = {
    collection.withReadPreference(primaryPreferred())
      .findOneAndUpdate(filter = equal("globalId", Codecs.toBson(globalId)),
        update = set("retryDateTime", newRetryDateTime),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      ).map(_.asInstanceOf[RetryingOutboundSoapMessage]).headOption()
  }


  def updateConfirmationStatus(messageId: String, newStatus: DeliveryStatus, confirmationMsg: String): Future[Option[OutboundSoapMessage]] = {
    val field: String = newStatus match {
      case DeliveryStatus.COD => "codMessage"
      case DeliveryStatus.COE => "coeMessage"
    }

    for {
      _ <- collection.bulkWrite(
        List(UpdateManyModel(Document("messageId" -> messageId), combine(set("status", Codecs.toBson(newStatus.entryName)), set(field, confirmationMsg)))),
        BulkWriteOptions().ordered(false)).toFuture()
      findUpdated <- findById(messageId)
    } yield findUpdated
  }

  */
}
