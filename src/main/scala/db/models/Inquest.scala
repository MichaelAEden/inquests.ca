package db.models

case class Inquest(id: Option[Int], title: String, description: String)
case class CreateInquest(title: String, description: String)
case class UpdateInquest(title: Option[String], description: Option[String])
