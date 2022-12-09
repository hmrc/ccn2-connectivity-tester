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

package uk.gov.hmrc.ccn2connectivitytester.models

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import scala.collection.immutable

final case class SoapMessageStatus(globalId: UUID, messageId: String, status: SendingStatus, ccnHttpStatus: Int,
                                   createDateTime: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS))

object SoapMessageStatus {
  val reads: Reads[SoapMessageStatus] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "globalId").read[UUID] and
        (__ \ "messageId").read[String] and
        (__ \ "status").read[SendingStatus] and
        (__ \ "ccnHttpStatus").read[Int] and
        (__ \ "createDateTime").read(MongoJavatimeFormats.instantFormat).orElse(Reads.pure(Instant.now()))
      ) (SoapMessageStatus.apply _)
  }
  val writes: OWrites[SoapMessageStatus] = {
    import play.api.libs.functional.syntax._
    
    (
      (__ \ "globalId").write[UUID] and
        (__ \ "messageId").write[String] and
        (__ \ "status").write[SendingStatus] and
        (__ \ "ccnHttpStatus").write[Int] and
        (__ \ "createDateTime").write(MongoJavatimeFormats.instantFormat)
      ) (unlift(SoapMessageStatus.unapply))
  }
  implicit val formatter: OFormat[SoapMessageStatus] = OFormat(reads, writes)
}

sealed abstract class StatusType extends EnumEntry

object StatusType extends Enum[StatusType] with PlayJsonEnum[StatusType] {
  val values: immutable.IndexedSeq[StatusType] = findValues
}

sealed abstract class SendingStatus(override val entryName: String) extends StatusType

object SendingStatus extends Enum[SendingStatus] with PlayJsonEnum[SendingStatus] {
  val values: immutable.IndexedSeq[SendingStatus] = findValues

  case object SENT extends SendingStatus("SENT")

  case object FAILED extends SendingStatus("FAILED")

  case object RETRYING extends SendingStatus("RETRYING")

  case object ALERTED extends SendingStatus("ALERTED")

  case object COE extends SendingStatus("COE")

  case object COD extends SendingStatus("COD")

}
