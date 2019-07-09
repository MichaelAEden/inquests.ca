package service.directives

import akka.http.scaladsl.server.{Directive1, Directives}
import akka.http.scaladsl.server.directives.Credentials

import clients.firebase.{FirebaseClient, FirebaseUser}

import scala.concurrent.Future

trait AuthDirectives extends Directives {

  private def authenticate(credentials: Credentials)(implicit firebaseClient: FirebaseClient): Future[Option[FirebaseUser]] = {
    credentials match {
      case Credentials.Provided(idToken) =>
        firebaseClient.getUserFromToken(idToken)
      case _ =>
        Future.successful(None)
    }
  }

  def authenticateUser(realm: String)(implicit firebaseClient: FirebaseClient): Directive1[FirebaseUser] = {
    authenticateOAuth2Async(realm, authenticate)
  }

  def authorizeAdmin(realm: String)(implicit firebaseClient: FirebaseClient): Directive1[FirebaseUser] = {
    authenticateUser(realm) flatMap { user =>
      authorizeAsync(_ => firebaseClient.isAdmin(user)).tmap(_ => user)
    }
  }
}
