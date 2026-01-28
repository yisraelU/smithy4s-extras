package com.example.smithy4s.extras

/** Represents a templated curl command */
case class CurlTemplate(
  method: String,
  path: String,
  headers: Map[String, String] = Map.empty,
  queryParams: List[String] = List.empty,
  body: Option[String] = None
) {

  /** Render the curl template as a string */
  def render(indent: Boolean = true): String = {
    val sb = new StringBuilder()

    if (indent) sb.append("curl \\\n  ")
    else sb.append("curl ")

    // Method
    sb.append(s"-X $method")

    // URL with query params
    val url = if (queryParams.isEmpty) {
      s"{{{{host}}}}$path"
    } else {
      val query = queryParams.mkString("&")
      s"{{{{host}}}}$path?$query"
    }

    if (indent) {
      sb.append(s" \\\n  '$url'")
    } else {
      sb.append(s" '$url'")
    }

    // Headers
    headers.foreach { case (key, value) =>
      if (indent) sb.append(s" \\\n  ")
      else sb.append(" ")
      sb.append(s"-H '$key: $value'")
    }

    // Body
    body.foreach { bodyTemplate =>
      if (indent) sb.append(s" \\\n  ")
      else sb.append(" ")
      sb.append(s"-d '$bodyTemplate'")
    }

    sb.toString()
  }

  override def toString: String = render()
}
