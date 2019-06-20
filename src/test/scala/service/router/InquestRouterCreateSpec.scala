package service.router

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

import db.models.{CreateInquest, Inquest}
import db.spec.InquestRepository
import service.models.ApiError

import scala.concurrent.Future

class InquestRouterCreateSpec extends WordSpec with Matchers with ScalatestRouteTest with MockFactory {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val testCreateInquest = CreateInquest("Mega Shark vs Crocasaurus", "some inquest")
  private val testCreateInquestInvalidTitle = testCreateInquest.copy(title = "")
  private val testInquest = Inquest(Some(1), testCreateInquest.title, testCreateInquest.description)

  "InquestRouter" ignore {

    "create inquest with valid data" in {
      val mockInquestRepository = mock[InquestRepository]
      val router = new InquestRouter(mockInquestRepository)

      (mockInquestRepository.create _)
        .expects(testCreateInquest)
        .returns(Future.successful(testInquest))

      Post("/api/inquests", testCreateInquest) ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Inquest]
        response shouldBe testInquest
      }
    }

    "not create inquest with invalid data" in {
      val mockInquestRepository = mock[InquestRepository]
      val router = new InquestRouter(mockInquestRepository)

      (mockInquestRepository.create _)
        .expects(*)
        .never

      Post("/api/inquests", testCreateInquestInvalidTitle) ~> router.route ~> check {
        val apiError = ApiError.invalidInquestTitle(testCreateInquestInvalidTitle.title)
        status shouldBe apiError.statusCode
        val response = responseAs[String]
        response shouldBe apiError.message
      }
    }

    "handle repository failure in inquests route" in {
      val mockInquestRepository = mock[InquestRepository]
      val router = new InquestRouter(mockInquestRepository)

      (mockInquestRepository.create _)
        .expects(testCreateInquest)
        .returns(Future.failed(new Exception("BOOM!")))

      Post("/api/inquests", testCreateInquest) ~> router.route ~> check {
        status shouldBe ApiError.generic.statusCode
        val response = responseAs[String]
        response shouldBe ApiError.generic.message
      }
    }

  }
}
