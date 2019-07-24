package service.router

import akka.http.scaladsl.server.Route

import clients.firebase.FirebaseClient
import db.spec.UserRepository
import service.directives._
import service.models._

class UserRouter(userRepository: UserRepository, firebaseClient: FirebaseClient)
  extends Router with AuthDirectives with HandlerDirectives with ValidatorDirectives {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private implicit val fbClient: FirebaseClient = firebaseClient

  override def route: Route = pathPrefix("api") {
    pathPrefix("users") {
      pathEndOrSingleSlash {
        post {
          authenticateFirebaseUser("access to create user") apply { firebaseUser =>
            entity(as[CreateUser]) { createUser =>
              validateWith(CreateUserValidator)(createUser) {
                handleWithGeneric(userRepository.create(createUser, firebaseUser)) { user =>
                  val userResponse = UserResponse.fromUser(user)
                  complete(userResponse)
                }
              }
            }
          }
        }
      }
    }
  }

}