package com.example.smithy4s.extras

import smithy4s.{Endpoint, Hints, Schema}
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
      val inputAnalysis = analyzeInput(inputSchema).getOrElse(InputAnalysis.empty)

      // Build the path with templated path parameters
      val path = buildTemplatedPath(uriPattern, inputAnalysis.pathParams)


      // Build body if applicable
      val body = buildBody(method, inputAnalysis.bodyFields)

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

  sealed trait FieldType
  object FieldType {
    case object StringType extends FieldType
    case object NumberType extends FieldType
    case object BooleanType extends FieldType
    case class ObjectType(fields: IndexedSeq[(String, FieldType)]) extends FieldType
    case class ArrayType(element: FieldType) extends FieldType
  }

  case class InputAnalysis(
                            pathParams: Set[String],
                            queryParams: IndexedSeq[(String, String)], // (paramName, fieldName) - supports duplicates and preserves order
                            headers: IndexedSeq[(String, String)], // (headerName, fieldName)
                            bodyFields: IndexedSeq[(String, FieldType)] // (fieldName, fieldType)
                          )

  object InputAnalysis {
    val empty = InputAnalysis(
      pathParams = Set.empty,
      queryParams = IndexedSeq.empty,
      headers = IndexedSeq.empty,
      bodyFields = IndexedSeq.empty
    )
  }

  private def extractHttpMetadata(hints: Hints): Option[HttpMetadata] = {
    hints.get(smithy.api.Http).map { http =>
      HttpMetadata(
        method = http.method.value.toUpperCase,
        uri = http.uri.value,
        code = http.code
      )
    }
  }

  private type Result[A] = Option[InputAnalysis]

  private def schemaToFieldType[A](schema: Schema[A]): FieldType = {
    // Unwrap optional fields to get the underlying type
    val unwrapped = schema match {
      case opt: OptionSchema[_] => opt.underlying
      case s => s
    }

    type Const[T] = FieldType

    unwrapped.compile(new smithy4s.schema.SchemaVisitor.Default[Const] {
      override def default[T]: Const[T] = FieldType.ObjectType(IndexedSeq.empty)

      override def primitive[P](shapeId: smithy4s.ShapeId, hints: Hints, tag: smithy4s.schema.Primitive[P]): Const[P] = {
        shapeId.name match {
          case "String" | "Timestamp" | "Blob" => FieldType.StringType
          case "Integer" | "Long" | "Short" | "Byte" | "Double" | "Float" | "BigInteger" | "BigDecimal" =>
            FieldType.NumberType
          case "Boolean" => FieldType.BooleanType
          case _ => FieldType.StringType
        }
      }

      override def collection[C[_], A](shapeId: smithy4s.ShapeId, hints: Hints, tag: smithy4s.schema.CollectionTag[C], member: Schema[A]): Const[C[A]] = {
        val elementType = schemaToFieldType(member)
        FieldType.ArrayType(elementType)
      }

      override def map[K, V](shapeId: smithy4s.ShapeId, hints: Hints, key: Schema[K], value: Schema[V]): Const[Map[K, V]] =
        FieldType.ObjectType(IndexedSeq.empty)

      override def struct[S](shapeId: smithy4s.ShapeId, hints: Hints, fields: Vector[smithy4s.schema.Field[S, _]], make: IndexedSeq[Any] => S): Const[S] = {
        val fieldTypes = fields.map { field =>
          (field.label, schemaToFieldType(field.schema))
        }
        FieldType.ObjectType(fieldTypes)
      }

      override def union[U](shapeId: smithy4s.ShapeId, hints: Hints, alternatives: Vector[smithy4s.schema.Alt[U, _]], dispatch: smithy4s.schema.Alt.Dispatcher[U]): Const[U] =
        FieldType.ObjectType(IndexedSeq.empty)
    })
  }

  private def analyzeInput[A](schema: Schema[A]): Result[A] = {


    schema.compile(new smithy4s.schema.SchemaVisitor.Default[Result] {
      // Default case: Non-struct schemas are treated as body payload
      override def default[T]: Result[T] = None

      override def struct[S](shapeId: smithy4s.ShapeId, hints: Hints, fields: Vector[smithy4s.schema.Field[S, _]], make: IndexedSeq[Any] => S): Result[S] = {
        Some(
          fields.foldLeft(InputAnalysis.empty) { (acc, field) =>
            val fieldName = field.label
            val hints = field.hints

            // Check for HTTP label (path parameter)
            if (hints.has(smithy.api.HttpLabel)) {
              acc.copy(pathParams = acc.pathParams + fieldName)
            }
            // Check for HTTP query parameter
            else if (hints.has(smithy.api.HttpQuery)) {
              val queryName = hints.get(smithy.api.HttpQuery).map(_.value).getOrElse(fieldName)
              acc.copy(queryParams = acc.queryParams :+ ((queryName, fieldName)))
            }
            // Check for HTTP header
            else if (hints.has(smithy.api.HttpHeader)) {
              val headerName = hints.get(smithy.api.HttpHeader).map(_.value).getOrElse(fieldName)
              acc.copy(headers = acc.headers :+ ((headerName, fieldName)))
            }
            // Otherwise, it's part of the body
            else {
              val fieldType = schemaToFieldType(field.schema)
              acc.copy(bodyFields = acc.bodyFields :+ ((fieldName, fieldType)))
            }
          }
        )
      }
    })
  }

  private def buildTemplatedPath(uriPattern: String, pathParams: Set[String]): String = {
    // Replace {param} with {{param}} in the URI pattern
    pathParams.foldLeft(uriPattern) { (path, param) =>
      path.replace(s"{$param}", s"{{$param}}")
    }
  }


  private def buildBody(
                         method: String,
                         bodyFields: IndexedSeq[(String, FieldType)]
                       ): Option[String] = {
    // Only include body for methods that typically have one
    val hasBody = method == "POST" || method == "PUT" || method == "PATCH"

    if (hasBody && bodyFields.nonEmpty) {
      // Generate JSON template with proper typing hints
      val fields = bodyFields.map { case (fieldName, fieldType) =>
        val value = formatFieldValue(fieldName, fieldType)
        s""""$fieldName": $value"""
      }.mkString(", ")
      Some(s"{$fields}")
    } else {
      None
    }
  }

  private def formatFieldValue(fieldName: String, fieldType: FieldType): String = {
    // Use explicit brace concatenation to avoid escaping issues
    val openBrace = "{"
    val closeBrace = "}"
    val template = s"$openBrace$openBrace$fieldName$closeBrace$closeBrace"

    fieldType match {
      case FieldType.StringType => s""""$template""""
      case FieldType.NumberType => template
      case FieldType.BooleanType => template
      case FieldType.ObjectType(fields) =>
        if (fields.isEmpty) {
          template
        } else {
          val formattedFields = fields.map { case (name, typ) =>
            s""""$name": ${formatFieldValue(name, typ)}"""
          }.mkString(", ")
          s"{$formattedFields}"
        }
      case FieldType.ArrayType(element) =>
        s"[${formatFieldValue("item", element)}]"
    }
  }
}
