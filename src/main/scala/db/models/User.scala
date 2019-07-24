package db.models

import java.sql.Timestamp

// TODO: jurisdictionId field should be nullable for users who choose not to
// TODO: (cont) provide it.
case class User(
  id: Option[Int],
  firebaseUid: String,
  email: String,
  name: String,
  jurisdictionId: String,
  role: Role,
  created: Option[Timestamp]
) {

  def canPerformAction(action: Action): Boolean = role.actions.contains(action)

}
