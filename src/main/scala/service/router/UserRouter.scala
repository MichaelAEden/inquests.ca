package service.router

import akka.http.scaladsl.server.Route

import clients.firebase.FirebaseClient
import db.models.Action
import db.spec.UserRepository
import service.directives._
import service.models._

class UserRouter(userRepository: UserRepository, firebaseClient: FirebaseClient)
  extends Router with AuthDirectives with HandlerDirectives with ValidatorDirectives {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private implicit val userRepo: UserRepository = userRepository
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
        } ~ get {
          authorizeAction(Action.ManageUsers) apply { _ =>
            parameters("email") { email =>
              handle(userRepository.byEmail(email)) {
                case UserRepository.UserNotFound() =>
                  ApiError.userNotFound
                case _ =>
                  ApiError.generic
              } { user =>
                val userResponse = UserResponse.fromUser(user)
                complete(userResponse)
              }
            }
          }
        }
      } ~ path(IntNumber) { id: Int =>
        put {
          authorizeAction(Action.ManageUsers) apply { _ =>
            entity(as[UpdateUser]) { updateUser =>
              validateWith(UpdateUserValidator)(updateUser) {
                handle(userRepository.update(id, updateUser)) {
                  case UserRepository.UserNotFound() =>
                    ApiError.userNotFound
                  case _ =>
                    ApiError.generic
                } { user =>
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