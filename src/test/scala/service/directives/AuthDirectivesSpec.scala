package service.directives

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, OAuth2BearerToken}
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import clients.firebase.{FirebaseClient, FirebaseUser}

import scala.concurrent.{ExecutionContextExecutor, Future}

class AuthDirectivesSpec
  extends WordSpec with Matchers with ScalatestRouteTest with AuthDirectives with MockFactory {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private implicit val ece: ExecutionContextExecutor = system.dispatcher

  private val testUser = FirebaseUser("userUid")
  private val testAdmin = FirebaseUser("adminUid")

  private val testUserToken = "userToken"
  private val testAdminToken = "adminToken"

  private def testRoute(implicit firebaseClient: FirebaseClient) = Route.seal {
    pathPrefix("secured") {
      pathPrefix("user") {
        authenticateUser("test server") apply { user =>
          complete(user)
        }
      } ~ pathPrefix("admin") {
        authorizeAdmin("test server") apply { admin =>
          complete(admin)
        }
      }
    }
  }

  "AuthDirectives" should {

    "provide an authenticateUser directive" which {

      "authenticates a user" in {
        val credentials = OAuth2BearerToken(testUserToken)
        val mockFirebaseClient = mock[FirebaseClient]

        (mockFirebaseClient.getUserFromToken _)
          .expects(testUserToken)
          .returns(Future.successful(Some(testUser)))

        Get("/secured/user") ~> addCredentials(credentials) ~> testRoute(mockFirebaseClient) ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[FirebaseUser]
          response shouldBe testUser
        }
      }

      "returns 401 unauthorized if token was not successfully verified" in {
        val credentials = OAuth2BearerToken(testUserToken)
        val mockFirebaseClient = mock[FirebaseClient]

        (mockFirebaseClient.getUserFromToken _)
          .expects(testUserToken)
          .returns(Future.successful(None))

        Get("/secured/user") ~> addCredentials(credentials) ~> testRoute(mockFirebaseClient) ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }

      "returns 401 unauthorized if authorization is missing" in {
        Get("/secured/user") ~> testRoute(mock[FirebaseClient]) ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }

      "returns 401 unauthorized if authorization has wrong scheme" in {
        val credentials = BasicHttpCredentials("p4$$w0rd")

        Get("/secured/user") ~> addCredentials(credentials) ~> testRoute(mock[FirebaseClient]) ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }

    }

    "provide an authenticateAdmin directive" which {

      "authenticates an admin" in {
        val credentials = OAuth2BearerToken(testAdminToken)
        val mockFirebaseClient = mock[FirebaseClient]

        (mockFirebaseClient.getUserFromToken _)
          .expects(testAdminToken)
          .returns(Future.successful(Some(testAdmin)))

        (mockFirebaseClient.isAdmin _)
          .expects(testAdmin)
          .returns(Future.successful(true))

        Get("/secured/admin") ~> addCredentials(credentials) ~> testRoute(mockFirebaseClient) ~> check {
          status shouldBe StatusCodes.OK
        }
      }

      "returns 403 forbidden if user is not admin" in {
        val credentials = OAuth2BearerToken(testUserToken)
        val mockFirebaseClient = mock[FirebaseClient]

        (mockFirebaseClient.getUserFromToken _)
          .expects(testUserToken)
          .returns(Future.successful(Some(testUser)))

        (mockFirebaseClient.isAdmin _)
          .expects(testUser)
          .returns(Future.successful(false))

        Get("/secured/admin") ~> addCredentials(credentials) ~> testRoute(mockFirebaseClient) ~> check {
          status shouldBe StatusCodes.Forbidden
        }
      }

      "returns 401 unauthorized if token was not successfully verified" in {
        val credentials = OAuth2BearerToken(testAdminToken)
        val mockFirebaseClient = mock[FirebaseClient]

        (mockFirebaseClient.getUserFromToken _)
          .expects(testAdminToken)
          .returns(Future.successful(None))

        Get("/secured/admin") ~> addCredentials(credentials) ~> testRoute(mockFirebaseClient) ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }

      "returns 401 unauthorized if authorization is missing" in {
        Get("/secured/admin") ~> testRoute(mock[FirebaseClient]) ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }

      "returns 401 unauthorized if authorization has wrong scheme" in {
        val credentials = BasicHttpCredentials("p4$$w0rd")

        Get("/secured/admin") ~> addCredentials(credentials) ~> testRoute(mock[FirebaseClient]) ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }

    }

  }

}
