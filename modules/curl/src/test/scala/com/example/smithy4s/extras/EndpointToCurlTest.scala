package com.example.smithy4s.extras

import test.fixtures.TestService

class EndpointToCurlTest extends munit.FunSuite {

  val service = TestService.service

  test("should handle simple GET with no parameters") {
    val endpoint = service.endpoints.find(_.name == "SimpleGet").get
    val curlOpt = EndpointToCurl.toCurl(endpoint)

    assert(curlOpt.isDefined, "should generate curl for SimpleGet")
    val curl = curlOpt.get

    assertEquals(curl.method, "GET")
    assertEquals(curl.path, "/simple")
    assert(curl.queryParams.isEmpty, "should have no query params")
    assert(curl.headers.isEmpty, "should have no headers")
    assert(curl.body.isEmpty, "should have no body")
  }

  test("should handle path parameters only") {
    val endpoint = service.endpoints.find(_.name == "PathParamsOnly").get
    val curlOpt = EndpointToCurl.toCurl(endpoint)

    assert(curlOpt.isDefined)
    val curl = curlOpt.get

    assertEquals(curl.method, "GET")
    assertEquals(curl.path, "/users/{{userId}}/posts/{{postId}}")
    assert(curl.queryParams.isEmpty)
    assert(curl.headers.isEmpty)
    assert(curl.body.isEmpty)
  }

  test("should handle query parameters only") {
    val endpoint = service.endpoints.find(_.name == "QueryParamsOnly").get
    val curlOpt = EndpointToCurl.toCurl(endpoint)

    assert(curlOpt.isDefined)
    val curl = curlOpt.get

    assertEquals(curl.method, "GET")
    assertEquals(curl.path, "/search")
    assertEquals(curl.queryParams.size, 4, "should have 4 query params")

    // Check query params in order
    val queryMap = curl.queryParams.toMap
    assert(queryMap.contains("q"), "should have 'q' param")
    assert(queryMap.contains("limit"), "should have 'limit' param")
    assert(queryMap.contains("offset"), "should have 'offset' param")
    assert(queryMap.contains("active"), "should have 'active' param")

    assertEquals(queryMap("q"), "query")
    assertEquals(queryMap("limit"), "limit")
    assertEquals(queryMap("offset"), "offset")
    assertEquals(queryMap("active"), "active")

    assert(curl.headers.isEmpty)
    assert(curl.body.isEmpty)
  }

  test("should handle mixed parameters (path, query, headers, body)") {
    val endpoint = service.endpoints.find(_.name == "MixedParams").get
    val curlOpt = EndpointToCurl.toCurl(endpoint)

    assert(curlOpt.isDefined)
    val curl = curlOpt.get

    assertEquals(curl.method, "POST")
    assertEquals(curl.path, "/organizations/{{orgId}}/users", "path should have orgId replaced")

    // Check query params
    assertEquals(curl.queryParams.size, 1)
    assertEquals(curl.queryParams.head, ("notify", "notify"))

    // Check headers
    assertEquals(curl.headers.size, 2)
    val headerMap = curl.headers.toMap
    assertEquals(headerMap("X-Request-ID"), "requestId")
    assertEquals(headerMap("X-Idempotency-Key"), "idempotencyKey")

    // Check body
    assert(curl.body.isDefined, "POST should have body")
    val body = curl.body.get
    assert(body.contains("username"), "body should contain username field")
    assert(body.contains("email"), "body should contain email field")
    assert(body.contains("role"), "body should contain role field")
    assert(!body.contains("orgId"), "body should not contain path param")
    assert(!body.contains("notify"), "body should not contain query param")
  }

  test("should handle POST with optional fields and different types") {
    val endpoint = service.endpoints.find(_.name == "PostWithOptionalFields").get
    val curlOpt = EndpointToCurl.toCurl(endpoint)

    assert(curlOpt.isDefined)
    val curl = curlOpt.get

    assertEquals(curl.method, "POST")
    assert(curl.body.isDefined)

    val body = curl.body.get

    // String should be quoted
    assert(body.contains("\"name\": \"{{name}}\""), s"name should be quoted string. Body: $body")

    // Numbers should not be quoted
    assert(body.contains("\"count\": {{count}}"), s"count should be unquoted number. Body: $body")
    assert(body.contains("\"price\": {{price}}"), s"price should be unquoted number. Body: $body")

    // Boolean should not be quoted
    assert(body.contains("\"active\": {{active}}"), s"active should be unquoted boolean. Body: $body")

    // Collections should be rendered as arrays with element template
    assert(body.contains("\"tags\":"), s"tags should be present. Body: $body")
    assert(body.matches(""".*"tags":\s*\[.*\].*"""), s"tags should be formatted as array. Body: $body")
  }

  test("query params should preserve field order from schema") {
    val endpoint = service.endpoints.find(_.name == "QueryParamsOnly").get
    val curlOpt = EndpointToCurl.toCurl(endpoint)

    assert(curlOpt.isDefined)
    val curl = curlOpt.get

    // Query params should be in schema definition order, not sorted
    val paramNames = curl.queryParams.map(_._1)
    assertEquals(paramNames, IndexedSeq("q", "limit", "offset", "active"))
  }

