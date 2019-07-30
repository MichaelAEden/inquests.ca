package service.router

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.testkit.ScalatestRouteTest

import clients.firebase.FirebaseUser
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import db.models.{Role, User}
import db.spec.UserRepository
import db.spec.UserRepository.UserNotFound
import mocks.AuthMocks
import service.models._

import scala.concurrent.Future

class UserRouterSpec
  extends WordSpec with Matchers with ScalatestRouteTest with MockFactory with AuthMocks {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val testFirebaseUid = "uid"
  private val testName = "name"
  private val testEmail = "email"
  private val testFirebaseUser = FirebaseUser(testFirebaseUid, testEmail)
  private val testUser = User(None, testFirebaseUid, testEmail, testName, "", Role.User, None)
  private val testEditor = testUser.copy(role = Role.Editor)
  private val testAdmin = testUser.copy(role = Role.Admin)

  private val testUserCreateRequest = UserCreateRequest(testName, "")
  private val testUserCreateRequestInvalid = testUserCreateRequest.copy(name = "")

  private val testUserUpdateRequest = UserUpdateRequest(Some(testName), None, None)
  private val testUserUpdateRequestInvalid = testUserUpdateRequest.copy(name = Some(""))

  private val testToken = "token"
  private val testCredentials = OAuth2BearerToken("token")

  "UserRouter" should {

    "provide GET /api/users route" which {

      "returns user" in {
        val (mockFirebaseClient, mockUserRepository) = mockUserAuthentication(testToken, Some(testAdmin))
        val router = new UserRouter(mockUserRepository, mockFirebaseClient)

        (mockUserRepository.byEmail _)
          .expects(testEmail)
          .returns(Future.successful(testUser))

        (Get(s"/api/users?email=$testEmail")
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[UserResponse]
          response shouldBe UserResponse.fromUser(testUser)
        })
      }

      "handles user not found" in {
        val (mockFirebaseClient, mockUserRepository) = mockUserAuthentication(testToken, Some(testAdmin))
        val router = new UserRouter(mockUserRepository, mockFirebaseClient)

        (mockUserRepository.byEmail _)
          .expects(testEmail)
          .returns(Future.failed(UserNotFound()))

        (Get(s"/api/users?email=$testEmail")
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          val apiError = ApiError.userNotFound
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        })
      }

      "handles repository failure" in {
        val (mockFirebaseClient, mockUserRepository) = mockUserAuthentication(testToken, Some(testAdmin))
        val router = new UserRouter(mockUserRepository, mockFirebaseClient)

        (mockUserRepository.byEmail _)
          .expects(testEmail)
          .returns(Future.failed(new Exception("BOOM!")))

        (Get(s"/api/users?email=$testEmail")
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          val apiError = ApiError.generic
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        })
      }

      "handles unauthorized user" in {
        val (mockFirebaseClient, mockUserRepository) = mockUserAuthentication(testToken, Some(testEditor))
        val router = new UserRouter(mockUserRepository, mockFirebaseClient)

        (Get(s"/api/users?email=$testEmail")
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.Forbidden
        })
      }

    }

    "provide POST /api/users route" which {

      "creates user with valid data" in {
        val mockFirebaseClient = mockFirebaseUserAuthentication(testToken, Some(testFirebaseUser))
        val mockUserRepository = mock[UserRepository]
        val router = new UserRouter(mockUserRepository, mockFirebaseClient)

        (mockUserRepository.create _)
          .expects(testUserCreateRequest, testFirebaseUser)
          .returns(Future.successful(testUser))

        (Post(s"/api/users", testUserCreateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[UserResponse]
          response shouldBe UserResponse.fromUser(testUser)
        })
      }

      "does not create user with invalid data" in {
        val mockFirebaseClient = mockFirebaseUserAuthentication(testToken, Some(testFirebaseUser))
        val mockUserRepository = mock[UserRepository]
        val router = new UserRouter(mockUserRepository, mockFirebaseClient)

        (Post(s"/api/users", testUserCreateRequestInvalid)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          val apiError = ApiError.emptyUserName
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        })
      }

      "handles repository failure" in {
        val mockFirebaseClient = mockFirebaseUserAuthentication(testToken, Some(testFirebaseUser))
        val mockUserRepository = mock[UserRepository]
        val router = new UserRouter(mockUserRepository, mockFirebaseClient)

        (mockUserRepository.create _)
          .expects(testUserCreateRequest, testFirebaseUser)
          .returns(Future.failed(new Exception("BOOM!")))

        (Post(s"/api/users", testUserCreateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          val apiError = ApiError.generic
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        })
      }

      "handles unauthenticated user" in {
        val mockFirebaseClient = mockFirebaseUserAuthentication(testToken, None)
        val router = new UserRouter(mock[UserRepository], mockFirebaseClient)

        (Post(s"/api/users", testUserCreateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.Unauthorized
        })
      }

    }

    "provide PUT /api/users route" which {

      "updates user with valid data" in {
        val (mockFirebaseClient, mockUserRepository) = mockUserAuthentication(testToken, Some(testAdmin))
        val router = new UserRouter(mockUserRepository, mockFirebaseClient)

        (mockUserRepository.update _)
          .expects(1, testUserUpdateRequest)
          .returns(Future.successful(testUser))

        (Put(s"/api/users/1", testUserUpdateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[UserResponse]
          response shouldBe UserResponse.fromUser(testUser)
        })
      }

      "does not update user with invalid data" in {
        val (mockFirebaseClient, mockUserRepository) = mockUserAuthentication(testToken, Some(testAdmin))
        val router = new UserRouter(mockUserRepository, mockFirebaseClient)

        (Put(s"/api/users/1", testUserUpdateRequestInvalid)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          val apiError = ApiError.emptyUserName
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        })
      }

      "handles user not found" in {
        val (mockFirebaseClient, mockUserRepository) = mockUserAuthentication(testToken, Some(testAdmin))
        val router = new UserRouter(mockUserRepository, mockFirebaseClient)

        (mockUserRepository.update _)
          .expects(1, testUserUpdateRequest)
          .returns(Future.failed(UserNotFound()))

        (Put(s"/api/users/1", testUserUpdateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          val apiError = ApiError.userNotFound
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        })
      }

      "handles repository failure" in {
        val (mockFirebaseClient, mockUserRepository) = mockUserAuthentication(testToken, Some(testAdmin))
        val router = new UserRouter(mockUserRepository, mockFirebaseClient)

        (mockUserRepository.update _)
          .expects(1, testUserUpdateRequest)
          .returns(Future.failed(new Exception("BOOM!")))

        (Put(s"/api/users/1", testUserUpdateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          val apiError = ApiError.generic
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        })
      }

      "handles unauthorized user" in {
        val (mockFirebaseClient, mockUserRepository) = mockUserAuthentication(testToken, Some(testEditor))
        val router = new UserRouter(mockUserRepository, mockFirebaseClient)

        (Put(s"/api/users/1", testUserUpdateRequest)
          ~> addCredentials(testCredentials)
          ~> router.sealedRoute
          ~> check {
          status shouldBe StatusCodes.Forbidden
        })
      }

    }

  }

}


