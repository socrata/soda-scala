package com.socrata.soda2.exceptions.soda1

import com.socrata.soda2.SodaException

/** The root of the hierarchy representing legacy SODA1 exceptions.
 *
 * At the time of this writing, the SODA2 reference implementation still returns SODA1-format
 * error codes.  These will be phased out, but until that happens this exception and its subclasses
 * represent those codes.
 */
class Soda1Exception(val resource: String, val code: String, message: Option[String]) extends SodaException(message.map(resource + ": " + _).getOrElse(resource))

class Soda1InvalidRequestException(resource: String, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1ForbiddenException(resource: String, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1NotFoundException(resource: String, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1MethodNotAllowedException(resource: String, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1UnacceptableResponseTypeException(resource: String, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1ConflictException(resource: String, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1InternalServerErrorException(resource: String, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1BadGatewayException(resource: String, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1UnknownException(val status: Int, resource: String, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)

object Soda1Exception {
  def apply(resource: String, status: Int, code: String, message:Option[String]): Soda1Exception = status match {
    case 400 => throw new Soda1InvalidRequestException(resource, code, message)
    case 403 => throw new Soda1ForbiddenException(resource, code, message)
    case 404 => throw new Soda1NotFoundException(resource, code, message)
    case 405 => throw new Soda1MethodNotAllowedException(resource, code, message)
    case 406 => throw new Soda1UnacceptableResponseTypeException(resource, code, message)
    case 409 => throw new Soda1ConflictException(resource, code, message)
    case 500 => throw new Soda1InternalServerErrorException(resource, code, message)
    case 502  => throw new Soda1BadGatewayException(resource, code, message)
    case other => throw new Soda1UnknownException(other, resource, code, message)
  }
}
