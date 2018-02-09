package controllers.schema

import play.api.libs.json.Json
import slick.jdbc.GetResult
/**
  * Created by Riccardo Merolla on 16/05/17.
  */

case class CodeDescription(code: Option[String], description: Option[String])

case class SchemaColumn(number: Long, name: String, attnum: String, notnull: Boolean, columntype: String, primarykey: String, uniquekey: String, foreignkey: Option[String])

case class SchemaKey(title: String, `type`: String, check: Boolean, format: Option[String], entity_id: Option[String], mapping: Option[CodeDescription])

case class ColumnDef(field: String, name: String)

case class Schema(title: String, `type`: String, properties: Map[String, SchemaKey], required: Seq[String])

case class FormField(key: Seq[String], `type`: String, title: String, required: Boolean)

case class SchemaForm(code: String, description: String, columnDefs: Seq[ColumnDef], schema: Schema, form: Seq[FormField])

object SchemaForm {

  implicit val codeDescriptionFormat = Json.format[CodeDescription]
  implicit val schemaKeyFormat = Json.format[SchemaKey]
  implicit val columnDefFormat = Json.format[ColumnDef]
  implicit val schemaFormat = Json.format[Schema]
  implicit val formFieldFormat = Json.format[FormField]
  implicit val schemaFormFormat = Json.format[SchemaForm]

}

object SchemaColumn {
  implicit val getSchemaColumn = GetResult(r => SchemaColumn(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.nextStringOption()))

  implicit val schemaColumnFormat = Json.format[SchemaColumn]
}