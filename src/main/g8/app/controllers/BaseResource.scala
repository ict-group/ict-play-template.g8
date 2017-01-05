package controllers

import models.QueryFilter
import play.api.{Configuration, Logger}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json._
import play.api.libs.json._
import play.api.mvc.{Action, Controller, Result}
import scalikejdbc._
import services.JsonUtil

import scala.concurrent.Future

/**
  * Created by Riccardo Merolla on 26/08/16.
  */
abstract class BaseResource(configuration: Configuration) extends Controller {

  implicit val session = AutoSession

  def list(entity: String, res_mode: Option[String], page: Int, page_size: Int, sort_fields: Option[String], sort_order: Option[String],
           query: Option[String], code: Option[String], description: Option[String]) = Action.async {
    val whereQuery = if (query.isEmpty) None else Some(s"WHERE LOWER(${code.get}) LIKE LOWER('%${query.get}%')" + description.get.split(",").foldLeft(" ")((condition, field) => condition + s"OR LOWER(${field}) LIKE LOWER('%${query.get}%') "))
    collection(entity, res_mode, page, page_size, sort_fields, sort_order, query, code, description, whereQuery)
  }

  def _lookup(entity: String, res_mode: Option[String], page: Int, page_size: Int, sort_fields: Option[String], sort_order: Option[String], query: Option[String]) = Action.async {
    collection(entity, res_mode, page, page_size, sort_fields, sort_order, query, None, None, None)
  }

