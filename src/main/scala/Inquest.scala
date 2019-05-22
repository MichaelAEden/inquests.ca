case class Inquest(id: String, title: String, description: String)
case class CreateInquest(title: String, description: String)
case class UpdateInquest(title: Option[String], description: Option[String])
