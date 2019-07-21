package service.models

import db.models.Inquest
import service.directives.Validator

case class CreateInquest(title: String, description: String) {

  def toInquest: Inquest =
    Inquest(None, title, description)

}

object CreateInquestValidator extends Validator[CreateInquest] {

  def validate(createInquest: CreateInquest): Option[ApiError] = {
    if (createInquest.title.isEmpty)
      Some(ApiError.invalidInquestTitle(""))
    else
      None
  }

}
