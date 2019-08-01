package service.directives

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, OAuth2BearerToken}
import akka.http.scaladsl.server.Route
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

import clients.firebase.{FirebaseClient, FirebaseUser}
import db.models.{Action, Role, User}
import db.spec.UserRepository
import service.models.UserResponse

import scala.concurrent.{ExecutionContextExecutor, Future}

class AuthDirectivesSpec
  extends WordSpec with Matchers with ScalatestRouteTest with AuthDirectives with MockFactory {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private implicit val ece: ExecutionContextExecutor = system.dispatcher

  private val testFirebaseUid = "uid"
  private val testEmail = "email"
  private val testFirebaseUser = FirebaseUser(testFirebaseUid, testEmail)
  private val testUser = User(None, testFirebaseUid, testEmail, "", "", Role.User, None)
  private val testEditor = testUser.copy(role = Role.Editor)
  private val testAdmin = testUser.copy(role = Role.Admin)

  private val testToken = "token"
  private val testCredentials = OAuth2BearerToken(testToken)
  private val testCredentialsWrongSchema = BasicHttpCredentials("p4$$w0rd")

  private def testRoute(implicit firebaseClient: FirebaseClient, userRepository: UserRepository) = Route.seal {
    pathPrefix("test") {
      path("firebaseAuthentication") {
        authenticateFirebaseUser("testing authentication") apply { firebaseUser =>
          complete(firebaseUser)
        }
      } ~ path("userAuthentication") {
        authenticateUser("testing authentication") apply { user =>
          val userResponse = UserResponse.fromUser(user)
          complete(userResponse)
        }
      } ~ pathPrefix("userAuthorization") {
        pathPrefix("editAuthority") {
          authorizeAction(Action.EditAuthority) apply { user =>
            val userResponse = UserResponse.fromUser(user)
            complete(userResponse)
          }
        } ~ pathPrefix("manageUsers") {
          authorizeAction(Action.ManageUsers) apply { user =>
            val userResponse = UserResponse.fromUser(user)
            complete(userResponse)
          }
        }
      }
    }
  }

  "AuthDirectives" should {

    "provide authenticateFirebaseUser directive" which {

      "authenticates a Firebase user" in {
        val mockFirebaseClient = mock[FirebaseClient]

        (mockFirebaseClient.getFirebaseUserFromToken _)
          .expects(testToken)
          .returns(Future.successful(Some(testFirebaseUser)))

        (Get("/test/firebaseAuthentication")
          ~> addCredentials(testCredentials)
          ~> testRoute(mockFirebaseClient, mock[UserRepository])
          ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[FirebaseUser]
          response shouldBe testFirebaseUser
        })
      }

      "returns 401 unauthorized if token was not successfully verified" in {
        val mockFirebaseClient = mock[FirebaseClient]

        (mockFirebaseClient.getFirebaseUserFromToken _)
          .expects(testToken)
          .returns(Future.successful(None))

        (Get("/test/firebaseAuthentication")
          ~> addCredentials(testCredentials)
          ~> testRoute(mockFirebaseClient, mock[UserRepository])
          ~> check {
          status shouldBe StatusCodes.Unauthorized
        })
      }

      "returns 401 unauthorized if authorization is missing" in {
        (Get("/test/firebaseAuthentication")
          ~> testRoute(mock[FirebaseClient], mock[UserRepository])
          ~> check {
          status shouldBe StatusCodes.Unauthorized
        })
      }

      "returns 401 unauthorized if authorization has wrong scheme" in {
        (Get("/test/firebaseAuthentication")
          ~> addCredentials(testCredentialsWrongSchema)
          ~> testRoute(mock[FirebaseClient], mock[UserRepository])
          ~> check {
          status shouldBe StatusCodes.Unauthorized
        })
      }

    }

    "provide authenticateUser directive" which {

      "authenticates a user" in {
        val mockFirebaseClient = mock[FirebaseClient]
        val mockUserRepository = mock[UserRepository]

        (mockFirebaseClient.getFirebaseUserFromToken _)
          .expects(testToken)
          .returns(Future.successful(Some(testFirebaseUser)))

        (mockUserRepository.byFirebaseUid _)
          .expects(testFirebaseUid)
          .returns(Future.successful(testUser))

        (Get("/test/userAuthentication")
          ~> addCredentials(testCredentials)
          ~> testRoute(mockFirebaseClient, mockUserRepository)
          ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[UserResponse]
          response shouldBe UserResponse.fromUser(testUser)
        })
      }

      "returns 401 unauthorized if token was not successfully verified" in {
        val mockFirebaseClient = mock[FirebaseClient]

        (mockFirebaseClient.getFirebaseUserFromToken _)
          .expects(testToken)
          .returns(Future.successful(None))

        (Get("/test/userAuthentication")
          ~> addCredentials(testCredentials)
          ~> testRoute(mockFirebaseClient, mock[UserRepository])
          ~> check {
          status shouldBe StatusCodes.Unauthorized
        })
      }

      "returns 401 unauthorized if authorization is missing" in {
        (Get("/test/userAuthentication")
          ~> testRoute(mock[FirebaseClient], mock[UserRepository])
          ~> check {
          status shouldBe StatusCodes.Unauthorized
        })
      }

      "returns 401 unauthorized if authorization has wrong scheme" in {
        (Get("/test/userAuthentication")
          ~> addCredentials(testCredentialsWrongSchema)
          ~> testRoute(mock[FirebaseClient], mock[UserRepository])
          ~> check {
          status shouldBe StatusCodes.Unauthorized
        })
      }

      "returns 500 internal server error if an unknown error was thrown by the repository" in {
        val mockFirebaseClient = mock[FirebaseClient]
        val mockUserRepository = mock[UserRepository]

        (mockFirebaseClient.getFirebaseUserFromToken _)
          .expects(testToken)
          .returns(Future.successful(Some(testFirebaseUser)))

        (mockUserRepository.byFirebaseUid _)
          .expects(testFirebaseUid)
          .returns(Future.failed(new Exception("BOOM!")))

        (Get("/test/userAuthentication")
          ~> addCredentials(testCredentials)
          ~> testRoute(mockFirebaseClient, mockUserRepository)
          ~> check {
          status shouldBe StatusCodes.InternalServerError
        })
      }

    }

    "provide authorizeAction directive" which {

      "authorizes an admin to manage users" in {
        val mockFirebaseClient = mock[FirebaseClient]
        val mockUserRepository = mock[UserRepository]

        (mockFirebaseClient.getFirebaseUserFromToken _)
          .expects(testToken)
          .returns(Future.successful(Some(testFirebaseUser)))

        (mockUserRepository.byFirebaseUid _)
          .expects(testFirebaseUid)
          .returns(Future.successful(testAdmin))

        (Get("/test/userAuthorization/manageUsers")
          ~> addCredentials(testCredentials)
          ~> testRoute(mockFirebaseClient, mockUserRepository)
          ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[UserResponse]
          response shouldBe UserResponse.fromUser(testAdmin)
        })
      }

      "authorizes an editor to edit an authority" in {
        val mockFirebaseClient = mock[FirebaseClient]
        val mockUserRepository = mock[UserRepository]

        (mockFirebaseClient.getFirebaseUserFromToken _)
          .expects(testToken)
          .returns(Future.successful(Some(testFirebaseUser)))

        (mockUserRepository.byFirebaseUid _)
          .expects(testFirebaseUid)
          .returns(Future.successful(testEditor))

        (Get("/test/userAuthorization/editAuthority")
          ~> addCredentials(testCredentials)
          ~> testRoute(mockFirebaseClient, mockUserRepository)
          ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[UserResponse]
          response shouldBe UserResponse.fromUser(testEditor)
        })
      }

      "returns 403 forbidden if editor attempts to manage users" in {
        val mockFirebaseClient = mock[FirebaseClient]
        val mockUserRepository = mock[UserRepository]

        (mockFirebaseClient.getFirebaseUserFromToken _)
          .expects(testToken)
          .returns(Future.successful(Some(testFirebaseUser)))

        (mockUserRepository.byFirebaseUid _)
          .expects(testFirebaseUid)
          .returns(Future.successful(testEditor))

        (Get("/test/userAuthorization/manageUsers")
          ~> addCredentials(testCredentials)
          ~> testRoute(mockFirebaseClient, mockUserRepository)
          ~> check {
          status shouldBe StatusCodes.Forbidden
        })
      }

      "returns 403 forbidden if user attempts to edit authority" in {
        val mockFirebaseClient = mock[FirebaseClient]
        val mockUserRepository = mock[UserRepository]

        (mockFirebaseClient.getFirebaseUserFromToken _)
          .expects(testToken)
          .returns(Future.successful(Some(testFirebaseUser)))

        (mockUserRepository.byFirebaseUid _)
          .expects(testFirebaseUid)
          .returns(Future.successful(testUser))

        (Get("/test/userAuthorization/editAuthority")
          ~> addCredentials(testCredentials)
          ~> testRoute(mockFirebaseClient, mockUserRepository)
          ~> check {
          status shouldBe StatusCodes.Forbidden
        })
      }

      "returns 401 unauthorized if token was not successfully verified" in {
        val mockFirebaseClient = mock[FirebaseClient]

        (mockFirebaseClient.getFirebaseUserFromToken _)
          .expects(testToken)
          .returns(Future.successful(None))

        (Get("/test/userAuthorization/manageUsers")
          ~> addCredentials(testCredentials)
          ~> testRoute(mockFirebaseClient, mock[UserRepository])
          ~> check {
          status shouldBe StatusCodes.Unauthorized
        })
      }

      "returns 401 unauthorized if authorization is missing" in {
        (Get("/test/userAuthorization/manageUsers")
          ~> testRoute(mock[FirebaseClient], mock[UserRepository])
          ~> check {
          status shouldBe StatusCodes.Unauthorized
        })
      }

      "returns 401 unauthorized if authorization has wrong scheme" in {
        (Get("/test/userAuthorization/manageUsers")
          ~> addCredentials(testCredentialsWrongSchema)
          ~> testRoute(mock[FirebaseClient], mock[UserRepository])
          ~> check {
          status shouldBe StatusCodes.Unauthorized
        })
      }

      "returns 500 internal server error if an unknown error was thrown by the repository" in {
        val mockFirebaseClient = mock[FirebaseClient]
        val mockUserRepository = mock[UserRepository]

        (mockFirebaseClient.getFirebaseUserFromToken _)
          .expects(testToken)
          .returns(Future.successful(Some(testFirebaseUser)))

        (mockUserRepository.byFirebaseUid _)
          .expects(testFirebaseUid)
          .returns(Future.failed(new Exception("BOOM!")))

        (Get("/test/userAuthorization/editAuthority")
          ~> addCredentials(testCredentials)
          ~> testRoute(mockFirebaseClient, mockUserRepository)
          ~> check {
          status shouldBe StatusCodes.InternalServerError
        })
      }

    }

  }

}
