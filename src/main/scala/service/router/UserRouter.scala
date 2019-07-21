package service.router

import akka.http.scaladsl.server.Route

import clients.firebase.FirebaseClient
import db.spec.UserRepository
import service.directives._
import service.models.{CreateUser, CreateUserValidator}

class UserRouter(userRepository: UserRepository, fbClient: FirebaseClient)
  extends Router with AuthDirectives with HandlerDirectives with ValidatorDirectives {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private implicit val firebaseClient: FirebaseClient = fbClient

  override def route: Route = pathPrefix("api") {
    // TODO: should user be singular or plural according to REST convention?
    pathPrefix("users") {
      pathEndOrSingleSlash {
        post {
          authenticateFirebaseUser("access to create user") apply { firebaseUser =>
            entity(as[CreateUser]) { createUser =>
              validateWith(CreateUserValidator)(createUser) {
                handleWithGeneric(userRepository.create(createUser, firebaseUser)) { user =>
                  complete(user)
                }
              }
            }
          }
        }
      }
    }
  }

}