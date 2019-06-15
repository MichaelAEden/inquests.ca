package service.router

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

import db.models.Inquest
import mocks.InquestMocks
import service.models.ApiError

import scala.concurrent.Await
import scala.concurrent.duration._

class InquestRouterListSpec extends WordSpec with BeforeAndAfter with Matchers with ScalatestRouteTest with InquestMocks {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val testInquest1 = Inquest(Some(1), "Queen vs CBC", "some inquest")
  private val testInquest2 = Inquest(Some(2), "Superman vs Batman", "some inquest")

  private val testInquests = Seq(testInquest1, testInquest2)

  private val timeout = 5.seconds

  private val inquestRepository = testRepository

  before {
    Await.result(inquestRepository.init(testInquests), timeout)
  }

  after {
    Await.result(inquestRepository.drop(), timeout)
  }

  "InquestRouter" should {

    "return all inquests" in {
      val router = new InquestRouter(inquestRepository)

      Get("/api/inquests") ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Seq[Inquest]]
        response shouldBe testInquests
      }
    }

    "handle repository failure in inquests route" in {
      val repository = new FailingRepository
      val router = new InquestRouter(repository)

      Get("/api/inquests") ~> router.route ~> check {
        status shouldBe ApiError.generic.statusCode
        val response = responseAs[String]
        response shouldBe ApiError.generic.message
      }
    }

  }
}
