import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future

class InquestDirectivesSpec extends WordSpec with Matchers with ScalatestRouteTest with Directives with InquestDirectives {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val testRoute = pathPrefix("test") {
    path("success") {
      get {
        handleWithGeneric(Future.unit) { _ =>
          complete(StatusCodes.OK)
        }
      }
    } ~ path("failure") {
      get {
        handleWithGeneric(Future.failed(new Exception("BOOM!"))) { _ =>
          complete(StatusCodes.OK)
        }
      }
    }
  }

  "InquestDirectives" should {

    "not return an error if the future succeeds" in {
      Get("/test/success") ~> testRoute ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return an error if the future fails" in {
      Get("/test/failure") ~> testRoute ~> check {
        status shouldBe StatusCodes.InternalServerError
        val response = responseAs[String]
        response shouldBe ApiError.generic.message
      }
    }

  }
}