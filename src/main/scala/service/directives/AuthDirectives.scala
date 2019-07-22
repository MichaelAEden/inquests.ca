package service.directives

import akka.http.scaladsl.server.{Directive1, Directives}
import akka.http.scaladsl.server.directives.Credentials

import clients.firebase.{FirebaseClient, FirebaseUser}
import db.models.{User, Action}
import db.spec.UserRepository
import service.models.ApiError

import scala.concurrent.Future
import scala.util.Success

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

  def authorizeAction(action: Action)
                     (implicit firebaseClient: FirebaseClient, userRepository: UserRepository): Directive1[User] = {
    // TODO: log errors.
    authenticateFirebaseUser(action.realm) flatMap { firebaseUser =>
      val eventualUser = userRepository.byEmail(firebaseUser.email)
      onComplete(eventualUser) flatMap {
        case Success(Some(user)) =>
          authorize(_ => user.canPerformAction(action)) tmap { _ => user }
        case _ =>
          val apiError = ApiError.generic
          complete(apiError.statusCode, apiError.message)
      }
    }
  }

}
