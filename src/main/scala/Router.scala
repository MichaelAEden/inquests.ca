import akka.http.scaladsl.server.{Directives, Route}

trait Router {

  def route: Route

}

class InquestRouter(inquestRepository: InquestRepository) extends Router with Directives {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  override def route: Route = pathPrefix("inquests") {
    pathEndOrSingleSlash {
      get {
        complete(inquestRepository.all())
      }
    }
  }
}
