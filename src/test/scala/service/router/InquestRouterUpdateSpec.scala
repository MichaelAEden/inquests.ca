package service.router

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

import clients.firebase.{FirebaseClient, FirebaseUser}
import db.models.{Inquest, UpdateInquest}
import db.spec.InquestRepository
import db.spec.InquestRepository.InquestNotFound
import service.models.ApiError

import scala.concurrent.Future

class InquestRouterUpdateSpec extends WordSpec with Matchers with ScalatestRouteTest with MockFactory {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val testInquestId = 1
  private val testInquest = Inquest(Some(1), "Queen vs CBC", "some inquest")
  private val testUpdateInquest = UpdateInquest(Some("Queen vs CBC"), Some("some inquest"))
  private val testUpdateInquestInvalidTitle = UpdateInquest(Some(""), Some("some inquest"))

  private val testUser = FirebaseUser("uid")
  private val testToken = "token"
  private val testCredentials = OAuth2BearerToken(testToken)

  private def createMockFirebaseClient(
    token: String,
    maybeUser: Option[FirebaseUser],
    isAdmin: Boolean
  ): FirebaseClient = {
    val mockFirebaseClient = mock[FirebaseClient]

    (mockFirebaseClient.getUserFromToken _)
      .expects(token)
      .returns(Future.successful(maybeUser))

    maybeUser.map { user =>
      (mockFirebaseClient.isAdmin _)
        .expects(user)
        .returns(Future.successful(isAdmin))
    }

    mockFirebaseClient
  }

  "InquestRouter" should {

    "update an inquest with valid data" in {
      val mockInquestRepository = mock[InquestRepository]
      val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
      val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

      (mockInquestRepository.update _)
        .expects(testInquestId, testUpdateInquest)
        .returns(Future.successful(testInquest))

      Put(s"/api/inquests/$testInquestId", testUpdateInquest) ~> addCredentials(testCredentials) ~> router.sealedRoute ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Inquest]
        response shouldBe testInquest
      }
    }

    "return not found if inquest does not exist" in {
      val mockInquestRepository = mock[InquestRepository]
      val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
      val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

      (mockInquestRepository.update _)
        .expects(2, testUpdateInquest)
        .returns(Future.failed(InquestNotFound(2)))

      Put("/api/inquests/2", testUpdateInquest) ~> addCredentials(testCredentials) ~> router.sealedRoute ~> check {
        val apiError = ApiError.inquestNotFound(2)
        status shouldBe apiError.statusCode
        val response = responseAs[String]
        response shouldBe apiError.message
      }
    }

    "not update an inquest with invalid data" in {
      val mockInquestRepository = mock[InquestRepository]
      val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
      val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

      (mockInquestRepository.update _)
        .expects(*, *)
        .never

      Put(s"/api/inquests/$testInquestId", testUpdateInquestInvalidTitle) ~> addCredentials(testCredentials) ~> router.sealedRoute ~> check {
        val apiError = ApiError.invalidInquestTitle(testUpdateInquestInvalidTitle.title.get)
        status shouldBe apiError.statusCode
        val response = responseAs[String]
        response shouldBe apiError.message
      }
    }

    "handle repository failure" in {
      val mockInquestRepository = mock[InquestRepository]
      val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
      val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

      (mockInquestRepository.update _)
        .expects(testInquestId, testUpdateInquest)
        .returns(Future.failed(new Exception("BOOM!")))

      Put(s"/api/inquests/$testInquestId", testUpdateInquest) ~> addCredentials(testCredentials) ~> router.sealedRoute ~> check {
        status shouldBe ApiError.generic.statusCode
        val response = responseAs[String]
        response shouldBe ApiError.generic.message
      }
    }

    "handle failure to authorize in inquests route" in {
      val mockInquestRepository = mock[InquestRepository]
      val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = false)
      val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

      Put(s"/api/inquests/$testInquestId", testUpdateInquest) ~> addCredentials(testCredentials) ~> router.sealedRoute ~> check {
        status shouldBe StatusCodes.Forbidden
      }
    }

    "handle failure to authenticate in inquests route" in {
      val mockInquestRepository = mock[InquestRepository]
      val mockFirebaseClient = createMockFirebaseClient(testToken, maybeUser = None, isAdmin = false)
      val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

      Put(s"/api/inquests/$testInquestId", testUpdateInquest) ~> addCredentials(testCredentials) ~> router.sealedRoute ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

  }

}
