package service.models

import db.models.Authority

case class AuthorityUpdateRequest(title: Option[String], description: Option[String]) {

  def toAuthority(authority: Authority): Authority =
    authority.copy(
      title = title.getOrElse(authority.title),
      description = description.getOrElse(authority.description)
    )

}

object AuthorityUpdateRequestValidator extends Validator[AuthorityUpdateRequest] {

  def validate(updateAuthority: AuthorityUpdateRequest): Option[ApiError] = {
    if (updateAuthority.title.exists(_.isEmpty))
      Some(ApiError.invalidAuthorityTitle(""))
    else
      None
  }

}
