$version: "2"

namespace example

use alloy#simpleRestJson

@simpleRestJson
service HelloWorldService {
    version: "1.0.0"
    operations: [SayHello]
}

@http(method: "POST", uri: "/hello", code: 200)
operation SayHello {
    input: SayHelloInput
    output: SayHelloOutput
}

structure SayHelloInput {
    @required
    name: String
}

structure SayHelloOutput {
    @required
    greeting: String
}
