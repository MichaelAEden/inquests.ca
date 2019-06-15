package service.router

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

import db.models.{Inquest, UpdateInquest}
import mocks.InquestMocks
import service.models.ApiError

import scala.concurrent.Await
import scala.concurrent.duration._

class InquestRouterUpdateSpec extends WordSpec with BeforeAndAfter with Matchers with ScalatestRouteTest with InquestMocks {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val testInquestId = 1
  private val testInquest = Inquest(Some(1), "Queen vs CBC", "some inquest")
  private val testUpdateInquest = UpdateInquest(Some("Queen vs CBC"), Some("some inquest"))
  private val testUpdateInquestInvalidTitle = UpdateInquest(Some(""), Some("some inquest"))

  private val testInquests = Seq(testInquest)

  private val timeout = 500.milliseconds

  private val inquestRepository = testRepository

  before {
    Await.result(inquestRepository.init(testInquests), timeout)
  }

  after {
    Await.result(inquestRepository.drop(), timeout)
  }

  "InquestRouter" should {

    "update an inquest with valid data" in {
      val router = new InquestRouter(inquestRepository)

      Put(s"/api/inquests/$testInquestId", testUpdateInquest) ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Inquest]
        response shouldBe testInquest
      }
    }

    "return not found if inquest does not exist" in {
      val router = new InquestRouter(inquestRepository)

      Put("/api/inquests/2", testUpdateInquest) ~> router.route ~> check {
        val apiError = ApiError.inquestNotFound(2)
        status shouldBe apiError.statusCode
        val response = responseAs[String]
        response shouldBe apiError.message
      }
    }

    "not update an inquest with invalid data" in {
      val router = new InquestRouter(inquestRepository)

      Put(s"/api/inquests/$testInquestId", testUpdateInquestInvalidTitle) ~> router.route ~> check {
        val apiError = ApiError.invalidInquestTitle(testUpdateInquestInvalidTitle.title.get)
        status shouldBe apiError.statusCode
        val response = responseAs[String]
        response shouldBe apiError.message
      }
    }

    "handle repository failure" in {
      val repository = new FailingRepository
      val router = new InquestRouter(repository)

      Put(s"/api/inquests/$testInquestId", testUpdateInquest) ~> router.route ~> check {
        status shouldBe ApiError.generic.statusCode
        val response = responseAs[String]
        response shouldBe ApiError.generic.message
      }
    }

  }

}
