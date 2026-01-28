# smithy4s-extras

A collection of utilities for working with smithy4s.

## Features

### Curl Template Generator

Generate templatable curl commands from smithy4s endpoints. The generator extracts HTTP metadata from Smithy schemas and produces curl commands with Mustache-style template variables.

#### Usage

```scala
import com.example.smithy4s.extras.EndpointToCurl
import yourpackage.YourService

val service = YourService.service

service.endpoints.foreach { endpoint =>
  EndpointToCurl.toCurl(endpoint) match {
    case Some(curlTemplate) =>
      println(curlTemplate.render(indent = true))
    case None =>
      println("No HTTP hints found")
  }
}
```

#### Template Syntax

The generator uses Mustache-style `{{variable}}` syntax for template variables:

```bash
curl -X POST '{{host}}/users' \
  -H 'Content-Type: application/json' \
  -d '{"name": "{{name}}", "email": "{{email}}", "age": {{age}}}'
```

#### Supported Features

- **HTTP Methods**: GET, POST, PUT, DELETE, PATCH
- **Path Parameters**: Extracted from `@httpLabel` hints
  ```bash
  curl -X GET '{{host}}/users/{{userId}}'
  ```

- **Query Parameters**: Extracted from `@httpQuery` hints
  ```bash
  curl -X GET '{{host}}/users?limit={{limit}}&offset={{offset}}'
  ```

- **Headers**: Extracted from `@httpHeader` hints
  ```bash
  curl -X GET '{{host}}/users' \
    -H 'X-Request-ID: {{requestId}}'
  ```

- **Request Body**: Auto-generated from input schema with proper type handling
  - String fields are quoted: `"name": "{{name}}"`
  - Numeric fields are unquoted: `"age": {{age}}`
  - Boolean fields are unquoted: `"active": {{active}}`

#### Output Formats

**Indented** (default):
```scala
curlTemplate.render(indent = true)
```
```bash
curl \
  -X POST \
  '{{host}}/users' \
  -H 'Content-Type: application/json' \
  -d '{"name": "{{name}}", "email": "{{email}}"}'
```

**Compact**:
```scala
curlTemplate.render(indent = false)
```
```bash
curl -X POST '{{host}}/users' -H 'Content-Type: application/json' -d '{"name": "{{name}}", "email": "{{email}}"}'
```

#### Example

See `src/main/smithy/comprehensive.smithy` for a complete example service and `src/main/scala/com/example/Main.scala` for usage examples.

Run the example:
```bash
sbt run
```

## Building

```bash
sbt compile
```

## License

See LICENSE file.