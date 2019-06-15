package service.router

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

import db.models.{CreateInquest, Inquest}
import mocks.InquestMocks
import service.models.ApiError

import scala.concurrent.Await
import scala.concurrent.duration._

class InquestRouterCreateSpec extends WordSpec with BeforeAndAfter with Matchers with ScalatestRouteTest with InquestMocks {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val testCreateInquest = CreateInquest("Mega Shark vs Crocasaurus", "some inquest")
  private val testCreateInquestInvalidTitle = testCreateInquest.copy(title = "")

  private val timeout = 500.milliseconds

  private val inquestRepository = testRepository

  before {
    Await.result(inquestRepository.init(), timeout)
  }

  after {
    Await.result(inquestRepository.drop(), timeout)
  }

  "InquestRouter" should {

    "create inquest with valid data" in {
      val router = new InquestRouter(inquestRepository)

      Post("/api/inquests", testCreateInquest) ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Inquest]
        response.title shouldBe testCreateInquest.title
        response.description shouldBe testCreateInquest.description
      }
    }

    "not create inquest with invalid data" in {
      val router = new InquestRouter(inquestRepository)

      Post("/api/inquests", testCreateInquestInvalidTitle) ~> router.route ~> check {
        val apiError = ApiError.invalidInquestTitle(testCreateInquestInvalidTitle.title)
        status shouldBe apiError.statusCode
        val response = responseAs[String]
        response shouldBe apiError.message
      }
    }

    "handle repository failure in inquests route" in {
      val repository = new FailingRepository
      val router = new InquestRouter(repository)

      Post("/api/inquests", testCreateInquest) ~> router.route ~> check {
        status shouldBe ApiError.generic.statusCode
        val response = responseAs[String]
        response shouldBe ApiError.generic.message
      }
    }

  }
}
