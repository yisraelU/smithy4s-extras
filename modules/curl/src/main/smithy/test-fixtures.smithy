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
        PostWithNestedObject
        PostWithDeeplyNestedObject
        PostWithNestedArray
        PostWithComplexNesting
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

// POST with single-level nested object
@http(method: "POST", uri: "/users", code: 201)
operation PostWithNestedObject {
    input: PostWithNestedObjectInput
    output: PostWithNestedObjectOutput
}

structure PostWithNestedObjectInput {
    @required
    name: String

    @required
    address: Address
}

structure Address {
    @required
    street: String

    @required
    city: String

    country: String

    zipCode: String
}

structure PostWithNestedObjectOutput {
    id: String
}

// POST with deeply nested objects (3+ levels)
@http(method: "POST", uri: "/organizations", code: 201)
operation PostWithDeeplyNestedObject {
    input: PostWithDeeplyNestedObjectInput
    output: PostWithDeeplyNestedObjectOutput
}

structure PostWithDeeplyNestedObjectInput {
    @required
    name: String

    @required
    headquarters: Office
}

structure Office {
    @required
    location: Location

    phoneNumber: String
}

structure Location {
    @required
    address: Address

    coordinates: Coordinates
}

structure Coordinates {
    @required
    latitude: Double

    @required
    longitude: Double
}

structure PostWithDeeplyNestedObjectOutput {
    id: String
}

// POST with nested arrays
@http(method: "POST", uri: "/projects", code: 201)
operation PostWithNestedArray {
    input: PostWithNestedArrayInput
    output: PostWithNestedArrayOutput
}

structure PostWithNestedArrayInput {
    @required
    name: String

    @required
    members: MemberList

    tags: StringList
}

structure Member {
    @required
    userId: String

    @required
    role: String

    permissions: StringList
}

list MemberList {
    member: Member
}

structure PostWithNestedArrayOutput {
    id: String
}

// POST with complex nesting (mix of objects, arrays, and primitives)
@http(method: "POST", uri: "/orders", code: 201)
operation PostWithComplexNesting {
    input: PostWithComplexNestingInput
    output: PostWithComplexNestingOutput
}

structure PostWithComplexNestingInput {
    @required
    customer: Customer

    @required
    items: OrderItemList

    @required
    shippingAddress: Address

    billingAddress: Address

    metadata: OrderMetadata
}

structure Customer {
    @required
    id: String

    @required
    name: String

    email: String

    preferences: CustomerPreferences
}

structure CustomerPreferences {
    newsletter: Boolean

    notifications: NotificationSettings
}

structure NotificationSettings {
    email: Boolean

    sms: Boolean

    push: Boolean
}

structure OrderItem {
    @required
    productId: String

    @required
    quantity: Integer

    @required
    price: Double

    options: StringList
}

list OrderItemList {
    member: OrderItem
}

structure OrderMetadata {
    source: String

    campaign: String

    tags: StringList
}

structure PostWithComplexNestingOutput {
    orderId: String
}