  test("should handle single-level nested object") {
    val endpoint = service.endpoints.find(_.name == "PostWithNestedObject").get
    val curlOpt = EndpointToCurl.toCurl(endpoint)

    assert(curlOpt.isDefined)
    val curl = curlOpt.get

    assertEquals(curl.method, "POST")
    assert(curl.body.isDefined)

    val body = curl.body.get

    // Should have top-level name field as string
    assert(body.contains("\"name\": \"{{name}}\""), s"name should be quoted string. Body: $body")

    // Should have nested address object with all its fields
    assert(body.contains("\"address\":"), s"should have address field. Body: $body")
    assert(body.contains("\"street\": \"{{street}}\""), s"address.street should be present. Body: $body")
    assert(body.contains("\"city\": \"{{city}}\""), s"address.city should be present. Body: $body")
    assert(body.contains("\"country\": \"{{country}}\""), s"address.country should be present. Body: $body")
    assert(body.contains("\"zipCode\": \"{{zipCode}}\""), s"address.zipCode should be present. Body: $body")

    // Nested object should be properly formatted as JSON object
    assert(body.matches(""".*"address":\s*\{.*\}.*"""), s"address should be a JSON object. Body: $body")
  }

  test("should handle deeply nested objects (3+ levels)") {
    val endpoint = service.endpoints.find(_.name == "PostWithDeeplyNestedObject").get
    val curlOpt = EndpointToCurl.toCurl(endpoint)

    assert(curlOpt.isDefined)
    val curl = curlOpt.get

    assertEquals(curl.method, "POST")
    assert(curl.body.isDefined)

    val body = curl.body.get

    // Top level
    assert(body.contains("\"name\": \"{{name}}\""), s"name should be present. Body: $body")
    assert(body.contains("\"headquarters\":"), s"headquarters should be present. Body: $body")

    // Level 2: Office
    assert(body.contains("\"location\":"), s"headquarters.location should be present. Body: $body")
    assert(body.contains("\"phoneNumber\": \"{{phoneNumber}}\""), s"headquarters.phoneNumber should be present. Body: $body")

    // Level 3: Location
    assert(body.contains("\"address\":"), s"location.address should be present. Body: $body")
    assert(body.contains("\"coordinates\":"), s"location.coordinates should be present. Body: $body")

    // Level 4: Address fields
    assert(body.contains("\"street\": \"{{street}}\""), s"address.street should be present. Body: $body")
    assert(body.contains("\"city\": \"{{city}}\""), s"address.city should be present. Body: $body")

    // Level 4: Coordinates
    assert(body.contains("\"latitude\": {{latitude}}"), s"coordinates.latitude should be present. Body: $body")
    assert(body.contains("\"longitude\": {{longitude}}"), s"coordinates.longitude should be present. Body: $body")
  }

  test("should handle nested arrays of objects") {
    val endpoint = service.endpoints.find(_.name == "PostWithNestedArray").get
    val curlOpt = EndpointToCurl.toCurl(endpoint)

    assert(curlOpt.isDefined)
    val curl = curlOpt.get

    assertEquals(curl.method, "POST")
    assert(curl.body.isDefined)

    val body = curl.body.get

    // Top level
    assert(body.contains("\"name\": \"{{name}}\""), s"name should be present. Body: $body")

    // Array of complex objects
    assert(body.contains("\"members\":"), s"members array should be present. Body: $body")
    assert(body.contains("\"userId\": \"{{userId}}\""), s"member.userId should be present in array template. Body: $body")
    assert(body.contains("\"role\": \"{{role}}\""), s"member.role should be present in array template. Body: $body")
    assert(body.contains("\"permissions\":"), s"member.permissions should be present. Body: $body")

    // Simple array
    assert(body.contains("\"tags\":"), s"tags array should be present. Body: $body")

    // Array should be formatted with brackets
    assert(body.matches(""".*"members":\s*\[.*\].*"""), s"members should be a JSON array. Body: $body")
  }

  test("should handle complex multi-level nesting with mixed types") {
    val endpoint = service.endpoints.find(_.name == "PostWithComplexNesting").get
    val curlOpt = EndpointToCurl.toCurl(endpoint)

    assert(curlOpt.isDefined)
    val curl = curlOpt.get

    assertEquals(curl.method, "POST")
    assert(curl.body.isDefined)

    val body = curl.body.get

    // Customer object with nested preferences
    assert(body.contains("\"customer\":"), s"customer should be present. Body: $body")
    assert(body.contains("\"id\": \"{{id}}\""), s"customer.id should be present. Body: $body")
    assert(body.contains("\"preferences\":"), s"customer.preferences should be present. Body: $body")
    assert(body.contains("\"notifications\":"), s"preferences.notifications should be present. Body: $body")
    assert(body.contains("\"newsletter\": {{newsletter}}"), s"preferences.newsletter should be boolean. Body: $body")
    assert(body.contains("\"email\": {{email}}"), s"notifications.email should be boolean. Body: $body")
    assert(body.contains("\"sms\": {{sms}}"), s"notifications.sms should be boolean. Body: $body")

    // Array of items with nested fields
    assert(body.contains("\"items\":"), s"items array should be present. Body: $body")
    assert(body.contains("\"productId\": \"{{productId}}\""), s"item.productId should be present. Body: $body")
    assert(body.contains("\"quantity\": {{quantity}}"), s"item.quantity should be number. Body: $body")
    assert(body.contains("\"price\": {{price}}"), s"item.price should be number. Body: $body")
    assert(body.contains("\"options\":"), s"item.options array should be present. Body: $body")

    // Multiple address objects (reused structure)
    assert(body.contains("\"shippingAddress\":"), s"shippingAddress should be present. Body: $body")
    assert(body.contains("\"billingAddress\":"), s"billingAddress should be present. Body: $body")

    // Metadata object
    assert(body.contains("\"metadata\":"), s"metadata should be present. Body: $body")
    assert(body.contains("\"source\": \"{{source}}\""), s"metadata.source should be present. Body: $body")
  }
}
