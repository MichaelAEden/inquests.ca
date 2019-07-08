package service.directives

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.{Directive1, Directives, Rejection}
import com.google.firebase.auth.{FirebaseAuth, FirebaseAuthException}

import service.models.{ApiError, User}
import utils.FutureConverters.ApiFutureConverter

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

trait AuthenticationDirectives extends Directives {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  implicit val contextExecutor: ExecutionContextExecutor

  protected def getUserFromToken(idToken: String): Future[User] = {
    FirebaseAuth
      .getInstance
      .verifyIdTokenAsync(idToken)
      .asScala
      .map(decodedToken => User(decodedToken.getUid))
  }

  private def extractUser(idToken: String): Directive1[User] = {
    val eventualUser = getUserFromToken(idToken)

    onComplete(eventualUser) flatMap {
      case Success(user) =>
        provide(user)
      case Failure(_: FirebaseAuthException) =>
        val apiError = ApiError.authenticationFailure
        complete(apiError.statusCode, apiError.message)
      case Failure(_: Throwable) =>
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
