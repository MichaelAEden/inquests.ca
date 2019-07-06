package service.directives

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.{Directive1, Directives, Rejection}
import com.google.firebase.auth.{FirebaseAuth, FirebaseAuthException, UserRecord}

import service.models.ApiError

import scala.collection.JavaConverters._

trait AuthenticationDirectives extends Directives {

  private def extractUserRecord(idToken: String): Directive1[UserRecord] = {
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

  private def handleUnauthorized(rejections: Seq[Rejection]): Directive1[String] = {
    val apiError = ApiError.unauthorized
    complete(apiError.statusCode, apiError.message)
  }

  def authenticateUser: Directive1[UserRecord] = {

    def extractBearerToken: HttpHeader => Option[String] = {
      case HttpHeader("authorization", token) if token.startsWith(s"Bearer ") =>
        Some(token.stripPrefix(s"Bearer "))
      case _ =>
        None
    }

    headerValue(extractBearerToken)
      .recover(handleUnauthorized)
      .flatMap(extractUserRecord)
  }

  def authenticateAdmin: Directive1[UserRecord] = {
    authenticateUser.flatMap { userRecord =>
      userRecord.getCustomClaims.asScala.get("admin") match {
        case Some(_) =>
          provide(userRecord)
        case None =>
          val apiError = ApiError.adminPrivilegeRequired
          complete(apiError.statusCode, apiError.message)
      }
    }
  }
}
