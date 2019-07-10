package service.router

import akka.http.scaladsl.server.Route

import clients.firebase.FirebaseClient
import db.models.{CreateInquest, UpdateInquest}
import db.spec.InquestRepository
import service.directives._
import service.models.ApiError

class InquestRouter(inquestRepository: InquestRepository, fbClient: FirebaseClient)
  extends Router with AuthDirectives with HandlerDirectives with ValidatorDirectives {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private implicit val firebaseClient: FirebaseClient = fbClient

  // TODO: sanitize input.
  override def route: Route = pathPrefix("api") {
    pathPrefix("inquests") {
      pathEndOrSingleSlash {
        get {
          handleWithGeneric(inquestRepository.all()) { inquests =>
            complete(inquests)
          }
        } ~ post {
          authorizeAdmin("access to create inquest") apply { _ =>
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
         authorizeAdmin("access to update inquest") apply { _ =>
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
