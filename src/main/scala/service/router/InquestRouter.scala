package service.router

import akka.http.scaladsl.server.Route

import db.models.{CreateInquest, UpdateInquest}
import db.spec.InquestRepository
import service.directives._
import service.models.ApiError

class InquestRouter(inquestRepository: InquestRepository) extends Router with HandlerDirectives with ValidatorDirectives {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  override def route: Route = pathPrefix("api") {
    pathPrefix("inquests") {
      pathEndOrSingleSlash {
        get {
          handleWithGeneric(inquestRepository.all()) { inquests =>
            complete(inquests)
          }
        } ~ post {
          entity(as[CreateInquest]) { createInquest =>
            validateWith(CreateInquestValidator)(createInquest) {
              handleWithGeneric(inquestRepository.create(createInquest)) { inquest =>
                complete(inquest)
              }
            }
          }
        }
      } ~ path(Segment) { id: String =>
        put {
          entity(as[UpdateInquest]) { updateInquest =>
            validateWith(UpdateInquestValidator)(updateInquest) {
              handle(inquestRepository.update(id, updateInquest)) {
                case InquestRepository.InquestNotFound(_) =>
                  ApiError.inquestNotFound(id)
                case _ =>
                  ApiError.generic
              } { inquest =>
                complete(inquest)
              }
            }
          }
        }
      }
    }
  }

}
