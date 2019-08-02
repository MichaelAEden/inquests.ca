package service.router

import akka.http.scaladsl.server.Route

import clients.firebase.FirebaseClient
import db.spec.AuthorityRepository
import service.directives._
import service.models._

class AuthorityRouter(authorityRepository: AuthorityRepository, fbClient: FirebaseClient)
  extends Router with AuthDirectives with HandlerDirectives with ValidatorDirectives {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private implicit val firebaseClient: FirebaseClient = fbClient

  // TODO: sanitize input.
  override def route: Route = pathPrefix("api") {
    pathPrefix("authorities") {
      pathEndOrSingleSlash {
        get {
          handleWithGeneric(authorityRepository.all()) { authorities =>
            complete(authorities)
          }
        } ~ post {
          authorizeAdmin("access to create authority") apply { _ =>
            entity(as[AuthorityCreateRequest]) { createAuthority =>
              validateWith(AuthorityCreateRequestValidator)(createAuthority) {
                handleWithGeneric(authorityRepository.create(createAuthority)) { authority =>
                  complete(authority)
                }
              }
            }
          }
        }
      }
    }
  }

}
