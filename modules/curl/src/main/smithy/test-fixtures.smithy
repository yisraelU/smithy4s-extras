$version: "2"

namespace test.fixtures

use alloy#simpleRestJson

@simpleRestJson
service TestService {
    version: "1.0.0"
    operations: [
        SimpleGet
        PathParamsOnly
        QueryParamsOnly
        MixedParams
        PostWithOptionalFields
    ]
}

// Simple GET with no parameters
@http(method: "GET", uri: "/simple", code: 200)
@readonly
operation SimpleGet {
    input: SimpleGetInput
    output: SimpleGetOutput
}

structure SimpleGetInput {}
structure SimpleGetOutput {
    message: String
}

// Only path parameters
@http(method: "GET", uri: "/users/{userId}/posts/{postId}", code: 200)
@readonly
operation PathParamsOnly {
    input: PathParamsInput
    output: PathParamsOutput
}

structure PathParamsInput {
    @httpLabel
    @required
    userId: String

    @httpLabel
    @required
    postId: String
}

structure PathParamsOutput {
    data: String
}

// Only query parameters
@http(method: "GET", uri: "/search", code: 200)
@readonly
operation QueryParamsOnly {
    input: QueryParamsInput
    output: QueryParamsOutput
}

structure QueryParamsInput {
    @httpQuery("q")
    @required
    query: String

    @httpQuery("limit")
    limit: Integer

    @httpQuery("offset")
    offset: Integer

    @httpQuery("active")
    active: Boolean
}

structure QueryParamsOutput {
    results: String
}

// Mixed: path, query, header, and body
@http(method: "POST", uri: "/organizations/{orgId}/users", code: 201)
operation MixedParams {
    input: MixedParamsInput
    output: MixedParamsOutput
}

structure MixedParamsInput {
    @httpLabel
    @required
    orgId: String

    @httpQuery("notify")
    notify: Boolean

    @httpHeader("X-Request-ID")
    requestId: String

    @httpHeader("X-Idempotency-Key")
    idempotencyKey: String

    @required
    username: String

    @required
    email: String

    role: String
}

structure MixedParamsOutput {
    userId: String
}

// POST with optional and required fields, different types
@http(method: "POST", uri: "/data", code: 200)
operation PostWithOptionalFields {
    input: PostWithOptionalFieldsInput
    output: PostWithOptionalFieldsOutput
}

structure PostWithOptionalFieldsInput {
    @required
    name: String

    count: Integer
    price: Double
    active: Boolean
    tags: StringList
}

structure PostWithOptionalFieldsOutput {
    id: String
}

list StringList {
    member: String
}
