package controllers.auth

import javax.inject.{Singleton, Inject}
import play.api.mvc._
import play.api.i18n.I18nSupport
import scala.concurrent.{ExecutionContext, Future}
import cats.data.EitherT
import cats.implicits._

import mvc.auth.AuthMethods
import model.auth.ViewValueAuthLogin
import form.auth.LoginFormData
import libs.model.{User, UserPassword}
import libs.dao.{UserDAO, UserPasswordDAO}

@Singleton
class LoginController @Inject()(
  val userDao:              UserDAO,
  val userPasswordDao:      UserPasswordDAO,
  val authMethods:          AuthMethods,
  val controllerComponents: ControllerComponents
) (implicit val ec: ExecutionContext)
extends BaseController with I18nSupport {

  import play.api.data.Form  

  private val postUrl: Call = controllers.auth.routes.LoginController.post()
  private val homeUrl: Call = controllers.routes.HomeController.home()

  def get() = Action { implicit request =>
    val vv: ViewValueAuthLogin =
      ViewValueAuthLogin(
        form    = LoginFormData.form,
        postUrl = postUrl
      )
    Ok(views.html.auth.Login(vv))
  }

  def post() = Action.async { implicit request =>
    LoginFormData.form.bindFromRequest().fold(
      (formWithErrors: Form[LoginFormData]) => {
        val vv: ViewValueAuthLogin =
          ViewValueAuthLogin(
            form    = LoginFormData.form,
            postUrl = postUrl
          )
        Future.successful(BadRequest(views.html.auth.Login(vv)))
      },
      (login: LoginFormData) => {
        val result: EitherT[Future, Result, Result] =
          for {
            user         <- EitherT(userDao.getByName(login.name).map(_.toRight(NotFound("not found name"))))
            userPassword <- EitherT(userPasswordDao.get(user.withId).map(_.toRight(NotFound)))
            result       <- EitherT(
              userPassword.verify(login.password) match {
                case false => Future.successful(Left(Unauthorized("invalid password")))
                case true  => authMethods.loginSuccess(user, Redirect(homeUrl)).map(Right(_))
              }
            )
          } yield result

        result.merge
      }
    )
  }
}
