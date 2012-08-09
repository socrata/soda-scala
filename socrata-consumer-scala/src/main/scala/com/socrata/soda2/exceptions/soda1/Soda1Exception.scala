package com.socrata.soda2.exceptions.soda1

import com.socrata.soda2.{Resource, SodaException}

/** The root of the hierarchy representing legacy SODA1 exceptions.
 *
 * At the time of this writing, the SODA2 reference implementation still returns SODA1-format
 * error codes.  These will be phased out, but until that happens this exception and its subclasses
 * represent those codes.
 */
abstract class Soda1Exception(val resource: Resource, val code: String, message: Option[String]) extends SodaException(message.map(resource + ": " + _).getOrElse(resource.toString))

class Soda1InvalidRequestException(resource: Resource, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1ForbiddenException(resource: Resource, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1NotFoundException(resource: Resource, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1MethodNotAllowedException(resource: Resource, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1UnacceptableResponseTypeException(resource: Resource, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1ConflictException(resource: Resource, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1InternalServerErrorException(resource: Resource, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1BadGatewayException(resource: Resource, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1GatewayTimeoutException(resource: Resource, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)
class Soda1UnknownException(val status: Int, resource: Resource, code: String, message: Option[String]) extends Soda1Exception(resource, code, message)

object Soda1Exception {
  def apply(resource: Resource, status: Int, code: String, message:Option[String]): Soda1Exception = status match {
    case 400 => throw new Soda1InvalidRequestException(resource, code, message)
    case 403 => throw new Soda1ForbiddenException(resource, code, message)
    case 404 => throw new Soda1NotFoundException(resource, code, message)
    case 405 => throw new Soda1MethodNotAllowedException(resource, code, message)
    case 406 => throw new Soda1UnacceptableResponseTypeException(resource, code, message)
    case 409 => throw new Soda1ConflictException(resource, code, message)
    case 500 => throw new Soda1InternalServerErrorException(resource, code, message)
    case 502  => throw new Soda1BadGatewayException(resource, code, message)
    case 504  => throw new Soda1GatewayTimeoutException(resource, code, message)
    case other => throw new Soda1UnknownException(other, resource, code, message)
  }
}
