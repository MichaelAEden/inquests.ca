package db.models

case class UpdateInquest(title: Option[String], description: Option[String]) {

  def toInquest(inquest: Inquest): Inquest =
    inquest.copy(
      title = title.getOrElse(inquest.title),
      description = description.getOrElse(inquest.description)
    )

}
