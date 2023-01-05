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

package uk.gov.hmrc.ccn2connectivitytester.scheduled

import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

trait LockedScheduledJob {

  lazy val lockKeeper: LockService = LockService(lockRepository, lockId = s"$name-scheduled-job-lock", ttl = 1.hour)
  val releaseLockAfter: FiniteDuration
  val lockRepository: MongoLockRepository

  def name: String

  def initialDelay: FiniteDuration

  def interval: FiniteDuration

  def executeInLock(implicit ec: ExecutionContext): Future[this.Result]

  final def execute(implicit ec: ExecutionContext): Future[Result] =
    lockKeeper.withLock {
      executeInLock
    } map {
      case Some(Result(msg)) => Result(s"Job named $name ran and completed with result $msg")
      case None => Result(s"Job named $name cannot acquire mongo lock so did not run")
    }

  override def toString() = s"Scheduled job named $name, initially pausing for $initialDelay then running every $interval"

  case class Result(message: String)

}
