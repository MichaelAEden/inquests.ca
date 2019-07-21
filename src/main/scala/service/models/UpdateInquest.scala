package service.models

import db.models.Inquest
import service.directives.Validator

case class UpdateInquest(title: Option[String], description: Option[String]) {

  def toInquest(inquest: Inquest): Inquest =
    inquest.copy(
      title = title.getOrElse(inquest.title),
      description = description.getOrElse(inquest.description)
    )

}

object UpdateInquestValidator extends Validator[UpdateInquest] {

  def validate(updateInquest: UpdateInquest): Option[ApiError] = {
    if (updateInquest.title.exists(_.isEmpty))
      Some(ApiError.invalidInquestTitle(""))
    else
      None
  }

}
