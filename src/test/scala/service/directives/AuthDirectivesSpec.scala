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

  private val testUser = FirebaseUser("uid")
  private val testToken = "token"
  private val testCredentials = OAuth2BearerToken(testToken)
  private val testCredentialsWrongSchema = BasicHttpCredentials("p4$$w0rd")

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

    "provide authenticateUser directive" which {

      "authenticates a user" in {
        val mockFirebaseClient = mock[FirebaseClient]

        (mockFirebaseClient.getFirebaseUserFromToken _)
          .expects(testToken)
          .returns(Future.successful(Some(testUser)))

        Get("/secured/user") ~> addCredentials(testCredentials) ~> testRoute(mockFirebaseClient) ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[FirebaseUser]
          response shouldBe testUser
        }
      }

      "returns 401 unauthorized if token was not successfully verified" in {
        val mockFirebaseClient = mock[FirebaseClient]

        (mockFirebaseClient.getFirebaseUserFromToken _)
          .expects(testToken)
          .returns(Future.successful(None))

        Get("/secured/user") ~> addCredentials(testCredentials) ~> testRoute(mockFirebaseClient) ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }

      "returns 401 unauthorized if authorization is missing" in {
        Get("/secured/user") ~> testRoute(mock[FirebaseClient]) ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }

      "returns 401 unauthorized if authorization has wrong scheme" in {
        Get("/secured/user") ~> addCredentials(testCredentialsWrongSchema) ~> testRoute(mock[FirebaseClient]) ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }

    }

    "provide authorizeAdmin directive" which {

      "authorizes an admin" in {
        val mockFirebaseClient = mock[FirebaseClient]

        (mockFirebaseClient.getFirebaseUserFromToken _)
          .expects(testToken)
          .returns(Future.successful(Some(testUser)))

        (mockFirebaseClient.isAdmin _)
          .expects(testUser)
          .returns(Future.successful(true))

        Get("/secured/admin") ~> addCredentials(testCredentials) ~> testRoute(mockFirebaseClient) ~> check {
          status shouldBe StatusCodes.OK
        }
      }

      "returns 403 forbidden if user is not admin" in {
        val mockFirebaseClient = mock[FirebaseClient]

        (mockFirebaseClient.getFirebaseUserFromToken _)
          .expects(testToken)
          .returns(Future.successful(Some(testUser)))

        (mockFirebaseClient.isAdmin _)
          .expects(testUser)
          .returns(Future.successful(false))

        Get("/secured/admin") ~> addCredentials(testCredentials) ~> testRoute(mockFirebaseClient) ~> check {
          status shouldBe StatusCodes.Forbidden
        }
      }

      "returns 401 unauthorized if token was not successfully verified" in {
        val mockFirebaseClient = mock[FirebaseClient]

        (mockFirebaseClient.getFirebaseUserFromToken _)
          .expects(testToken)
          .returns(Future.successful(None))

        Get("/secured/admin") ~> addCredentials(testCredentials) ~> testRoute(mockFirebaseClient) ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }

      "returns 401 unauthorized if authorization is missing" in {
        Get("/secured/admin") ~> testRoute(mock[FirebaseClient]) ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }

      "returns 401 unauthorized if authorization has wrong scheme" in {
        Get("/secured/admin") ~> addCredentials(testCredentialsWrongSchema) ~> testRoute(mock[FirebaseClient]) ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }

    }

  }

}
