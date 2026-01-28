package com.example

import com.example.smithy4s.extras.EndpointToCurl
import comprehensive.ApiService

object Main extends App {
  println("=== Smithy4s Curl Template Generator ===\n")

  val service = ApiService.service

  println(s"Service: ${service.id.name}")
  println("=" * 60 + "\n")

  service.endpoints.foreach { endpoint =>
    println(s"Operation: ${endpoint.name}")

    // Generate curl template
    EndpointToCurl.toCurl(endpoint) match {
      case Some(curlTemplate) =>
        println("\nCurl Command:")
        println(curlTemplate.render(indent = true))
        println()
      case None =>
        println("⚠️  Skipped: No HTTP hints found\n")
    }

    println("-" * 60 + "\n")
  }

  println("\n=== Template Style Examples ===\n")
  // Show different template styles for one endpoint
  ApiService.service.endpoints.find(_.name == "CreateUser").foreach { endpoint =>
    EndpointToCurl.toCurl(endpoint).foreach { curlTemplate =>
      println("Mustache Style ({{variable}}):")
      println(curlTemplate.renderMustache(indent = true))
      println()

      println("Shell Style (${variable}):")
      println(curlTemplate.renderShell(indent = true))
      println()

      println("Simple Style ({variable}):")
      println(curlTemplate.renderSimple(indent = true))
      println()

      println("Compact Mustache:")
      println(curlTemplate.renderMustache(indent = false))
    }
  }
}
