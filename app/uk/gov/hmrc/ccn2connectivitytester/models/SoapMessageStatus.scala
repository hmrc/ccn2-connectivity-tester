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

package uk.gov.hmrc.ccn2connectivitytester.models

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

final case class SoapMessageStatus(
    globalId: UUID,
    messageId: String,
    status: SendingStatus,
    ccnHttpStatus: Int,
    createDateTime: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
  )

object SoapMessageStatus {

  val reads: Reads[SoapMessageStatus] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "globalId").read[UUID] and
        (__ \ "messageId").read[String] and
        (__ \ "status").read[SendingStatus] and
        (__ \ "ccnHttpStatus").read[Int] and
        (__ \ "createDateTime").read(MongoJavatimeFormats.instantFormat).orElse(Reads.pure(Instant.now()))
    )(SoapMessageStatus.apply _)
  }

  val writes: OWrites[SoapMessageStatus]             = {
    import play.api.libs.functional.syntax._

    (
      (__ \ "globalId").write[UUID] and
        (__ \ "messageId").write[String] and
        (__ \ "status").write[SendingStatus] and
        (__ \ "ccnHttpStatus").write[Int] and
        (__ \ "createDateTime").write(MongoJavatimeFormats.instantFormat)
    )(unlift(SoapMessageStatus.unapply))
  }
  implicit val formatter: OFormat[SoapMessageStatus] = OFormat(reads, writes)
}

sealed trait StatusType

object StatusType {}

sealed trait SendingStatus extends StatusType

object SendingStatus {
  case object SENT     extends SendingStatus
  case object FAILED   extends SendingStatus
  case object RETRYING extends SendingStatus
  case object ALERTED  extends SendingStatus
  case object COE      extends SendingStatus
  case object COD      extends SendingStatus

  val values: Set[SendingStatus] = Set(SENT, FAILED, RETRYING, ALERTED, COE, COD)

  def apply(text: String): Option[SendingStatus] = SendingStatus.values.find(_.toString() == text.toUpperCase)

  implicit val format: Format[SendingStatus] = SealedTraitJsonFormatting.createFormatFor[SendingStatus]("Sending Status", apply)
}
