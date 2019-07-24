package service.models

import db.models.{Role, User}
import service.directives.Validator

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
      role = role.getOrElse(user.role)
    )

}

object UpdateUserValidator extends Validator[UpdateUser] {

  def validate(updateUser: UpdateUser): Option[ApiError] = {
    if (updateUser.name.exists(_.isEmpty))
      Some(ApiError.emptyUserName)
    else if (updateUser.role.exists(role => !Role.isRoleValid(role)))
      Some(ApiError.invalidUserRole)
    else
      None
  }

}
