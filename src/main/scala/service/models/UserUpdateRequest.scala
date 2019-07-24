package service.models

import db.models.{Role, User}
import service.directives.Validator

import scala.util.Try

// Currently only used by admins.
// Note updates to email should be done through Firebase.
case class UpdateUser(
  name: Option[String],
  jurisdictionId: Option[String],
  role: Option[String]
) {

  def toUser(user: User): User =
    user.copy(
      name = name.getOrElse(user.name),
      jurisdictionId = jurisdictionId.getOrElse(user.jurisdictionId),
      role = role.map(Role.getRole).getOrElse(user.role)
    )

}

object UpdateUserValidator extends Validator[UpdateUser] {

  def validate(updateUser: UpdateUser): Option[ApiError] = {
    if (updateUser.name.exists(_.isEmpty))
      Some(ApiError.emptyUserName)
    else if (updateUser.role.exists(role => Try(Role.getRole(role)).isFailure))
      Some(ApiError.invalidUserRole)
    else
      None
  }

}
