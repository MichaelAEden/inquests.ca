package db.models

case class User(
  id: Option[Int],
  firebaseUid: String,
  email: String,
  name: String,
  jurisdiction: String,
  role: String
)
