package service.models

import clients.firebase.FirebaseUser
import db.models.{User, Role}
import service.directives.Validator

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
      Role.User,
      None
    )

}

object CreateUserValidator extends Validator[CreateUser] {

  def validate(createUser: CreateUser): Option[ApiError] = {
    // Note the jurisdiction will be validated upon insertion by DB constraints.
    if (createUser.name.isEmpty)
      Some(ApiError.emptyUserName)
    else
      None
  }

}
