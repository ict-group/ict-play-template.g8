package controllers.schema

import javax.inject.Inject

import controllers.BaseResource
import org.sedis.Pool
import play.api.Configuration
import play.api.cache.{CacheApi, _}
import play.api.libs.json.{JsResult, JsSuccess, Json}
import play.api.mvc.Action
import services.JsonUtil

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Riccardo Merolla on 13/11/16.
  */
class SchemaResource  @Inject() (@NamedCache("schema-cache") schemaCache: CacheApi, sedisPool: Pool, configuration: Configuration) extends BaseResource(configuration) {

  def _code(code: String) = Action {
    Ok(schemaCache.get(code).getOrElse(Json.obj()))
  }

  def _set(code: String) = Action(parse.json) { request =>
      schemaCache.set(code, request.body)
      Ok(Json.obj("result" -> "OK"))
  }

}

