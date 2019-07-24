package service.models

import db.models.Inquest
import service.directives.Validator

case class InquestUpdateRequest(title: Option[String], description: Option[String]) {

  def toInquest(inquest: Inquest): Inquest =
    inquest.copy(
      title = title.getOrElse(inquest.title),
      description = description.getOrElse(inquest.description)
    )

}

object InquestUpdateRequestValidator extends Validator[InquestUpdateRequest] {

  def validate(updateInquest: InquestUpdateRequest): Option[ApiError] = {
    if (updateInquest.title.exists(_.isEmpty))
      Some(ApiError.emptyInquestTitle)
    else
      None
  }

}
