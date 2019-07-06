package service.directives

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.{Directive1, Directives}
import com.google.firebase.auth.{FirebaseAuth, FirebaseAuthException, UserRecord}

import service.models.ApiError

trait AuthenticationDirectives extends Directives {

  def authenticateUser: Directive1[UserRecord] = {

    def extractBearerToken: HttpHeader => Option[String] = {
      case HttpHeader("Authorization", token) if token.startsWith(s"Bearer ") =>
        Some(token.stripPrefix(s"Bearer "))
      case _ =>
        None
    }

    def extractUserRecord(idToken: String): Directive1[UserRecord] = {
      try {
        val decodedToken = FirebaseAuth.getInstance.verifyIdToken(idToken)
        val uid = decodedToken.getUid
        val userRecord = FirebaseAuth.getInstance.getUser(uid)
        provide(userRecord)
      } catch {
        // TODO: log error.
        case _: FirebaseAuthException =>
          val apiError = ApiError.authenticationFailure
          complete(apiError.statusCode, apiError.message)
        case _: Throwable =>
          val apiError = ApiError.generic
          complete(apiError.statusCode, apiError.message)
      }
    }

    headerValue(extractBearerToken).flatMap(extractUserRecord)
  }
}
