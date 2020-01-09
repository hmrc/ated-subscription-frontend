/*
 * Copyright 2020 HM Revenue & Customs
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

package utils

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import utils.ErrorMessageUtils._

import scala.xml._

class ErrorMessageUtilsSpec extends PlaySpec {

  val FileSystemReadXxePayload = """<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?> \n <!DOCTYPE foo [  \n <!ELEMENT foo ANY >  <!ENTITY xxe SYSTEM \"file:///does/not/exist\" >]>\n<foo>&xxe;</foo>"""

  "ErrorMessageUtils" must {
    "return 9001" when {
      "message has error number as 9001" in {
        val badGatewayResponse = Json.parse( """{"statusCode":502,"message":"<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/03/addressing\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"><soap:Header><wsa:Action>http://schemas.xmlsoap.org/ws/2004/03/addressing/fault</wsa:Action><wsa:MessageID>uuid:199814d0-9758-49d1-a2c0-d24300f67e2c</wsa:MessageID><wsa:RelatesTo>uuid:d1894fa0-b97d-4707-a814-e0c5ea79a01a</wsa:RelatesTo><wsa:To>http://schemas.xmlsoap.org/ws/2004/03/addressing/role/anonymous</wsa:To><wsse:Security><wsu:Timestamp wsu:Id=\"Timestamp-0fdb513d-1da4-4804-80b5-d04530653fac\"><wsu:Created>2017-03-22T14:23:00Z</wsu:Created><wsu:Expires>2017-03-22T14:28:00Z</wsu:Expires></wsu:Timestamp></wsse:Security></soap:Header><soap:Body><soap:Fault><faultcode>soap:Client</faultcode><faultstring>Business Rule Error</faultstring><faultactor>http://www.gateway.gov.uk/soap/2007/02/portal</faultactor><detail><GatewayDetails xmlns=\"urn:GSO-System-Services:external:SoapException\"><ErrorNumber>9001</ErrorNumber><Message>The service HMRC-AGENT-AGENT requires unique identifiers</Message><RequestID>0753B23CA0C14D23A4BBFC129795C42E</RequestID></GatewayDetails></detail></soap:Fault></soap:Body></soap:Envelope>"}	""")

        parseErrorResp(HttpResponse(502, Some(badGatewayResponse))) must be("9001")
      }
    }

    "return 10006" when {
      "message has error number as 10006" in {
        val badGatewayResponse = Json.parse( """{"statusCode":502,"message":"<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/03/addressing\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"><soap:Header><wsa:Action>http://schemas.xmlsoap.org/ws/2004/03/addressing/fault</wsa:Action><wsa:MessageID>uuid:199814d0-9758-49d1-a2c0-d24300f67e2c</wsa:MessageID><wsa:RelatesTo>uuid:d1894fa0-b97d-4707-a814-e0c5ea79a01a</wsa:RelatesTo><wsa:To>http://schemas.xmlsoap.org/ws/2004/03/addressing/role/anonymous</wsa:To><wsse:Security><wsu:Timestamp wsu:Id=\"Timestamp-0fdb513d-1da4-4804-80b5-d04530653fac\"><wsu:Created>2017-03-22T14:23:00Z</wsu:Created><wsu:Expires>2017-03-22T14:28:00Z</wsu:Expires></wsu:Timestamp></wsse:Security></soap:Header><soap:Body><soap:Fault><faultcode>soap:Client</faultcode><faultstring>Business Rule Error</faultstring><faultactor>http://www.gateway.gov.uk/soap/2007/02/portal</faultactor><detail><GatewayDetails xmlns=\"urn:GSO-System-Services:external:SoapException\"><ErrorNumber>10006</ErrorNumber><Message>The service HMRC-AGENT-AGENT requires unique identifiers</Message><RequestID>0753B23CA0C14D23A4BBFC129795C42E</RequestID></GatewayDetails></detail></soap:Fault></soap:Body></soap:Envelope>"}	""")

        parseErrorResp(HttpResponse(502, Some(badGatewayResponse))) must be("10006")
      }
    }

    "Show that scala.xml.XML can protect against file access when securely configured" in {
    intercept[SAXParseException] {
        val payload = Json.parse( s"""{"statusCode":502,"message":"${FileSystemReadXxePayload}"}""")
        parseErrorResp(HttpResponse(502, Some(payload)))
      }
    }

  }

}
