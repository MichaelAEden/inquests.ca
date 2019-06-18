package service.router

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

import db.models.{Inquest, UpdateInquest}
import db.spec.InquestRepository
import db.spec.InquestRepository.InquestNotFound
import service.models.ApiError

import scala.concurrent.Future

class InquestRouterUpdateSpec extends WordSpec with Matchers with ScalatestRouteTest with MockFactory {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val testInquestId = 1
  private val testInquest = Inquest(Some(1), "Queen vs CBC", "some inquest")
  private val testUpdateInquest = UpdateInquest(Some("Queen vs CBC"), Some("some inquest"))
  private val testUpdateInquestInvalidTitle = UpdateInquest(Some(""), Some("some inquest"))

  private val testInquests = Seq(testInquest)

  "InquestRouter" should {

    "update an inquest with valid data" in {
      val mockInquestRepository = mock[InquestRepository]
      val router = new InquestRouter(mockInquestRepository)

      (mockInquestRepository.update _)
        .expects(testInquestId, testUpdateInquest)
        .returns(Future.successful(testInquest))

      Put(s"/api/inquests/$testInquestId", testUpdateInquest) ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Inquest]
        response shouldBe testInquest
      }
    }

    "return not found if inquest does not exist" in {
      val mockInquestRepository = mock[InquestRepository]
      val router = new InquestRouter(mockInquestRepository)

      (mockInquestRepository.update _)
        .expects(2, testUpdateInquest)
        .returns(Future.failed(InquestNotFound(2)))

      Put("/api/inquests/2", testUpdateInquest) ~> router.route ~> check {
        val apiError = ApiError.inquestNotFound(2)
        status shouldBe apiError.statusCode
        val response = responseAs[String]
        response shouldBe apiError.message
      }
    }

    "not update an inquest with invalid data" in {
      val mockInquestRepository = mock[InquestRepository]
      val router = new InquestRouter(mockInquestRepository)

      (mockInquestRepository.update _)
        .expects(*, *)
        .never

      Put(s"/api/inquests/$testInquestId", testUpdateInquestInvalidTitle) ~> router.route ~> check {
        val apiError = ApiError.invalidInquestTitle(testUpdateInquestInvalidTitle.title.get)
        status shouldBe apiError.statusCode
        val response = responseAs[String]
        response shouldBe apiError.message
      }
    }

    "handle repository failure" in {
      val mockInquestRepository = mock[InquestRepository]
      val router = new InquestRouter(mockInquestRepository)

      (mockInquestRepository.update _)
        .expects(testInquestId, testUpdateInquest)
        .returns(Future.failed(new Exception("BOOM!")))

      Put(s"/api/inquests/$testInquestId", testUpdateInquest) ~> router.route ~> check {
        status shouldBe ApiError.generic.statusCode
        val response = responseAs[String]
        response shouldBe ApiError.generic.message
      }
    }

  }

}
