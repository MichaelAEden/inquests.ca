package service.models

import db.models.Inquest

case class CreateInquest(title: String, description: String) {

  def toInquest(): Inquest =
    Inquest(None, title, description)

}
