import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class InquestRouterListSpec extends WordSpec with Matchers with ScalatestRouteTest with InquestMocks {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val testInquest1 = Inquest("1", "Queen vs CBC", "some inquest")
  private val testInquest2 = Inquest("2", "Superman vs Batman", "some inquest")

  private val testInquests = Seq(testInquest1, testInquest2)

  "InquestRouter" should {

    "return all inquests" in {
      val repository = new InMemoryInquestRepository(testInquests)
      val router = new InquestRouter(repository)

      Get("/inquests") ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Seq[Inquest]]
        response shouldBe testInquests
      }
    }

    "handle repository failure in inquests route" in {
      val repository = new FailingRepository
      val router = new InquestRouter(repository)

      Get("/inquests") ~> router.route ~> check {
        status shouldBe ApiError.generic.statusCode
        val response = responseAs[String]
        response shouldBe ApiError.generic.message
      }
    }

  }
}
