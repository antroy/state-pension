/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.statepension.connectors

import play.api.data.validation.ValidationError
import play.api.libs.json.JsPath
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpReads, HttpResponse}
import uk.gov.hmrc.statepension.WSHttp
import uk.gov.hmrc.statepension.domain.{StatePension, StatePensionExclusion}
import uk.gov.hmrc.statepension.util.EitherReads.eitherReads

import scala.concurrent.Future

trait NispConnector {
  def http: HttpGet

  def nispBaseUrl: String

  class JsonValidationException(message: String) extends Exception(message)

  private def formatJsonErrors(errors: Seq[(JsPath, Seq[ValidationError])]): String = {
    "JSON Validation Error: " + errors.map(p => p._1 + " - " + p._2.map(_.message).mkString(",")).mkString(" | ")
  }

  def getStatePension(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusion, StatePension]] = {
    val response = http.GET[HttpResponse](s"$nispBaseUrl/paye/$nino")(rds = HttpReads.readRaw, hc)
    response.flatMap { httpResponse =>
      httpResponse.json.validate[Either[StatePensionExclusion, StatePension]].fold(
        invalid => Future.failed(new JsonValidationException(formatJsonErrors(invalid))),
        valid => Future.successful(valid)
      )
    }
  }
}

object NispConnector extends NispConnector with ServicesConfig {
  override val nispBaseUrl: String = baseUrl("nisp")
  override def http: HttpGet = WSHttp
}
