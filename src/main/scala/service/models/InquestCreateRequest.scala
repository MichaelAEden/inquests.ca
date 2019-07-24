package service.models

import db.models.Inquest
import service.directives.Validator

case class InquestCreateRequest(title: String, description: String) {

  def toInquest: Inquest =
    Inquest(None, title, description)

}

object InquestCreateRequestValidator extends Validator[InquestCreateRequest] {

  def validate(createInquest: InquestCreateRequest): Option[ApiError] = {
    if (createInquest.title.isEmpty)
      Some(ApiError.emptyInquestTitle)
    else
      None
  }

}
