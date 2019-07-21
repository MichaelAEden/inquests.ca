package db.models

case class User(
  id: Option[Int],
  firebaseUid: String,
  email: String,
  name: String,
  jurisdiction: String,
  role: String
)

object User {

  type Role = String

  val UserRole: Role = "user"
  val EditorRole: Role = "editor"
  val AdminRole: Role = "admin"

}
