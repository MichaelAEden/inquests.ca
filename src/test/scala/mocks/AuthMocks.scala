package mocks

import org.scalamock.scalatest.MockFactory

import clients.firebase.{FirebaseClient, FirebaseUser}
import db.models.User
import db.spec.UserRepository

import scala.concurrent.Future

trait AuthMocks extends MockFactory {

  def mockFirebaseUserAuthentication(token: String, maybeFirebaseUser: Option[FirebaseUser]): FirebaseClient = {
    val mockFirebaseClient = mock[FirebaseClient]

    (mockFirebaseClient.getFirebaseUserFromToken _)
      .expects(token)
      .returns(Future.successful(maybeFirebaseUser))

    mockFirebaseClient
  }

  def mockUserAuthentication(token: String, maybeUser: Option[User]): (FirebaseClient, UserRepository) = {
    val mockFirebaseClient = mock[FirebaseClient]
    val mockUserRepository = mock[UserRepository]

    val maybeFirebaseUser = maybeUser.map { user =>
      FirebaseUser(user.firebaseUid, user.email)
    }

    (mockFirebaseClient.getFirebaseUserFromToken _)
      .expects(token)
      .returns(Future.successful(maybeFirebaseUser))

    maybeUser.foreach { user =>
      (mockUserRepository.byFirebaseUid _)
        .expects(user.firebaseUid)
        .returns(Future.successful(user))
    }

    (mockFirebaseClient, mockUserRepository)
  }

}
