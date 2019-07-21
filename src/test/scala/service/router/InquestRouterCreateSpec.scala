package service.router

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

import clients.firebase.{FirebaseClient, FirebaseUser}
import db.models.Inquest
import db.spec.InquestRepository
import service.models.{ApiError, CreateInquest}

import scala.concurrent.Future

class InquestRouterCreateSpec
  extends WordSpec with Matchers with ScalatestRouteTest with MockFactory {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val testCreateInquest = CreateInquest("Mega Shark vs Crocasaurus", "some inquest")
  private val testCreateInquestInvalidTitle = testCreateInquest.copy(title = "")
  private val testInquest = Inquest(Some(1), testCreateInquest.title, testCreateInquest.description)

  private val testUser = FirebaseUser("uid")
  private val testToken = "token"
  private val testCredentials = OAuth2BearerToken(testToken)

  private def createMockFirebaseClient(
    token: String,
    maybeUser: Option[FirebaseUser],
    isAdmin: Boolean
  ): FirebaseClient = {
    val mockFirebaseClient = mock[FirebaseClient]

    (mockFirebaseClient.getFirebaseUserFromToken _)
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

    "create inquest with valid data" in {
      val mockInquestRepository = mock[InquestRepository]
      val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
      val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

      (mockInquestRepository.create _)
        .expects(testCreateInquest)
        .returns(Future.successful(testInquest))

      Post("/api/inquests", testCreateInquest) ~> addCredentials(testCredentials) ~> router.sealedRoute ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Inquest]
        response shouldBe testInquest
      }
    }

    "not create inquest with invalid data" in {
      val mockInquestRepository = mock[InquestRepository]
      val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
      val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

      (mockInquestRepository.create _)
        .expects(*)
        .never

      Post("/api/inquests", testCreateInquestInvalidTitle) ~> addCredentials(testCredentials) ~> router.sealedRoute ~> check {
        val apiError = ApiError.invalidInquestTitle(testCreateInquestInvalidTitle.title)
        status shouldBe apiError.statusCode
        val response = responseAs[String]
        response shouldBe apiError.message
      }
    }

    "handle repository failure in inquests route" in {
      val mockInquestRepository = mock[InquestRepository]
      val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = true)
      val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

      (mockInquestRepository.create _)
        .expects(testCreateInquest)
        .returns(Future.failed(new Exception("BOOM!")))

      Post("/api/inquests", testCreateInquest) ~> addCredentials(testCredentials) ~> router.sealedRoute ~> check {
        status shouldBe ApiError.generic.statusCode
        val response = responseAs[String]
        response shouldBe ApiError.generic.message
      }
    }

    "handle failure to authorize in inquests route" in {
      val mockInquestRepository = mock[InquestRepository]
      val mockFirebaseClient = createMockFirebaseClient(testToken, Some(testUser), isAdmin = false)
      val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

      Post("/api/inquests", testCreateInquest) ~> addCredentials(testCredentials) ~> router.sealedRoute ~> check {
        status shouldBe StatusCodes.Forbidden
      }
    }

    "handle failure to authenticate in inquests route" in {
      val mockInquestRepository = mock[InquestRepository]
      val mockFirebaseClient = createMockFirebaseClient(testToken, maybeUser = None, isAdmin = false)
      val router = new InquestRouter(mockInquestRepository, mockFirebaseClient)

      Post("/api/inquests", testCreateInquest) ~> addCredentials(testCredentials) ~> router.sealedRoute ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

  }

}
