package db.models

case class CreateInquest(title: String, description: String) {

  def toInquest(): Inquest =
    Inquest(None, title, description)

}
