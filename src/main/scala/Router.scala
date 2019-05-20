import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}

import scala.util.control.NonFatal
import scala.util.{Failure, Success}

trait Router {

  def route: Route

}

class InquestRouter(inquestRepository: InquestRepository) extends Router with Directives {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  override def route: Route = pathPrefix("inquests") {
    pathEndOrSingleSlash {
      get {
        onComplete(inquestRepository.all()) {
          case Success(inquests) => complete(inquests)
          case Failure(NonFatal(error)) =>
            println(error.getMessage) // TODO: logging
            complete(StatusCodes.InternalServerError)
        }
      }
    }
  }
}
