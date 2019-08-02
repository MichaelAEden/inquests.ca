package service.router

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

import clients.firebase.{FirebaseClient, FirebaseUser}
import db.models.Inquest
import db.spec.InquestRepository
import db.spec.InquestRepository.InquestNotFound
import service.models.{ApiError, InquestCreateRequest, InquestUpdateRequest}

import scala.concurrent.Future

class InquestRouterSpec extends WordSpec with Matchers with ScalatestRouteTest with MockFactory {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val testUser = FirebaseUser("uid")
  private val testToken = "token"
  private val testCredentials = OAuth2BearerToken(testToken)

  private val testInquests = Seq(
    Inquest(Some(1), "Queen vs CBC", "some inquest"),
    Inquest(Some(2), "Superman vs Batman", "some inquest")
  )

  private val testInquestCreateRequest = InquestCreateRequest("Mega Shark vs Crocasaurus", "some inquest")
  private val testInquestCreateRequestInvalidTitle = testInquestCreateRequest.copy(title = "")
  private val testInquestCreateResponse = testInquestCreateRequest.toInquest.copy(id = Some(1))

  private val testUpdateInquestRequest = InquestUpdateRequest(Some("Queen vs CBC"), Some("some inquest"))
  private val testUpdateInquestRequestInvalidTitle = testUpdateInquestRequest.copy(title = Some(""))
  private val testInquestUpdateResponse = Inquest(Some(1), "Queen vs CBC", "some inquest")

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

    "provide GET /api/inquests route" which {

      "returns all inquests" in {
        val mockInquestRepository = mock[InquestRepository]
        val router = new InquestRouter(mockInquestRepository, mock[FirebaseClient])

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
        val router = new InquestRouter(mockInquestRepository, mock[FirebaseClient])

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

    "provides POST /api/inquests route" which {

      "creates inquest with valid data" in {
        val mockInquestRepository = mock[InquestRepository]
        val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
        val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

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
        val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
        val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

        (mockInquestRepository.create _)
          .expects(*)
          .never

        (Post("/api/inquests", testInquestCreateRequestInvalidTitle)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          val apiError = ApiError.invalidInquestTitle(testInquestCreateRequestInvalidTitle.title)
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        })
      }

      "handles repository failure" in {
        val mockInquestRepository = mock[InquestRepository]
        val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
        val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

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
        val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = false)
        val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

        (Post("/api/inquests", testInquestCreateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.Forbidden
        })
      }

      "handles failure to authenticate" in {
        val mockInquestRepository = mock[InquestRepository]
        val mockFirebaseClient = createMockFirebaseClient(testToken, maybeUser = None, isAdmin = false)
        val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

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
        val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
        val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

        (mockInquestRepository.update _)
          .expects(1, testUpdateInquestRequest)
          .returns(Future.successful(testInquestUpdateResponse))

        (Put(s"/api/inquests/1", testUpdateInquestRequest)
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
        val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
        val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

        (mockInquestRepository.update _)
          .expects(1, testUpdateInquestRequest)
          .returns(Future.failed(InquestNotFound(1)))

        (Put("/api/inquests/1", testUpdateInquestRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          val apiError = ApiError.inquestNotFound(1)
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        })
      }

      "does not update an inquest with invalid data" in {
        val mockInquestRepository = mock[InquestRepository]
        val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
        val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

        (mockInquestRepository.update _)
          .expects(*, *)
          .never

        (Put(s"/api/inquests/1", testUpdateInquestRequestInvalidTitle)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          val apiError = ApiError.invalidInquestTitle(testUpdateInquestRequestInvalidTitle.title.get)
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        })
      }

      "handles repository failure" in {
        val mockInquestRepository = mock[InquestRepository]
        val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
        val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

        (mockInquestRepository.update _)
          .expects(1, testUpdateInquestRequest)
          .returns(Future.failed(new Exception("BOOM!")))

        (Put(s"/api/inquests/1", testUpdateInquestRequest)
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
        val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = false)
        val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

        (Put(s"/api/inquests/1", testUpdateInquestRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.Forbidden
        })
      }

      "handles failure to authenticate" in {
        val mockInquestRepository = mock[InquestRepository]
        val mockFirebaseClient = createMockFirebaseClient(testToken, maybeUser = None, isAdmin = false)
        val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

        (Put(s"/api/inquests/1", testUpdateInquestRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.Unauthorized
        })
      }

    }

  }

  }
