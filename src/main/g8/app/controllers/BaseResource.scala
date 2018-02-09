package controllers

import models.QueryFilter
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.{Configuration, Logger}
import play.api.libs.json.Json._
import play.api.libs.json._
import play.api.mvc.{InjectedController, Result}
import services.JsonUtil
import slick.jdbc.{GetResult, JdbcProfile, PositionedResult}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Riccardo Merolla on 26/08/16.
  */
abstract class BaseResource(protected val dbConfigProvider: DatabaseConfigProvider, configuration: Configuration)
  extends InjectedController with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  def list(entity: String, res_mode: Option[String], page: Int, page_size: Int, sort_fields: Option[String], sort_order: Option[String],
           query: Option[String], code: Option[String], description: Option[String]) = Action.async {
    val whereQuery = if (query.isEmpty) None else Some(s"WHERE LOWER(${code.get}) LIKE LOWER('%${query.get.replaceAll("'","''")}%')" + description.get.split(",").foldLeft(" ")((condition, field) => condition + s"OR LOWER(${field}) LIKE LOWER('%${query.get.replaceAll("'","''")}%') "))
    collection(entity, res_mode, page, page_size, sort_fields, sort_order, query, code, description, whereQuery, None)
  }

  def _lookup(entity: String, res_mode: Option[String], page: Int, page_size: Int, sort_fields: Option[String], sort_order: Option[String], query: Option[String]) = Action.async {
    collection(entity, res_mode, page, page_size, sort_fields, sort_order, query, None, None, None, None)
  }

  def _filter(entity: String, res_mode: Option[String], page: Int, page_size: Int, sort_fields: Option[String], sort_order: Option[String],
              query: Option[String], code: Option[String], description: Option[String]) =
    Action.async(parse.json) { request =>
      request.body.validate[List[QueryFilter]].map { model =>
        val conditions = model.partition(qf => qf.relation.contains("JOIN"))
        val joins = if(conditions._1.nonEmpty) {
          conditions._1.foldLeft(("", "t.*")) {
            (result, join) =>
              (result._1 + s"LEFT JOIN ${join.column.get} ON ${join.value.get} ", result._2 + (if (join.kind.isEmpty) "" else s", ${join.kind.get}"))
          }
        } else ("", "t.*")
        val whereGetQuery = if (query.isEmpty) "" else s"AND ( LOWER(${code.get}) LIKE LOWER('%${query.get.replaceAll("'","''")}%')" + description.get.split(",").foldLeft(" ")((condition, field) => condition + s"OR LOWER(${field}) LIKE LOWER('%${query.get.replaceAll("'","''")}%')") + ")"
        val whereQuery = if (conditions._2.isEmpty) whereGetQuery.replace("AND (", "WHERE (") else "WHERE " + whereCondition(conditions._2.head) + " " + whereGetQuery + conditions._2.tail.foldLeft("") {
          (where, filter) => where + " AND " + whereCondition(filter)
        }
        Logger.trace(s"$joins$whereQuery")
        collection(entity, res_mode, page, page_size, sort_fields, sort_order, query, code, description, Some(joins._1.concat(whereQuery)), Some(joins._2))
      }.getOrElse(Future.successful(BadRequest("invalid json")))
    }

  def whereCondition(queryFilter: QueryFilter): String = {
    (queryFilter.kind, queryFilter.relation) match {
      case (Some("string"), Some("LIKE")) => s"LOWER(${queryFilter.column.get}) LIKE LOWER('%${queryFilter.value.get}%')"
      case (Some("string"), _) => s"${queryFilter.column.get} ${queryFilter.relation.getOrElse("=")} '${queryFilter.value.get}'"
      case (Some("number"), _) => s"${queryFilter.column.get} = ${queryFilter.value.get}"
      case (Some("boolean"), _) => s"${queryFilter.column.get} IS ${queryFilter.value.get}"
      case (Some("date"), _) => s"${queryFilter.column.get} ${queryFilter.relation.getOrElse("=")} TIMESTAMP '${queryFilter.value.get}'"
      case (Some("nullable"), _) => s"${queryFilter.column.get} ${queryFilter.relation.getOrElse("IS NULL")}"
      case (Some("custom"), _) => s"${queryFilter.value.getOrElse("")}"
      case _ => ""
    }
  }

  def _count = TODO

  implicit val getListStringResult = GetResult[List[Object]] (
    r => (1 to r.numColumns).map(_ => r.nextObject).toList
  )

  object ResultMap extends GetResult[Map[String,Any]] {
    def apply(pr: PositionedResult) = {
      val rs = pr.rs // <- jdbc result set
      val md = rs.getMetaData()
      val res = (1 to pr.numColumns).map{ i=> md.getColumnName(i) -> rs.getObject(i) }.toMap
      pr.nextRow // <- use Slick's advance method to avoid endless loop
      res
    }
  }

  protected implicit val getMap = GetResult[Map[String, Any]](r => {
    val metadata = r.rs.getMetaData
    (1 to r.numColumns).flatMap(i => {
      val columnName = metadata.getColumnName(i).toLowerCase
      val columnValue = r.nextObjectOption
      columnValue.map(columnName -> _)
    }).toMap
  })

  def _id(entity:String, id: Long) = Action.async {
    val query = sql"SELECT t.* FROM #${entity} t WHERE id = ${id}"
    db.run(query.as[Map[String, Any]]).map(
      x => {
        Logger.trace(s"$x")
        Ok(JsonUtil.toJson(x.toList.head)).as(JSON)
      }
    )
  }

  def _uuid(entity: String, uuid: String) = _id(entity, uuid.toLong)

  def _code(entity: String, code: String) = _id(entity, code.toLong)

  def _search = TODO

  def save(entity: String) = Action.async(parse.json) { request =>
    val payload = request.body.as[JsObject]
    val keys = payload.keys.filter(item => item != "id")
    val keysFiltered = keys.filter(key => (payload \ key).get match {
      case text: JsString => true
      case number: JsNumber => true
      case boolean: JsBoolean => true
      case _ => false
    })
    val keySQL = keysFiltered.head + keysFiltered.tail.foldLeft("") {(acc, item) => acc + s", $item"}
    val valueSQL = valueQuery((payload \ keysFiltered.head).get) + keysFiltered.tail.foldLeft("") {(acc, item) => acc + s", ${valueQuery((payload \ item).get)}"}
    db.run(sqlu"INSERT INTO #${entity} (id, #${keySQL}) VALUES (nextval('hibernate_sequence'), #${valueSQL})").map(
      id => Ok(obj("id" -> id ,"query" -> s"insert into ${entity} (${keySQL}) VALUES (${valueSQL})"))
    )
  }

  def valueQuery(jsValue: JsValue): String = {
    jsValue match {
      case text: JsString => s"'${text.value.replaceAll("'", "''")}'"
      case number: JsNumber => s"${number.value}"
      case boolean: JsBoolean => s"${boolean.value}"
      case JsNull => "null"
      case _ => ""
    }
  }

  def update(entity: String, id: Long) = Action.async(parse.json) { request =>
    val payload = request.body.as[JsObject]
    val keys = payload.keys
    val keysFiltered = keys.filter(key => (payload \ key).get match {
      case text: JsString => true
      case number: JsNumber => true
      case boolean: JsBoolean => true
      case JsNull => true
      case _ => false
    })
    Logger.info(s"$keysFiltered")
    val valueSQL = s"${keysFiltered.head} = " + valueQuery((payload \ keysFiltered.head).get) + keysFiltered.tail.foldLeft("") {(acc, item) => acc + s", ${item} = ${valueQuery((payload \ item).get)}"}
    val query = sqlu"UPDATE #${entity} SET #${valueSQL} WHERE id = ${id}"
    db.run(query).map(x => Ok(obj("updated" -> s"${x}")))
  }

  def delete(entity: String, id: Long) = Action.async {
    val query = sqlu"DELETE FROM #${entity} WHERE id = ${id}"
    db.run(query).map(
      x => Ok(obj("result" -> x))
    )
  }

  def id = TODO

  private def one(entity: String, key: String, value: String): Future[Result] = {
    val futureSchema: Future[Option[JsObject]] = ???

    futureSchema.map { schema => Ok(toJson(schema)) }
  }

  private def collection(entity: String, res_mode: Option[String], page: Int, page_size: Int, sort_fields: Option[String], sort_order: Option[String],
                         query: Option[String], code: Option[String], description: Option[String], whereQuery: Option[String], selectedColumn: Option[String]): Future[Result] = {
    val offset: Int = if (page == 1) 0 else (page - 1) * page_size
    val columns = s"${selectedColumn.getOrElse("t.*")}"
    val limitQuery = configuration.getOptional[String]("db.default.driver") match {
      case Some("com.microsoft.sqlserver.jdbc.SQLServerDriver") => s"OFFSET $offset ROWS FETCH NEXT $page_size ROWS ONLY"
      case _                                                    => s"LIMIT $page_size OFFSET $offset"
    }
    val whereSQLSyntax = whereQuery.getOrElse("")
    Logger.info(s"$whereSQLSyntax")
    val orderBy = s"ORDER BY ${sort_fields.getOrElse("t.id")} ${sort_order.getOrElse("asc")}"

    val _entities = db.run(
      sql"SELECT #${columns} FROM #${entity} t #${whereSQLSyntax} #${orderBy} #${limitQuery}".as[Map[String, Any]])
      .map(data => {
        // Aggiungo model: { code: "pippo", description: "pluto"} perché serve alle sixlookup
        data.map( record => { modelMap(record, code, description) })
      })

    val _count = db.run(sql"SELECT count(1) FROM #${entity} t #${whereSQLSyntax}".as[Int]).map(x => {
      Logger.debug(s"$x")
      x
    })

    val collect = for {
      entities <- _entities
      count <- _count
    } yield (entities, count)

    collect.flatMap(x => {
      collectionResult(x._1.toList, res_mode, page, page_size, Some(x._2.toList.head))
    })
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