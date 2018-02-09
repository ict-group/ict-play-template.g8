package controllers.record

import javax.inject.Inject

import controllers.BaseResource
import play.api.db.slick.DatabaseConfigProvider
import play.api.{Configuration}

/**
  * Created by Riccardo Merolla on 26/08/16.
  */
class RecordResource @Inject() (dbConfigProvider: DatabaseConfigProvider, configuration: Configuration) extends BaseResource(dbConfigProvider, configuration) {

}

