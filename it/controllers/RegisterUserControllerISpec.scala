package controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import helpers.IntegrationSpec
import models.{Address, BusinessCustomerDetails, RequestEMACPayload, Verifier, Verifiers}
import play.api.http.Status._
import play.api.http.{HeaderNames => HN}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.HeaderNames

class RegisterUserControllerISpec extends IntegrationSpec {

  val ATED_SERVICE_NAME = "HMRC-ATED-ORG"
  val enrolmentKey = s"$ATED_SERVICE_NAME~AtedRefNumber~XN1200000100001"

  val reviewDetails = BusinessCustomerDetails(businessName = "ACME",
    businessType = "Corporate Body",
    businessAddress = Address(line_1 = "line1", line_2 = "line2", line_3 = None, line_4 = None, postcode = Some("NE98 1ZZ"), country = "GB"),
    sapNumber = "1234567890", safeId = "XW0001234567890", agentReferenceNumber = None, utr = Some("12345678"))

  val reviewDetailsJson = Json.toJson(reviewDetails)

  val emacPayloadRequest = RequestEMACPayload(
    userId = "user-id",
    friendlyName = "friendlyName",
    `type` = "type",
    verifiers = Verifiers(List(Verifier(key = "Postcode", value = "NE98 1ZZ"), Verifier(key = "CTUTR", value = "12345678"))))

  val emacPayload = Json.toJson(emacPayloadRequest)

  val contactDetails: String =
    s"""
                                       {
       |    "id" : "${sessionId}",
       |    "atomicId" : "null",
       |    "data" : {
       |        "BC_BusinessReg_Details" : {
       |            "isCorrespondenceAddress" : true
       |        },
       |        "Correspondence_Address" : {
       |            "line_1" : "23 High Street",
       |            "line_2" : "Park View",
       |            "line_3" : "Gloucester",
       |            "line_4" : "Gloucestershire",
       |            "postcode" : "NE98 1ZZ",
       |            "country" : "GB"
       |        },
       |        "Contact_Details" : {
       |            "firstName" : "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
       |            "lastName" : "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
       |            "telephone" : "01914885251"
       |        },
       |        "Contact_Email_Details" : {
       |            "emailConsent" : true,
       |            "email" : "test@mail.com"
       |        }
       |    },
       |    "modifiedDetails" : {
       |        "createdAt" : "true",
       |        "lastUpdated" : "true"
       |    }
       |
       |}""".stripMargin

  val authLoginData = Json.parse(
    """{
      |   "affinityGroup":"Organisation",
      |   "credentialRole":"User",
      |   "groupIdentifier":"9FFB9E14-681D-446C-9731-F6B2AECA5087",
      |   "credentials": {"providerId": "12345-credId", "providerType": "GovernmentGateway"},
      |   "allEnrolments": [{
      |    "key": "IR-CT",
      |    "identifiers": [{ "key": "UTR", "value": "456" }],
      |    "state": "Activated"
      |   }],
      |   "internalId": "123456",
      |   "optionalCredentials":{
      |     "providerId":"12345-credId",
      |     "providerType":"GovernmentGateway"
      |     }
      |     }
      |""".stripMargin).toString


  "/register-user" should {
    "retrieve error" when {
      "Bad Request exception is sent from tax enrolments" in {

        stubFor(post(urlMatching("/auth/authorise"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                authLoginData
              )
          )
        )

        stubGet("/business-customer/fetch-review-details/ATED", 200, reviewDetailsJson.toString())
        stubGet("/mandate/agent/old-nonuk-mandate-from-session", 200, "")
        stubGet(s"/keystore/ated-subscription-frontend/${sessionId}", 200, contactDetails)
        stubPost(s"/org/12345-credId/subscribe", 200, "")

        stubFor(post(urlMatching("/tax-enrolments/groups/9FFB9E14-681D-446C-9731-F6B2AECA5087/enrolments/HMRC-ATED-ORG~ATEDRefNumber~XY1200000100002"))
          .willReturn(
            aResponse()
              .withStatus(CONFLICT)
              .withBody(
                emacPayload.toString()
              )
          ))

        val result: WSResponse = await(hitApplicationEndpoint("/register-user")
          .withHttpHeaders(HN.SET_COOKIE -> getSessionCookie())
          .addHttpHeaders(HeaderNames.xSessionId -> s"${sessionId}")
          .addHttpHeaders(HN.AUTHORIZATION -> "token")
          .get())

        result.body contains "Somebody has already registered from your organisation"

      }
    }
  }
}
