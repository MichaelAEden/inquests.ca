package service.directives

import akka.http.scaladsl.server.{Directive1, Directives}
import akka.http.scaladsl.server.directives.Credentials

import clients.firebase.{FirebaseClient, FirebaseUser}
import db.models.{Action, User}
import db.spec.UserRepository
import service.models.ApiError

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait AuthDirectives extends Directives {

  private def authenticate(credentials: Credentials)
                          (implicit firebaseClient: FirebaseClient): Future[Option[FirebaseUser]] = {
    credentials match {
      case Credentials.Provided(idToken) =>
        firebaseClient.getFirebaseUserFromToken(idToken)
      case _ =>
        Future.successful(None)
    }
  }

  def authenticateFirebaseUser(realm: String)(implicit firebaseClient: FirebaseClient): Directive1[FirebaseUser] = {
    authenticateOAuth2Async(realm, authenticate)
  }

  def authenticateUser(realm: String)
                      (implicit firebaseClient: FirebaseClient, userRepository: UserRepository): Directive1[User] = {
    // TODO: log errors.
    // TODO: log case where Firebase email differs from User email.
    authenticateFirebaseUser(realm) flatMap { firebaseUser =>
      val eventualUser = userRepository.byFirebaseUid(firebaseUser.uid)
      onComplete(eventualUser) flatMap {
        case Success(user) =>
          provide(user)
        case Failure(_) =>
          val apiError = ApiError.generic
          complete(apiError.statusCode, apiError.message)
      }
    }
  }

  def authorizeAction(action: Action)
                     (implicit firebaseClient: FirebaseClient, userRepository: UserRepository): Directive1[User] = {
    authenticateUser(action.realm) flatMap { user =>
      authorize(_ => user.canPerformAction(action)) tmap { _ => user }
    }
  }

}
