package db.models

case class User(
  id: Option[Int],
  firebaseUid: String,
  name: String,
  email: String,
  jurisdiction: String,
  role: String
)
