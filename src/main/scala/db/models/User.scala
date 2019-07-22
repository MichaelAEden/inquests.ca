package db.models

case class User(
  id: Option[Int],
  firebaseUid: String,
  email: String,
  name: String,
  jurisdictionId: String,
  role: Role
) {

  def canPerformAction(action: Action): Boolean = role.actions.contains(action)

}
