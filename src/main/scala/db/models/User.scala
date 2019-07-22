package db.models

import java.sql.Timestamp

case class User(
  id: Option[Int],
  firebaseUid: String,
  email: String,
  name: String,
  jurisdictionId: String,
  role: Role,
  created: Timestamp
) {

  def canPerformAction(action: Action): Boolean = role.actions.contains(action)

}
