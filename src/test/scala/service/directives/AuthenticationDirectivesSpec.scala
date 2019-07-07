package service.directives

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model.{headers => httpHeaders}
import com.google.firebase.auth.FirebaseAuthException
import org.scalamock.scalatest.MockFactory
import service.models.{ApiError, User}

class AuthenticationDirectivesSpec
  extends WordSpec with Matchers with ScalatestRouteTest with AuthenticationDirectives with MockFactory {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  val testUserUid = "userUid"
  val testAdminUid = "adminUid"

  class MockableUser extends User(testUserUid)
  class MockableAdmin extends User(testAdminUid)

  private val testRoute = pathPrefix("test") {
    pathPrefix("user") {
      authenticateUser { user =>
        complete(user)
      }
    } ~ pathPrefix("admin") {
      authenticateAdmin { admin =>
        complete(admin)
      }
    }
  }

  // Mocking FirebaseAuth is difficult, so instead we will override this function for these tests.
  override def getUserFromToken(idToken: String): User = {
    idToken match {
      case "invalidToken" => throw new FirebaseAuthException("BOOM!", "BAM!")
      case "userToken" =>
        val mockUser = stub[MockableUser]
        (mockUser.isAdmin _)
          .when()
          .returns(false)
        mockUser
      case "adminToken" =>
        val mockAdmin = stub[MockableAdmin]
        (mockAdmin.isAdmin _)
          .when()
          .returns(true)
        mockAdmin
    }
  }

  "AuthenticationDirectives" should {

    "provide an authenticateUser directive" which {

      "authenticates a user" in {
        val authorizationHeader = httpHeaders.Authorization(httpHeaders.OAuth2BearerToken("userToken"))
        Get("/test/user").addHeader(authorizationHeader) ~> testRoute ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[User]
          response shouldBe User(testUserUid)
        }
      }

      "return authentication error if an error occurs while verifying token" in {
        val authorizationHeader = httpHeaders.Authorization(httpHeaders.OAuth2BearerToken("invalidToken"))
        Get("/test/user").addHeader(authorizationHeader) ~> testRoute ~> check {
          val apiError = ApiError.authenticationFailure
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        }
      }

      "return unauthorized error if authorization header is missing" in {
        Get("/test/user") ~> testRoute ~> check {
          val apiError = ApiError.unauthorized
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        }
      }

      "return unauthorized error if authorization header has wrong authorization scheme" in {
        val authorizationHeader = httpHeaders.Authorization(httpHeaders.BasicHttpCredentials("someCredentials"))
        Get("/test/user").addHeader(authorizationHeader) ~> testRoute ~> check {
          val apiError = ApiError.unauthorized
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        }
      }

    }

    "provide an authenticateAdmin directive" which {

      "authenticates an admin" in {
        val authorizationHeader = httpHeaders.Authorization(httpHeaders.OAuth2BearerToken("adminToken"))
        Get("/test/admin").addHeader(authorizationHeader) ~> testRoute ~> check {
          status shouldBe StatusCodes.OK
        }
      }

      "return admin privilege required error if user is not admin" in {
        val authorizationHeader = httpHeaders.Authorization(httpHeaders.OAuth2BearerToken("userToken"))
        Get("/test/admin").addHeader(authorizationHeader) ~> testRoute ~> check {
          val apiError = ApiError.adminPrivilegeRequired
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        }
      }

      "return authentication error if an error occurs while verifying token" in {
        val authorizationHeader = httpHeaders.Authorization(httpHeaders.OAuth2BearerToken("invalidToken"))
        Get("/test/admin").addHeader(authorizationHeader) ~> testRoute ~> check {
          val apiError = ApiError.authenticationFailure
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        }
      }

      "return unauthorized error if authorization header is missing" in {
        Get("/test/admin") ~> testRoute ~> check {
          val apiError = ApiError.unauthorized
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        }
      }

      "return unauthorized error if authorization header has wrong authorization scheme" in {
        val authorizationHeader = httpHeaders.Authorization(httpHeaders.BasicHttpCredentials("someCredentials"))
        Get("/test/admin").addHeader(authorizationHeader) ~> testRoute ~> check {
          val apiError = ApiError.unauthorized
          status shouldBe apiError.statusCode
          val response = responseAs[String]
          response shouldBe apiError.message
        }
      }

    }

  }

}
