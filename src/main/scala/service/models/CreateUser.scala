package service.models

import clients.firebase.FirebaseUser
import db.models.User

case class CreateUser(
  name: String,
  jurisdiction: String
) {

  def toUser(firebaseUser: FirebaseUser): User =
    User(
      None,
      firebaseUser.uid,
      firebaseUser.email,
      name,
      jurisdiction,
      User.UserRole
    )

}
