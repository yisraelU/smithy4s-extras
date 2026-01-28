package com.example

import com.example.smithy4s.extras.EndpointToCurl
import comprehensive.ApiService

object TemplateDemo extends App {
  println("=== Multi-Template Style Demo ===\n")

  // Get the SearchUsers endpoint (has path, query, and headers)
  val endpoint = ApiService.service.endpoints.find(_.name == "SearchUsers").get

  EndpointToCurl.toCurl(endpoint).foreach { curlTemplate =>
    println("Mustache Style:")
    println(curlTemplate.renderMustache())
    println("\n" + "-" * 60 + "\n")

    println("Shell Style (great for bash scripts):")
    println(curlTemplate.renderShell())
    println("\n" + "-" * 60 + "\n")

    println("Simple Style (minimal syntax):")
    println(curlTemplate.renderSimple())
    println("\n" + "-" * 60 + "\n")

    println("Compact Shell (one-liner for scripts):")
    println(curlTemplate.renderShell(indent = false))
  }
}
