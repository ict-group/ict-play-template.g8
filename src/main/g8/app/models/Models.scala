package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class BaseElement(id: Option[String], code: Option[String], description: Option[String])

object BaseElement {
  import play.api.libs.json.Json

  implicit val baseElement = Json.format[BaseElement]
}

case class QueryFilter(column: Option[String], value: Option[String], relation: Option[String], kind: Option[String])

object QueryFilter {
  import play.api.libs.json.Json

  implicit val queryFilter = Json.format[QueryFilter]
}

case class LoginForm(username: String, password: String)

object LoginForm {
  import play.api.libs.json.Json

  implicit val userFormat = Json.format[LoginForm]
}
