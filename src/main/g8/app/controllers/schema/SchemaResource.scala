package controllers.schema

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.util.Timeout
import play.api.{Configuration, Logger}
import play.api.libs.json.Json
import akka.pattern.ask
import controllers.BaseResource
import controllers.schema.SchemaRepository._
import play.api.db.slick.DatabaseConfigProvider
import services.JsonUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by Riccardo Merolla on 13/11/16.
  */
class SchemaResource  @Inject() (dbConfigProvider: DatabaseConfigProvider, configuration: Configuration, actorSystem: ActorSystem) extends BaseResource(dbConfigProvider, configuration) {

  import profile.api._

  implicit val timeout = Timeout(15 seconds)

  lazy val schemaRepository = Await.result(actorSystem.actorSelection("user/" + SchemaRepository.NAME).resolveOne(), timeout.duration)

  def _code(code: String) = Action.async {
    schemaRepository ? GetSchema(code) map {
      case sf@Some(_) => Ok(sf.getOrElse(Json.obj()).toString).as(JSON)
      case _ => Ok(Json.obj())
    }
  }

  def _set(code: String) = Action.async(parse.json) { request =>
    schemaRepository ? SetSchema(code, request.body) map {
      case _ => Ok(Json.obj("result" -> "OK"))
    }
  }

  def _list()  = Action.async {
    val _entities = db.run(sql"""
                      select c.relfilenode AS code, c.relname AS description from pg_class c
                      left JOIN pg_namespace n ON n.oid = c.relnamespace
                      where c.relkind = 'r'::char
                      AND n.nspname = 'public'
                      order by c.relname;
                  """.as[Map[String, Any]])
      .map(data => data)
    val futureCollection = for {
      list <- _entities
    } yield list

    futureCollection.flatMap(entities => {
      val list = entities.map(data => modelMap(data.toMap, Some("code"), Some("description")))
      collectionResult(list.toList, None, 0, 10000, None)
    })
  }

