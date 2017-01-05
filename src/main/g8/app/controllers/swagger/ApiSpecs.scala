package controllers.swagger

import javax.inject.Inject

import com.iheart.playSwagger.SwaggerSpecGenerator
import play.api.cache.Cached
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
/**
  * @author Riccardo Merolla
  *         Created on 04/07/16.
  */
class ApiSpecs @Inject() (cached: Cached) extends Controller {
  implicit val cl = getClass.getClassLoader

  // The root package of your domain classes, play-swagger will automatically generate definitions when it encounters class references in this package.
  // In our case it would be "com.iheart", play-swagger supports multiple domain package names
  val domainPackage = "models"
  private lazy val generator = SwaggerSpecGenerator(domainPackage)

  def specs = {  //cached("swaggerDef") ßit would be beneficial to cache this endpoint as we do here, but it's not required if you don't expect much traffic.
    Action.async { _ =>
      Future.fromTry(generator.generate()).map(Ok(_)) //generate() can also taking in an optional arg of the route file name.
    }
  }

}
