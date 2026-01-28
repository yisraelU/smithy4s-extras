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

    // Collections should not be quoted
    assert(body.contains("\"tags\": {{tags}}"), s"tags should be unquoted collection. Body: $body")
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
}
