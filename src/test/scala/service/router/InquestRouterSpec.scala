package service.router

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.testkit.ScalatestRouteTest

import clients.firebase.FirebaseClient
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import db.models.{Inquest, Role, User}
import db.spec.InquestRepository.InquestNotFound
import db.spec.{InquestRepository, UserRepository}
import mocks.AuthMocks
import service.models.{ApiError, InquestCreateRequest, InquestUpdateRequest}

import scala.concurrent.Future

class InquestRouterSpec
  extends WordSpec with Matchers with ScalatestRouteTest with MockFactory with AuthMocks {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val testInquests = Seq(
    Inquest(Some(1), "Queen vs CBC", "some inquest"),
    Inquest(Some(2), "Superman vs Batman", "some inquest")
  )

  private val testInquestCreateRequest = InquestCreateRequest("Mega Shark vs Crocasaurus", "some inquest")
  private val testInquestCreateRequestInvalidTitle = testInquestCreateRequest.copy(title = "")
  private val testInquestCreateResponse = testInquestCreateRequest.toInquest.copy(id = Some(1))

  private val testInquestUpdateRequest = InquestUpdateRequest(Some("Queen vs CBC"), Some("some inquest"))
  private val testInquestUpdateRequestInvalidTitle = InquestUpdateRequest(Some(""), Some("some inquest"))
  private val testInquestUpdateResponse = Inquest(Some(1), "Queen vs CBC", "some inquest")

  private val testFirebaseUid = "uid"
  private val testEmail = "email"
  private val testUser = User(None, testFirebaseUid, testEmail, "", "", Role.User, None)
  private val testEditor = testUser.copy(role = Role.Editor)

  private val testToken = "token"
  private val testCredentials = OAuth2BearerToken("token")

  "InquestRouter" should {

    "provide GET /api/inquests route" which {

      "returns all inquests" in {
        val mockInquestRepository = mock[InquestRepository]
        val router = new InquestRouter(mockInquestRepository, mock[UserRepository], mock[FirebaseClient])

        (mockInquestRepository.all _)
          .expects()
          .returns(Future.successful(testInquests))

        Get("/api/inquests") ~> router.sealedRoute ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[Seq[Inquest]]
          response shouldBe testInquests
        }
      }

      "handles repository failure" in {
        val mockInquestRepository = mock[InquestRepository]
        val router = new InquestRouter(mockInquestRepository, mock[UserRepository], mock[FirebaseClient])

        (mockInquestRepository.all _)
          .expects()
          .returns(Future.failed(new Exception("BOOM!")))

        Get("/api/inquests") ~> router.sealedRoute ~> check {
          status shouldBe ApiError.generic.statusCode
          val response = responseAs[String]
          response shouldBe ApiError.generic.message
        }
      }

    }

    "provide POST /api/inquests route" which {

      "creates inquest with valid data" in {
        val mockInquestRepository = mock[InquestRepository]
        val (mockFirebaseClient, mockUserRepository) = mockAuthentication(testToken, Some(testEditor))
        val router = new InquestRouter(mockInquestRepository, mockUserRepository, mockFirebaseClient)

        (mockInquestRepository.create _)
          .expects(testInquestCreateRequest)
          .returns(Future.successful(testInquestCreateResponse))

        (Post("/api/inquests", testInquestCreateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[Inquest]
          response shouldBe testInquestCreateResponse
        })
      }

      "does not create inquest with invalid data" in {
        val mockInquestRepository = mock[InquestRepository]
        val (mockFirebaseClient, mockUserRepository) = mockAuthentication(testToken, Some(testEditor))
        val router = new InquestRouter(mockInquestRepository, mockUserRepository, mockFirebaseClient)

        (Post("/api/inquests", testInquestCreateRequestInvalidTitle)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          val apiError = ApiError.emptyInquestTitle
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        })
      }

      "handles repository failure" in {
        val mockInquestRepository = mock[InquestRepository]
        val (mockFirebaseClient, mockUserRepository) = mockAuthentication(testToken, Some(testEditor))
        val router = new InquestRouter(mockInquestRepository, mockUserRepository, mockFirebaseClient)

        (mockInquestRepository.create _)
          .expects(testInquestCreateRequest)
          .returns(Future.failed(new Exception("BOOM!")))

        (Post("/api/inquests", testInquestCreateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe ApiError.generic.statusCode
          val response = responseAs[String]
          response shouldBe ApiError.generic.message
        })
      }

      "handles failure to authorize" in {
        val mockInquestRepository = mock[InquestRepository]
        val (mockFirebaseClient, mockUserRepository) = mockAuthentication(testToken, Some(testUser))
        val router = new InquestRouter(mockInquestRepository, mockUserRepository, mockFirebaseClient)

        (Post("/api/inquests", testInquestCreateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.Forbidden
        })
      }

      "handles failure to authenticate" in {
        val mockInquestRepository = mock[InquestRepository]
        val (mockFirebaseClient, mockUserRepository) = mockAuthentication(testToken, None)
        val router = new InquestRouter(mockInquestRepository, mockUserRepository, mockFirebaseClient)

        (Post("/api/inquests", testInquestCreateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.Unauthorized
        })
      }

    }

    "provide PUT /api/inquests route" which {

      "updates an inquest with valid data" in {
        val mockInquestRepository = mock[InquestRepository]
        val (mockFirebaseClient, mockUserRepository) = mockAuthentication(testToken, Some(testEditor))
        val router = new InquestRouter(mockInquestRepository, mockUserRepository, mockFirebaseClient)

        (mockInquestRepository.update _)
          .expects(1, testInquestUpdateRequest)
          .returns(Future.successful(testInquestUpdateResponse))

        (Put(s"/api/inquests/1", testInquestUpdateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[Inquest]
          response shouldBe testInquestUpdateResponse
        })
      }

      "returns not found if inquest does not exist" in {
        val mockInquestRepository = mock[InquestRepository]
        val (mockFirebaseClient, mockUserRepository) = mockAuthentication(testToken, Some(testEditor))
        val router = new InquestRouter(mockInquestRepository, mockUserRepository, mockFirebaseClient)

        (mockInquestRepository.update _)
          .expects(2, testInquestUpdateRequest)
          .returns(Future.failed(InquestNotFound(2)))

        (Put("/api/inquests/2", testInquestUpdateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          val apiError = ApiError.inquestNotFound
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        })
      }

      "does not update an inquest with invalid data" in {
        val mockInquestRepository = mock[InquestRepository]
        val (mockFirebaseClient, mockUserRepository) = mockAuthentication(testToken, Some(testEditor))
        val router = new InquestRouter(mockInquestRepository, mockUserRepository, mockFirebaseClient)

        (Put(s"/api/inquests/1", testInquestUpdateRequestInvalidTitle)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          val apiError = ApiError.emptyInquestTitle
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        })
      }

      "handles repository failure" in {
        val mockInquestRepository = mock[InquestRepository]
        val (mockFirebaseClient, mockUserRepository) = mockAuthentication(testToken, Some(testEditor))
        val router = new InquestRouter(mockInquestRepository, mockUserRepository, mockFirebaseClient)

        (mockInquestRepository.update _)
          .expects(1, testInquestUpdateRequest)
          .returns(Future.failed(new Exception("BOOM!")))

        (Put(s"/api/inquests/1", testInquestUpdateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe ApiError.generic.statusCode
          val response = responseAs[String]
          response shouldBe ApiError.generic.message
        })
      }

      "handles failure to authorize" in {
        val mockInquestRepository = mock[InquestRepository]
        val (mockFirebaseClient, mockUserRepository) = mockAuthentication(testToken, Some(testUser))
        val router = new InquestRouter(mockInquestRepository, mockUserRepository, mockFirebaseClient)

        (Put(s"/api/inquests/1", testInquestUpdateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.Forbidden
        })
      }

      "handles failure to authenticate" in {
        val mockInquestRepository = mock[InquestRepository]
        val (mockFirebaseClient, mockUserRepository) = mockAuthentication(testToken, None)
        val router = new InquestRouter(mockInquestRepository, mockUserRepository, mockFirebaseClient)

        (Put(s"/api/inquests/1", testInquestUpdateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.Unauthorized
        })
      }

    }

  }

}
