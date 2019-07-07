package service.directives

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.{Directive1, Directives, Rejection}
import com.google.firebase.auth.{FirebaseAuth, FirebaseAuthException}

import service.models.{ApiError, User}

trait AuthenticationDirectives extends Directives {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  // TODO: use async Firebase call.
  def getUserFromToken(idToken: String): User = {
    val decodedToken = FirebaseAuth.getInstance.verifyIdToken(idToken)
    val uid = decodedToken.getUid
    User(uid)
  }

  private def extractUser(idToken: String): Directive1[User] = {
    try {
      val user = getUserFromToken(idToken)
      provide(user)
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

  private def handleUnauthorized(rejections: Seq[Rejection]): Directive1[String] = {
    val apiError = ApiError.unauthorized
    complete(apiError.statusCode, apiError.message)
  }

  private def extractBearerToken: HttpHeader => Option[String] = {
    case HttpHeader("authorization", token) if token.startsWith(s"Bearer ") =>
      Some(token.stripPrefix(s"Bearer "))
    case _ =>
      None
  }

  def authenticateUser: Directive1[User] = {
    headerValue(extractBearerToken)
      .recover(handleUnauthorized)
      .flatMap(extractUser)
  }

  def authenticateAdmin: Directive1[User] = {
    authenticateUser.flatMap { user =>
      if (user.isAdmin) provide(user)
      else {
        val apiError = ApiError.adminPrivilegeRequired
        complete(apiError.statusCode, apiError.message)
      }
    }
  }
}
