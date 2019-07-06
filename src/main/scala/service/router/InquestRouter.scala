package service.router

import akka.http.scaladsl.server.Route

import db.models.{CreateInquest, UpdateInquest}
import db.spec.InquestRepository
import service.directives._
import service.models.ApiError

class InquestRouter(inquestRepository: InquestRepository)
  extends Router with AuthenticationDirectives with HandlerDirectives with ValidatorDirectives {

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
          authenticateAdmin { _ =>
            entity(as[CreateInquest]) { createInquest =>
              validateWith(CreateInquestValidator)(createInquest) {
                handleWithGeneric(inquestRepository.create(createInquest)) { inquest =>
                  complete(inquest)
                }
              }
            }
          }
        }
      } ~ path(IntNumber) { id: Int =>
        get {
          handleWithGeneric(inquestRepository.byId(id)) { inquest =>
            complete(inquest)
          }
        } ~ put {
         authenticateAdmin { _ =>
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

}
