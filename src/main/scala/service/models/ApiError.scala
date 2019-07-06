package service.models

import akka.http.scaladsl.model.{StatusCode, StatusCodes}

final case class ApiError private (statusCode: StatusCode, message: String)

object ApiError {

  private def apply(statusCode: StatusCode, message: String): ApiError = new ApiError(statusCode, message)

  val generic: ApiError = new ApiError(StatusCodes.InternalServerError, "Unknown error.")
  val unauthorized: ApiError = new ApiError(StatusCodes.Unauthorized, "Authentication was not provided.")
  val authenticationFailure: ApiError = new ApiError(StatusCodes.InternalServerError, "Failed to authenticate user.")

  def invalidInquestTitle(title: String): ApiError =
    new ApiError(StatusCodes.BadRequest, s"Invalid inquest title: '$title'.")

  def inquestNotFound(id: Int): ApiError =
    new ApiError(StatusCodes.NotFound, s"Inquest with id: '$id' not found.")

}
