package com.example.smithy4s.extras

class CurlTemplateTest extends munit.FunSuite {

  test("renderMustache - simple GET with path params") {
    val curl = CurlTemplate(
      method = "GET",
      path = "/users/{{userId}}",
      queryParams = IndexedSeq.empty,
      headers = IndexedSeq.empty,
      body = None
    )

    val result = curl.renderMustache(indent = false)
    assertEquals(result, "curl -X GET '{{host}}/users/{{userId}}'")
  }

  test("renderShell - simple GET with path params") {
    val curl = CurlTemplate(
      method = "GET",
      path = "/users/{{userId}}",
      queryParams = IndexedSeq.empty,
      headers = IndexedSeq.empty,
      body = None
    )

    val result = curl.renderShell(indent = false)
    assertEquals(result, "curl -X GET '${host}/users/${userId}'")
  }

  test("renderSimple - simple GET with path params") {
    val curl = CurlTemplate(
      method = "GET",
      path = "/users/{{userId}}",
      queryParams = IndexedSeq.empty,
      headers = IndexedSeq.empty,
      body = None
    )

    val result = curl.renderSimple(indent = false)
    assertEquals(result, "curl -X GET '{host}/users/{userId}'")
  }

  test("render with query params - Mustache style") {
    val curl = CurlTemplate(
      method = "GET",
      path = "/search",
      queryParams = IndexedSeq(
        ("q", "query"),
        ("limit", "limit"),
        ("offset", "offset")
      ),
      headers = IndexedSeq.empty,
      body = None
    )

    val result = curl.renderMustache(indent = false)
    assertEquals(
      result,
      "curl -X GET '{{host}}/search?q={{query}}&limit={{limit}}&offset={{offset}}'"
    )
  }

  test("render with query params - Shell style") {
    val curl = CurlTemplate(
      method = "GET",
      path = "/search",
      queryParams = IndexedSeq(("q", "query")),
      headers = IndexedSeq.empty,
      body = None
    )

    val result = curl.renderShell(indent = false)
    assertEquals(result, "curl -X GET '${host}/search?q=${query}'")
  }

  test("render with headers") {
    val curl = CurlTemplate(
      method = "GET",
      path = "/api",
      queryParams = IndexedSeq.empty,
      headers = IndexedSeq(
        ("X-API-Key", "apiKey"),
        ("X-Request-ID", "requestId")
      ),
      body = None
    )

    val result = curl.renderMustache(indent = false)
    assert(result.contains("-H 'X-API-Key: {{apiKey}}'"))
    assert(result.contains("-H 'X-Request-ID: {{requestId}}'"))
  }

  test("render POST with body adds Content-Type header") {
    val curl = CurlTemplate(
      method = "POST",
      path = "/users",
      queryParams = IndexedSeq.empty,
      headers = IndexedSeq.empty,
      body = Some("{\"name\": \"{{name}}\"}")
    )

    val result = curl.renderMustache(indent = false)
    assert(result.contains("-H 'Content-Type: application/json'"),
      s"should add Content-Type header. Result: $result")
    assert(result.contains("-d '{\"name\": \"{{name}}\"}'"),
      s"should contain body with escaped quotes. Result: $result")
  }

  test("render POST with body and existing Content-Type doesn't duplicate") {
    val curl = CurlTemplate(
      method = "POST",
      path = "/users",
      queryParams = IndexedSeq.empty,
      headers = IndexedSeq(("Content-Type", "customType")),
      body = Some("{\"name\": \"{{name}}\"}")
    )

    val result = curl.renderMustache(indent = false)

    // Count occurrences of "Content-Type"
    val count = "Content-Type".r.findAllIn(result).length
    assertEquals(count, 1, "Content-Type should appear exactly once")
    assert(result.contains("-H 'Content-Type: {{customType}}'"))
  }

  test("render with indentation") {
    val curl = CurlTemplate(
      method = "POST",
      path = "/users",
      queryParams = IndexedSeq(("notify", "notify")),
      headers = IndexedSeq(("X-API-Key", "apiKey")),
      body = Some("{\"name\": \"{{name}}\"}")
    )

    val result = curl.renderMustache(indent = true)

    assert(result.startsWith("curl \\\n"))
    assert(result.contains("\\\n  "))
    assert(result.contains("-X POST"))
    assert(result.contains("'{{host}}/users?notify={{notify}}'"))
  }

  test("render without indentation is single line") {
    val curl = CurlTemplate(
      method = "GET",
      path = "/api",
      queryParams = IndexedSeq(("q", "query")),
      headers = IndexedSeq(("X-Key", "key")),
      body = None
    )

    val result = curl.renderMustache(indent = false)
    assert(!result.contains("\n"), "should not contain newlines")
    assert(result.startsWith("curl -X"))
  }

  test("render with all template styles for same input") {
    val curl = CurlTemplate(
      method = "POST",
      path = "/items/{{itemId}}",
      queryParams = IndexedSeq(("filter", "filter")),
      headers = IndexedSeq(("X-Token", "token")),
      body = Some("{\"value\": {{value}}}")
    )

    val mustache = curl.renderMustache(indent = false)
    val shell = curl.renderShell(indent = false)
    val simple = curl.renderSimple(indent = false)

    // Mustache
    assert(mustache.contains("{{host}}"))
    assert(mustache.contains("{{itemId}}"))
    assert(mustache.contains("{{filter}}"))
    assert(mustache.contains("{{token}}"))
    assert(mustache.contains("{{value}}"))

    // Shell
    assert(shell.contains("${host}"))
    assert(shell.contains("${itemId}"))
    assert(shell.contains("${filter}"))
    assert(shell.contains("${token}"))
    assert(shell.contains("${value}"))

    // Simple
    assert(simple.contains("{host}"))
    assert(simple.contains("{itemId}"))
    assert(simple.contains("{filter}"))
    assert(simple.contains("{token}"))
    assert(simple.contains("{value}"))
  }

  test("query params preserve order") {
    val curl = CurlTemplate(
      method = "GET",
      path = "/search",
      queryParams = IndexedSeq(
        ("c", "third"),
        ("a", "first"),
        ("b", "second")
      ),
      headers = IndexedSeq.empty,
      body = None
    )

    val result = curl.renderMustache(indent = false)

    // Should preserve definition order, not sort alphabetically
    val queryString = result.substring(result.indexOf("?") + 1, result.lastIndexOf("'"))
    assertEquals(queryString, "c={{third}}&a={{first}}&b={{second}}")
  }
}
