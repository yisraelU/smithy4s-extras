package com.example.smithy4s.extras

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.schema.Schema._

/** Converts smithy4s endpoints to templated curl commands */
object EndpointToCurl {

  /** Generate a curl template from a smithy4s endpoint. Returns None if the endpoint doesn't have HTTP hints. */
  def toCurl[Op[_, _, _, _, _], I, E, O, SI, SO](endpoint: Endpoint[Op, I, E, O, SI, SO]): Option[CurlTemplate] = {
    val hints = endpoint.hints
    val inputSchema = endpoint.input

    // Extract HTTP metadata - skip if no HTTP hints
    extractHttpMetadata(hints).map { httpMetadata =>
      val method = httpMetadata.method
      val uriPattern = httpMetadata.uri

      // Analyze the input schema
      val inputAnalysis = analyzeInput(inputSchema, uriPattern)

      // Build the path with templated path parameters
      val path = buildTemplatedPath(uriPattern, inputAnalysis.pathParams)


      // Build body if applicable
      val body = buildBody(method, inputAnalysis.bodyFields, inputSchema)

      CurlTemplate(
        method = method,
        path = path,
        headers = inputAnalysis.headers,
        queryParams = inputAnalysis.queryParams,
        body = body
      )
    }
  }

  private case class HttpMetadata(method: String, uri: String, code: Int)

  private case class InputAnalysis(
    pathParams: Set[String],
    queryParams: IndexedSeq[(String, String)],  // (paramName, fieldName) - supports duplicates and preserves order
    headers: IndexedSeq[(String, String)],      // (headerName, fieldName)
    bodyFields: List[String]
  )

  private def extractHttpMetadata(hints: Hints): Option[HttpMetadata] = {
    hints.get(smithy.api.Http).map { http =>
      HttpMetadata(
        method = http.method.value.toUpperCase,
        uri = http.uri.value,
        code = http.code
      )
    }
  }

  private def analyzeInput[A](schema: Schema[A], uriPattern: String): InputAnalysis = {
    schema match {
      case struct: StructSchema[A] =>
        val pathParams = scala.collection.mutable.Set.empty[String]
        val queryParams = scala.collection.mutable.ListBuffer.empty[(String, String)]
        val headers = scala.collection.mutable.ListBuffer.empty[(String, String)]
        val bodyFields = scala.collection.mutable.ListBuffer.empty[String]

        struct.fields.foreach { field =>
          val fieldName = field.label
          val hints = field.hints

          // Check for HTTP label (path parameter)
          if (hints.has(smithy.api.HttpLabel)) {
            pathParams += fieldName
          }
          // Check for HTTP query parameter
          else if (hints.has(smithy.api.HttpQuery)) {
            val queryName = hints.get(smithy.api.HttpQuery).map(_.value).getOrElse(fieldName)
            queryParams += ((queryName, fieldName))
          }
          // Check for HTTP header
          else if (hints.has(smithy.api.HttpHeader)) {
            val headerName = hints.get(smithy.api.HttpHeader).map(_.value).getOrElse(fieldName)
            headers += ((headerName, fieldName))
          }
          // Otherwise, it's part of the body
          else {
            bodyFields += fieldName
          }
        }

        InputAnalysis(
          pathParams = pathParams.toSet,
          queryParams = queryParams.toIndexedSeq,
          headers = headers.toIndexedSeq,
          bodyFields = bodyFields.toList
        )

      case _ =>
        // Non-struct schemas are treated as body payload
        InputAnalysis(
          pathParams = Set.empty,
          queryParams = IndexedSeq.empty,
          headers = IndexedSeq.empty,
          bodyFields = List.empty
        )
    }
  }

  private def buildTemplatedPath(uriPattern: String, pathParams: Set[String]): String = {
    // Replace {param} with {{param}} in the URI pattern
    var path = uriPattern
    pathParams.foreach { param =>
      path = path.replace(s"{$param}", s"{{$param}}")
    }
    path
  }


  private def buildBody[A](
    method: String,
    bodyFields: List[String],
    schema: Schema[A]
  ): Option[String] = {
    // Only include body for methods that typically have one
    val hasBody = method == "POST" || method == "PUT" || method == "PATCH"

    if (hasBody && bodyFields.nonEmpty) {
      schema match {
        case struct: StructSchema[A] =>
          // Generate JSON template with proper typing hints
          val fields = struct.fields.collect {
            case field if bodyFields.contains(field.label) =>
              val fieldName = field.label
              val value = formatFieldValue(field.schema, fieldName)
              s""""$fieldName": $value"""
          }.mkString(", ")
          Some(s"{$fields}")
        case _ =>
          // For non-struct schemas, treat as raw template
          Some(s"{{{{body}}}}")
      }
    } else {
      None
    }
  }

  private def formatFieldValue[A](fieldSchema: Schema[A], fieldName: String): String = {
    // Unwrap optional fields to get the underlying type
    val unwrapped = fieldSchema match {
      case opt: OptionSchema[_] => opt.underlying
      case s => s
    }

    // Use explicit brace concatenation to avoid escaping issues
    val openBrace = "{"
    val closeBrace = "}"
    val template = s"$openBrace$openBrace$fieldName$closeBrace$closeBrace"

    unwrapped match {
      case _: PrimitiveSchema[_] =>
        unwrapped.shapeId.name match {
          case "String" | "Timestamp" | "Blob" => s""""$template""""
          case "Integer" | "Long" | "Short" | "Byte" | "Double" | "Float" | "BigInteger" | "BigDecimal" =>
            template
          case "Boolean" => template
          case _ => s""""$template""""
        }
      case _: CollectionSchema[_, _] => template
      case _: MapSchema[_, _] => template
      case _: StructSchema[_] => template
      case _: UnionSchema[_] => template
      case _ => s""""$template""""
    }
  }
}
