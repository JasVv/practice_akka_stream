package controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject()(override val controllerComponents: ControllerComponents)
                              (implicit system: ActorSystem, materializer: Materializer, ec: ExecutionContext)
  extends BaseController {

  def get: Action[AnyContent] = Action { implicit request =>
    Ok("ok")
  }
}
