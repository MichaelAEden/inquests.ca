package clients.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.{FirebaseApp, FirebaseOptions}
import com.google.firebase.auth.{FirebaseAuth, UserRecord}

import utils.FutureConverters.ApiFutureConverter

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.control.NonFatal

trait FirebaseClient {

  def getFirebaseUserFromToken(idToken: String): Future[Option[FirebaseUser]]
  def isAdmin(user: FirebaseUser): Future[Boolean]

}

object FirebaseClient {

  private lazy val firebaseApp: FirebaseApp = {
    val firebaseOptions: FirebaseOptions = new FirebaseOptions.Builder()
      .setCredentials(GoogleCredentials.getApplicationDefault)
      .build()
    FirebaseApp.initializeApp(firebaseOptions)
  }

  def apply(implicit ece: ExecutionContextExecutor): FirebaseClient = {
    firebaseApp
    new ScalaFirebaseClient()
  }

}

private class ScalaFirebaseClient(implicit ece: ExecutionContextExecutor) extends FirebaseClient {

  override def getFirebaseUserFromToken(idToken: String): Future[Option[FirebaseUser]] = {
    FirebaseAuth
      .getInstance
      .verifyIdTokenAsync(idToken)
      .asScala
      .map(decodedToken => Some(FirebaseUser(decodedToken.getUid)))
      .recover {
        // TODO: log exception.
        case NonFatal(_) => None
      }
  }

  override def isAdmin(user: FirebaseUser): Future[Boolean] = {
    getUserRecord(user)
      .map { userRecord =>
        userRecord
          .getCustomClaims
          .asScala
          .contains("admin")
      }
      .recover {
        // TODO: log exception.
        case NonFatal(_) => false
      }
  }

  private def getUserRecord(user: FirebaseUser): Future[UserRecord] = {
    FirebaseAuth
      .getInstance
      .getUserAsync(user.uid)
      .asScala
  }

}
