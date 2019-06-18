package service.router

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

import db.models.Inquest
import db.spec.InquestRepository
import service.models.ApiError

import scala.concurrent.Future

class InquestRouterListSpec extends WordSpec with Matchers with ScalatestRouteTest with MockFactory {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val testInquest1 = Inquest(Some(1), "Queen vs CBC", "some inquest")
  private val testInquest2 = Inquest(Some(2), "Superman vs Batman", "some inquest")

  private val testInquests = Seq(testInquest1, testInquest2)

  "InquestRouter" should {

    "return all inquests" in {
      val mockInquestRepository = mock[InquestRepository]
      val router = new InquestRouter(mockInquestRepository)

      (mockInquestRepository.all _)
        .expects()
        .returns(Future.successful(testInquests))

      Get("/api/inquests") ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Seq[Inquest]]
        response shouldBe testInquests
      }
    }

    "handle repository failure in inquests route" in {
      val mockInquestRepository = mock[InquestRepository]
      val router = new InquestRouter(mockInquestRepository)

      (mockInquestRepository.all _)
        .expects()
        .returns(Future.failed(new Exception("BOOM!")))

      Get("/api/inquests") ~> router.route ~> check {
        status shouldBe ApiError.generic.statusCode
        val response = responseAs[String]
        response shouldBe ApiError.generic.message
      }
    }

  }
}
