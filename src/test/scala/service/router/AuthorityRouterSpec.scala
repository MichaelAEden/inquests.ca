package service.router

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

import clients.firebase.{FirebaseClient, FirebaseUser}
import db.models.Authority
import db.spec.AuthorityRepository
import service.models.{ApiError, AuthorityCreateRequest}

import scala.concurrent.Future

class AuthorityRouterSpec extends WordSpec with Matchers with ScalatestRouteTest with MockFactory {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val testUser = FirebaseUser("uid")
  private val testToken = "token"
  private val testCredentials = OAuth2BearerToken(testToken)

  private val testAuthorities = Seq(
    Authority(Some(1), "Queen vs CBC", "some authority"),
    Authority(Some(2), "Superman vs Batman", "some authority")
  )

  private val testAuthorityCreateRequest = AuthorityCreateRequest("Mega Shark vs Crocasaurus", "some authority")
  private val testAuthorityCreateRequestInvalidTitle = testAuthorityCreateRequest.copy(title = "")
  private val testAuthorityCreateResponse = testAuthorityCreateRequest.toAuthority.copy(id = Some(1))

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

  "AuthorityRouter" should {

    "provide GET /api/authorities route" which {

      "returns all authorities" in {
        val mockAuthorityRepository = mock[AuthorityRepository]
        val router = new AuthorityRouter(mockAuthorityRepository, mock[FirebaseClient])

        (mockAuthorityRepository.all _)
          .expects()
          .returns(Future.successful(testAuthorities))

        Get("/api/authorities") ~> router.sealedRoute ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[Seq[Authority]]
          response shouldBe testAuthorities
        }
      }

      "handles repository failure" in {
        val mockAuthorityRepository = mock[AuthorityRepository]
        val router = new AuthorityRouter(mockAuthorityRepository, mock[FirebaseClient])

        (mockAuthorityRepository.all _)
          .expects()
          .returns(Future.failed(new Exception("BOOM!")))

        Get("/api/authorities") ~> router.sealedRoute ~> check {
          status shouldBe ApiError.generic.statusCode
          val response = responseAs[String]
          response shouldBe ApiError.generic.message
        }
      }

    }

    "provides POST /api/authorities route" which {

      "creates authority with valid data" in {
        val mockAuthorityRepository = mock[AuthorityRepository]
        val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
        val router = new AuthorityRouter(mockAuthorityRepository, mockFirebaseClient)

        (mockAuthorityRepository.create _)
          .expects(testAuthorityCreateRequest)
          .returns(Future.successful(testAuthorityCreateResponse))

        (Post("/api/authorities", testAuthorityCreateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[Authority]
          response shouldBe testAuthorityCreateResponse
        })
      }

      "does not create authority with invalid data" in {
        val mockAuthorityRepository = mock[AuthorityRepository]
        val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
        val router = new AuthorityRouter(mockAuthorityRepository, mockFirebaseClient)

        (mockAuthorityRepository.create _)
          .expects(*)
          .never

        (Post("/api/authorities", testAuthorityCreateRequestInvalidTitle)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          val apiError = ApiError.invalidAuthorityTitle(testAuthorityCreateRequestInvalidTitle.title)
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        })
      }

      "handles repository failure" in {
        val mockAuthorityRepository = mock[AuthorityRepository]
        val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
        val router = new AuthorityRouter(mockAuthorityRepository, mockFirebaseClient)

        (mockAuthorityRepository.create _)
          .expects(testAuthorityCreateRequest)
          .returns(Future.failed(new Exception("BOOM!")))

        (Post("/api/authorities", testAuthorityCreateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe ApiError.generic.statusCode
          val response = responseAs[String]
          response shouldBe ApiError.generic.message
        })
      }

      "handles failure to authorize" in {
        val mockAuthorityRepository = mock[AuthorityRepository]
        val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = false)
        val router = new AuthorityRouter(mockAuthorityRepository, mockFirebaseClient)

        (Post("/api/authorities", testAuthorityCreateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.Forbidden
        })
      }

      "handles failure to authenticate" in {
        val mockAuthorityRepository = mock[AuthorityRepository]
        val mockFirebaseClient = createMockFirebaseClient(testToken, maybeUser = None, isAdmin = false)
        val router = new AuthorityRouter(mockAuthorityRepository, mockFirebaseClient)

        (Post("/api/authorities", testAuthorityCreateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.Unauthorized
        })
      }

    }

  }

  }
