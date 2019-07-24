package service.models

import clients.firebase.FirebaseUser
import db.models.{User, Role}
import service.directives.Validator

case class UserCreateRequest(
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

object UserCreateRequestValidator extends Validator[UserCreateRequest] {

  def validate(createUser: UserCreateRequest): Option[ApiError] = {
    // Note the jurisdiction will be validated upon insertion by DB constraints.
    if (createUser.name.isEmpty)
      Some(ApiError.emptyUserName)
    else
      None
  }

}
