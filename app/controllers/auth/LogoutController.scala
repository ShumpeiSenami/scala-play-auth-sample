package controllers.auth

import javax.inject.{Singleton, Inject}
import play.api.mvc._
import play.api.i18n.I18nSupport
import scala.concurrent.{ExecutionContext, Future}

import mvc.auth.{AuthAction, AuthMethods}
import model.auth.ViewValueAuthLogout
import libs.model.{User, UserPassword}
import libs.dao.{UserDAO, UserPasswordDAO}

@Singleton
class LogoutController @Inject()(
  val userDao:              UserDAO,
  val userPasswordDao:      UserPasswordDAO,
  val authAction:           AuthAction,
  val authMethods:          AuthMethods,
  val controllerComponents: ControllerComponents
) (implicit val ec: ExecutionContext)
extends BaseController
with I18nSupport {

  import play.api.data.Form  

  private val postUrl:  Call = controllers.auth.routes.LogoutController.post()
  private val indexUrl: Call = controllers.routes.HomeController.index()

  def get() = (Action andThen authAction).async { implicit request =>
    val vv: ViewValueAuthLogout =
      ViewValueAuthLogout(
        postUrl = postUrl
      )
    Future.successful(Ok(views.html.auth.Logout(vv)))
  }

  def post() = (Action andThen authAction).async { implicit request =>
    for {
      result <- authMethods.logoutSuccess(Redirect(indexUrl))
    } yield result
  }
}