  def _filter(entity: String, res_mode: Option[String], page: Int, page_size: Int, sort_fields: Option[String], sort_order: Option[String],
              query: Option[String], code: Option[String], description: Option[String]) =
    Action.async(parse.json) { request =>
      request.body.validate[List[QueryFilter]].map { model =>
        val whereGetQuery = if (query.isEmpty) "" else s"AND ( LOWER(${code.get}) LIKE LOWER('%${query.get}%')" + description.get.split(",").foldLeft(" ")((condition, field) => condition + s"OR LOWER(${field}) LIKE LOWER('%${query.get}%')") + ")"
        val whereQuery = if (model.isEmpty) whereGetQuery.replace("AND (", "WHERE (") else "WHERE " + whereCondition(model.head) + " " + whereGetQuery + model.tail.foldLeft("") {
          (where, filter) => where + " AND " + whereCondition(filter)
        }
        Logger.debug(s"$whereQuery")
        collection(entity, res_mode, page, page_size, sort_fields, sort_order, query, code, description, Some(whereQuery))
      }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def whereCondition(queryFilter: QueryFilter): String = {
    (queryFilter.kind, queryFilter.relation) match {
      case (Some("string"), Some("LIKE")) => s"LOWER(${queryFilter.column.get}) LIKE LOWER('%${queryFilter.value.get}%')"
      case (Some("string"), _) => s"${queryFilter.column.get} = '${queryFilter.value.get}'"
      case (Some("number"), _) => s"${queryFilter.column.get} = ${queryFilter.value.get}"
      case (Some("boolean"), _) => s"${queryFilter.column.get} IS ${queryFilter.value.get}"
      case (Some("date"), _) => s"${queryFilter.column.get} ${queryFilter.relation.getOrElse("=")} TIMESTAMP '${queryFilter.value.get}'"
      case (Some("nullable"), _) => s"${queryFilter.column.get} ${queryFilter.relation.getOrElse("IS NULL")}"
      case (Some("custom"), _) => s"${queryFilter.value.getOrElse("")}"
      case _ => ""
    }
  }

  def _count = TODO

  def _id(entity:String, id: Long) = Action {
    val tableName = SQLSyntax.createUnsafely(s"$entity")
    val entities: Option[Map[String, Any]] = DB readOnly { implicit session =>
     sql"SELECT t.* FROM ${tableName} t WHERE id = ${id}".map(_.toMap).single().apply()
    }
    Logger.debug(s"$entities")
    Ok(JsonUtil.toJson(entities)).as(JSON)
  }

  def _uuid(entity: String, uuid: String) = _id(entity, uuid.toLong)

  def _code(entity: String, code: String) = _id(entity, code.toLong)

  def _search = TODO

  def save(entity: String) = Action.async(parse.json) { request =>
    val tableName = SQLSyntax.createUnsafely(s"$entity")
    val payload = request.body.as[JsObject]
    val keys = payload.keys.filter(item => item != "id")
    val keysFiltered = keys.filter(key => (payload \ key).get match {
      case text: JsString => true
      case number: JsNumber => true
      case _ => false
    })
    val keySQL = SQLSyntax.createUnsafely(keysFiltered.head + keysFiltered.tail.foldLeft("") {(acc, item) => acc + s", $item"})
    val valueSQL = SQLSyntax.createUnsafely(valueQuery((payload \ keysFiltered.head).get) + keysFiltered.tail.foldLeft("") {(acc, item) => acc + s", ${valueQuery((payload \ item).get)}"})
    val id = sql"INSERT INTO ${tableName} (id, ${keySQL}) VALUES (nextval('hibernate_sequence'), ${valueSQL})".updateAndReturnGeneratedKey.apply()
    Future(Ok(obj("id" -> id ,"query" -> s"insert into ${tableName.value} (${keySQL.value}) VALUES (${valueSQL.value})")))
  }

  def valueQuery(jsValue: JsValue): String = {
    jsValue match {
      case text: JsString => s"'${text.value.replaceAll("'", "''")}'"
      case number: JsNumber => s"${number.value}"
      case _ => ""
    }
  }

  def update(entity: String, id: Long) = Action.async(parse.json) { request =>
    val tableName = SQLSyntax.createUnsafely(s"$entity")
    val payload = request.body.as[JsObject]
    val keys = payload.keys
    val keysFiltered = keys.filter(key => (payload \ key).get match {
      case text: JsString => true
      case number: JsNumber => true
      case _ => false
    })
    Logger.info(s"$keysFiltered")
    val valueSQL = SQLSyntax.createUnsafely(s"${keysFiltered.head} = " + valueQuery((payload \ keysFiltered.head).get) + keysFiltered.tail.foldLeft("") {(acc, item) => acc + s", ${item} = ${valueQuery((payload \ item).get)}"})
    val query = sql"UPDATE ${tableName} SET ${valueSQL} WHERE id = ${id}"
    query.update.apply()
    Future(Ok(obj("query" -> s"${query.statement} --> ${id}")))
  }

  def delete(entity: String, id: Long) = Action { request =>
    val tableName = SQLSyntax.createUnsafely(s"$entity")
    val result = sql"DELETE FROM ${tableName} WHERE id = ${id}".update().apply()
    Ok(obj("result" -> result))
  }

  def id = TODO

  private def one(entity: String, key: String, value: String): Future[Result] = {
    val futureSchema: Future[Option[JsObject]] = ???

    futureSchema.map { schema => Ok(toJson(schema)) }
  }

  private def collection(entity: String, res_mode: Option[String], page: Int, page_size: Int, sort_fields: Option[String], sort_order: Option[String],
                         query: Option[String], code: Option[String], description: Option[String], whereQuery: Option[String]): Future[Result] = {
    val offset: Int = if (page == 1) 0 else (page - 1) * page_size
    val tableName = SQLSyntax.createUnsafely(s"$entity")
    val whereSQLSyntax = SQLSyntax.createUnsafely(whereQuery.getOrElse(""))
    val orderBy = SQLSyntax.createUnsafely(s"ORDER BY ${sort_fields.getOrElse("id")} ${sort_order.getOrElse("asc")}")
    val entities: List[Map[String, Any]] = DB readOnly { implicit session =>
      sql"SELECT t.* FROM ${tableName} t ${whereSQLSyntax} ${orderBy} LIMIT $page_size OFFSET $offset"
        .map(data => modelMap(data.toMap, code, description)).list().apply()
    }

    val count: Option[Int] = DB readOnly { implicit session =>
      sql"SELECT count(1) FROM ${tableName} t ${whereSQLSyntax}".map(_.intOpt("count")).single().apply().getOrElse(Some(0))
    }

    collectionResult(entities, res_mode, page, page_size, count)
  }

  protected def collectionResult(entities: List[Map[String, Any]], res_mode: Option[String], page: Int, page_size: Int, count: Option[Int]): Future[Result] = {
    res_mode match {
      case None => {
        Future(Ok(JsonUtil.toJson(Map(
          "list" -> entities, "totalResults" -> count, "currentPage" -> page, "pageSize" -> page_size
        ))).as(JSON))
      }
      case Some("list") => Future(Ok(JsonUtil.toJson(entities)).as(JSON))
      case _ => Future(BadRequest(obj("error" -> "unknown response mode")))
    }
  }

  protected def modelMap = (data: Map[String, Any], code: Option[String], description: Option[String]) => {
    data + ("model" ->
      Map("code" -> data.get(code.getOrElse("id")),
        "description" -> description.getOrElse("descrizione").split(",").foldLeft("")((desc, field) => desc + data.get(field).getOrElse("") + " ").trim()),
      "UUID" -> data.get("id")
      )
  }

}
