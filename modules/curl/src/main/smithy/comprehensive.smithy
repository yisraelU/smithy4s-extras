$version: "2"

namespace comprehensive

use alloy#simpleRestJson

@simpleRestJson
service ApiService {
    version: "1.0.0"
    operations: [
        GetUser
        CreateUser
        UpdateUser
        DeleteUser
        ListUsers
        SearchUsers
    ]
}

// GET with path parameter
@http(method: "GET", uri: "/users/{userId}", code: 200)
@readonly
operation GetUser {
    input: GetUserInput
    output: GetUserOutput
}

structure GetUserInput {
    @httpLabel
    @required
    userId: String
}

structure GetUserOutput {
    @required
    user: User
}

// POST with body
@http(method: "POST", uri: "/users", code: 201)
operation CreateUser {
    input: CreateUserInput
    output: CreateUserOutput
}

structure CreateUserInput {
    @required
    name: String

    @required
    email: String

    age: Integer
}

structure CreateUserOutput {
    @required
    user: User
}

// PUT with path parameter and body
@http(method: "PUT", uri: "/users/{userId}", code: 200)
operation UpdateUser {
    input: UpdateUserInput
    output: UpdateUserOutput
}

structure UpdateUserInput {
    @httpLabel
    @required
    userId: String

    name: String
    email: String
    age: Integer
}

structure UpdateUserOutput {
    @required
    user: User
}

// DELETE with path parameter
@http(method: "DELETE", uri: "/users/{userId}", code: 204)
operation DeleteUser {
    input: DeleteUserInput
    output: DeleteUserOutput
}

structure DeleteUserInput {
    @httpLabel
    @required
    userId: String
}

structure DeleteUserOutput {}

// GET with query parameters
@http(method: "GET", uri: "/users", code: 200)
@readonly
operation ListUsers {
    input: ListUsersInput
    output: ListUsersOutput
}

structure ListUsersInput {
    @httpQuery("limit")
    limit: Integer

    @httpQuery("offset")
    offset: Integer
}

structure ListUsersOutput {
    @required
    users: UserList
}

// GET with query and header parameters
@http(method: "GET", uri: "/users/search", code: 200)
@readonly
operation SearchUsers {
    input: SearchUsersInput
    output: SearchUsersOutput
}

structure SearchUsersInput {
    @httpQuery("q")
    @required
    query: String

    @httpQuery("max_results")
    maxResults: Integer

    @httpHeader("X-Request-ID")
    requestId: String
}

structure SearchUsersOutput {
    @required
    users: UserList
}

structure User {
    @required
    id: String

    @required
    name: String

    @required
    email: String

    age: Integer
}

list UserList {
    member: User
}