  def _generate(code: String) = Action.async {
    val _entities = db.run(
      sql"""
          SELECT
            f.attnum AS number,
            f.attname AS name,
            f.attnum,
            f.attnotnull AS notnull,
            pg_catalog.format_type(f.atttypid,f.atttypmod) AS type,
            CASE
                WHEN p.contype = 'p' THEN 't'
                ELSE 'f'
            END AS primarykey,
            CASE
                WHEN p.contype = 'u' THEN 't'
                ELSE 'f'
            END AS uniquekey,
            CASE
                WHEN p.contype = 'f' THEN g.relname
            END AS foreignkey,
            CASE
                WHEN p.contype = 'f' THEN p.confkey
            END AS foreignkey_fieldnum,
            CASE
                WHEN p.contype = 'f' THEN g.relname
            END AS foreignkey,
            CASE
                WHEN p.contype = 'f' THEN p.conkey
            END AS foreignkey_connnum,
            CASE
                WHEN f.atthasdef = 't' THEN d.adsrc
            END AS default
        FROM pg_attribute f
            JOIN pg_class c ON c.oid = f.attrelid
            JOIN pg_type t ON t.oid = f.atttypid
            LEFT JOIN pg_attrdef d ON d.adrelid = c.oid AND d.adnum = f.attnum
            LEFT JOIN pg_namespace n ON n.oid = c.relnamespace
            LEFT JOIN pg_constraint p ON p.conrelid = c.oid AND f.attnum = ANY (p.conkey)
            LEFT JOIN pg_class AS g ON p.confrelid = g.oid
        WHERE c.relkind = 'r'::CHAR
            AND n.nspname = 'public'
            AND c.relname = ${code}
            AND f.attnum > 0 ORDER BY number
      """.as[SchemaColumn]).map(data => data)

    val futureCollection = for {
      collection <- _entities
    } yield collection

    futureCollection.flatMap(entities => {
      val columnMap = entities.map(sc => sc.columntype match {
        case "numeric(19,0)" => {
          if (sc.foreignkey.isDefined) {
            sc.name -> SchemaKey(sc.name.capitalize, "number", sc.notnull, Some("lookup"), sc.foreignkey, Some(CodeDescription(Some("code"), Some("description"))))
          } else {
            sc.name -> SchemaKey(sc.name.capitalize, "number", sc.notnull, None, None, None)
          }
        }
        case "numeric(19,2)" => {
          sc.name -> SchemaKey(sc.name.capitalize, "number", sc.notnull, None, None, None)
        }
        case "bigint" => {
          if (sc.foreignkey.isDefined) {
            sc.name -> SchemaKey(sc.name.capitalize, "number", sc.notnull, Some("lookup"), sc.foreignkey, Some(CodeDescription(Some("code"), Some("description"))))
          } else {
            sc.name -> SchemaKey(sc.name.capitalize, "number", sc.notnull, None, None, None)
          }
        }
        case "integer" => {
          if (sc.foreignkey.isDefined) {
            sc.name -> SchemaKey(sc.name.capitalize, "number", sc.notnull, Some("lookup"), sc.foreignkey, Some(CodeDescription(Some("code"), Some("description"))))
          } else {
            sc.name -> SchemaKey(sc.name.capitalize, "number", sc.notnull, None, None, None)
          }
        }
        case _ => sc.name -> SchemaKey(sc.name.capitalize, "string", sc.notnull, None, None, None)
      }).toMap - "id"
      // SIX
      var columnDefs = entities.filter(e => e.name.equalsIgnoreCase("code") || e.name.equalsIgnoreCase("description")).take(2)
                       .map(sc => ColumnDef(sc.name, sc.name.capitalize))
      if(columnDefs.isEmpty){
        // EGGS
        columnDefs = entities.filterNot(e => e.name.equalsIgnoreCase("id") || e.name.equalsIgnoreCase("azcod")).take(2)
                      .map(sc => ColumnDef(sc.name, sc.name.capitalize))
      }
      val formFields = entities.map(sc => sc.columntype match {
        case "numeric(19,0)" => {
          if (sc.foreignkey.isDefined) {
            FormField(sc.name :: Nil, "sixlookup", sc.name.capitalize, sc.notnull)
          } else {
            FormField(sc.name :: Nil, "number", sc.name.capitalize, sc.notnull)
          }
        }
        case "bigint" => {
          if (sc.foreignkey.isDefined) {
            FormField(sc.name :: Nil, "sixlookup", sc.name.capitalize, sc.notnull)
          } else {
            FormField(sc.name :: Nil, "number", sc.name.capitalize, sc.notnull)
          }
        }
        case "timestamp without time zone" => {
          FormField(sc.name :: Nil, "datepicker", sc.name.capitalize, sc.notnull)
        }
        case "date" => {
          FormField(sc.name :: Nil, "datepicker", sc.name.capitalize, sc.notnull)
        }
        case "character varying(1)" => {
          FormField(sc.name :: Nil, "eggscheckbox", sc.name.capitalize, sc.notnull)
        }
        case "character(1)" => {
          FormField(sc.name :: Nil, "eggscheckbox", sc.name.capitalize, sc.notnull)
        }
        case _ => FormField(sc.name :: Nil, "string", sc.name.capitalize, sc.notnull)
      }).filterNot(ff => ff.key.contains("id") || ff.key.contains("azcod"))
      val requiredFields = entities.filter(e => e.notnull).filterNot(e => e.name.equalsIgnoreCase("id") || e.name.equalsIgnoreCase("azcod")).map(sc => sc.name)
      val schemaform = SchemaForm(code, code.capitalize, columnDefs, Schema(code, "object", columnMap, requiredFields), formFields)
      Logger.info(s"$schemaform")
      Future(Ok(Json.toJson(schemaform)).as(JSON))
    })
  }

}

