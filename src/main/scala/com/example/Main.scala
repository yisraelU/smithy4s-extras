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

  println("\n=== Compact Format Example ===\n")
  // Show compact format for one endpoint
  ApiService.service.endpoints.headOption.foreach { endpoint =>
    EndpointToCurl.toCurl(endpoint).foreach { curlTemplate =>
      println(curlTemplate.render(indent = false))
    }
  }
}
