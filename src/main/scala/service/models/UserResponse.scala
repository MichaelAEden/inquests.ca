package service.models

import db.models.User

// The circe library cannot marshal the java.sql.Timestamp type.
// TODO: determine way to marshal java.sql.Timestamp type so it can be included
// TODO: (cont) in this model.
case class UserResponse(
  id: Option[Int],
  firebaseUid: String,
  email: String,
  name: String,
  jurisdictionId: String,
  role: String
)

object UserResponse {

  def fromUser(user: User): UserResponse = {
    UserResponse(
      user.id,
      user.firebaseUid,
      user.email,
      user.name,
      user.jurisdictionId,
      user.role
    )
  }

}
