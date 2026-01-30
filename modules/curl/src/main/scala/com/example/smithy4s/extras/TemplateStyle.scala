package com.example.smithy4s.extras

/** Template variable syntax styles */
sealed trait TemplateStyle {
  def format(variable: String): String

  def name: String
}

object TemplateStyle {
  /** Mustache-style: {{variable}} */
  case object Mustache extends TemplateStyle {
    def format(variable: String): String = s"{{$variable}}"

    val name: String = "mustache"
  }

  /** Shell/Bash-style: ${variable} */
  case object Shell extends TemplateStyle {
    def format(variable: String): String = s"$${$variable}"

    val name: String = "shell"
  }

  /** Simple brackets: {variable} */
  case object Simple extends TemplateStyle {
    def format(variable: String): String = s"{$variable}"

    val name: String = "simple"
  }

  /** Default style (Mustache) */
  val default: TemplateStyle = Mustache
}
