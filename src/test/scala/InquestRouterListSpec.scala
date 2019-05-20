import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class InquestRouterListSpec extends WordSpec with Matchers with ScalatestRouteTest {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val inquest1 = Inquest("1", "Queen vs CBC", "some inquest")
  private val inquest2 = Inquest("2", "Superman vs Batman", "some inquest")

  private val inquests = Seq(inquest1, inquest2)

  "InquestRouter" should {

    "return all inquests" in {
      val repository = new InMemoryInquestRepository(inquests)
      val router = new InquestRouter(repository)

      Get("/inquests") ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Seq[Inquest]]
        response shouldBe inquests
      }
    }

  }
}
