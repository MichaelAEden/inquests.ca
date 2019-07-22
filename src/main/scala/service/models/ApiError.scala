package service.models

import akka.http.scaladsl.model.{StatusCode, StatusCodes}

final case class ApiError private (statusCode: StatusCode, message: String)

object ApiError {

  private def apply(statusCode: StatusCode, message: String): ApiError = new ApiError(statusCode, message)

  // 400
  val userNotFound: ApiError = new ApiError(StatusCodes.NotFound, s"User not found.")
  val inquestNotFound: ApiError = new ApiError(StatusCodes.NotFound, s"Inquest not found.")

  val emptyUserName: ApiError = new ApiError(StatusCodes.BadRequest, s"User name cannot be empty.")
  val emptyInquestTitle: ApiError = new ApiError(StatusCodes.BadRequest, s"Inquest title cannot be empty.")

  // 500
  val generic: ApiError = new ApiError(StatusCodes.InternalServerError, "Unknown error.")

}
