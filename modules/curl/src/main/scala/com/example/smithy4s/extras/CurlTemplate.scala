package com.example.smithy4s.extras

/** Represents a templated curl command */
case class CurlTemplate(
                         method: String,
                         path: String,
                         headers: IndexedSeq[(String, String)] = IndexedSeq.empty, // (headerName, fieldName)
                         queryParams: IndexedSeq[(String, String)] = IndexedSeq.empty, // (paramName, fieldName)
                         body: Option[String] = None
                       ) {

  /** Render using Mustache-style {{variable}} syntax */
  def renderMustache(indent: Boolean = true): String =
    render(TemplateStyle.Mustache, indent)

  /** Render using Shell-style ${variable} syntax */
  def renderShell(indent: Boolean = true): String =
    render(TemplateStyle.Shell, indent)

  /** Render using simple {variable} syntax */
  def renderSimple(indent: Boolean = true): String =
    render(TemplateStyle.Simple, indent)

  /** Render the curl template with a specific template style */
  def render(style: TemplateStyle = TemplateStyle.Mustache, indent: Boolean = true): String = {
    val sb = new StringBuilder()

    if (indent) sb.append("curl \\\n  ")
    else sb.append("curl ")

    // Method
    sb.append(s"-X $method")

    // URL with query params
    val queryString = if (queryParams.isEmpty) {
      ""
    } else {
      val formattedParams = queryParams.map { case (paramName, fieldName) =>
        s"$paramName=${style.format(fieldName)}"
      }.mkString("&")
      s"?$formattedParams"
    }

    val url = s"${style.format("host")}${convertStyle(path, style)}$queryString"

    if (indent) {
      sb.append(s" \\\n  '$url'")
    } else {
      sb.append(s" '$url'")
    }

    // Headers
    // Add Content-Type for methods with bodies
    val hasBody = body.isDefined
    val needsContentType = hasBody && !headers.exists(_._1 == "Content-Type")

    if (needsContentType) {
      if (indent) sb.append(s" \\\n  ")
      else sb.append(" ")
      sb.append(s"-H 'Content-Type: application/json'")
    }

    headers.foreach { case (headerName, fieldName) =>
      if (indent) sb.append(s" \\\n  ")
      else sb.append(" ")
      sb.append(s"-H '$headerName: ${style.format(fieldName)}'")
    }

    // Body
    body.foreach { bodyTemplate =>
      if (indent) sb.append(s" \\\n  ")
      else sb.append(" ")
      sb.append(s"-d '${convertStyle(bodyTemplate, style)}'")
    }

    sb.toString()
  }

  /** Convert Mustache-style {{variable}} to the target style */
  private def convertStyle(text: String, style: TemplateStyle): String = {
    // Match {{variable}} pattern and replace with target style
    val pattern = "\\{\\{([^}]+)\\}\\}".r
    pattern.replaceAllIn(text, m => {
      val varName = m.group(1)
      // Use quoteReplacement to handle special regex characters like $ in the replacement
      java.util.regex.Matcher.quoteReplacement(style.format(varName))
    })
  }

  override def toString: String = render()
}
