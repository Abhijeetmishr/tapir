# One-of mappings

Outputs with multiple variants can be specified using the `oneOf` output. Each variant  is defined using a one-of 
mapping. All possible outputs must have a common supertype. Typically, the supertype is a sealed trait, and the mappings 
are implementing case classes.

Each one-of mapping needs an `appliesTo` function to determine at run-time, if the variant should be used for a given 
value. This function is inferred at compile time when using `oneOfMapping`, but can also be provided by hand, or if
the compile-time inference fails, using one of the other factory methods (see below). A catch-all mapping can be defined
using `oneOfDefaultMapping`, and should be placed as the last mapping in the list of possible variants.

When encoding such an output to a response, the first matching output is chosen, using the following rules:
1. the mappings `appliesTo` method, applied to the output value (as returned by the server logic) must return `true`.
2. when a fixed content type is specified by the output, it must match the request's `Accept` header (if present). 
   This implements content negotiation.

When decoding from a response, the first output which decodes successfully is chosen.

The outputs might vary in status codes, headers (e.g. different content types), and body implementations. However, for 
bodies, only replayable ones can be used, and they need to have the same raw representation (e.g. all byte-array-base, 
or all file-based).

Note that exhaustiveness of the mappings (that all subtypes of `T` are covered) is not checked.

For example, below is a specification for an endpoint where the error output is a sealed trait `ErrorInfo`; 
such a specification can then be refined and reused for other endpoints:

```scala
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import sttp.model.StatusCode
import io.circe.generic.auto._

sealed trait ErrorInfo
case class NotFound(what: String) extends ErrorInfo
case class Unauthorized(realm: String) extends ErrorInfo
case class Unknown(code: Int, msg: String) extends ErrorInfo
case object NoContent extends ErrorInfo

// here we are defining an error output, but the same can be done for regular outputs
val baseEndpoint = endpoint.errorOut(
  oneOf[ErrorInfo](
    oneOfMapping(statusCode(StatusCode.NotFound).and(jsonBody[NotFound].description("not found"))),
    oneOfMapping(statusCode(StatusCode.Unauthorized.and(jsonBody[Unauthorized].description("unauthorized")))),
    oneOfMapping(statusCode(StatusCode.NoContent.and(emptyOutputAs(NoContent)))),
    oneOfDefaultMapping(jsonBody[Unknown].description("unknown"))
  )
)
```

## One-of-mapping and type erasure

Type erasure may prevent a one-of-mapping from working properly. The following example will fail at compile time because `Right[NotFound]` and `Right[BadRequest]` will become `Right[Any]`:

```scala
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import sttp.model.StatusCode
import io.circe.generic.auto._

case class ServerError(what: String)

sealed trait UserError
case class BadRequest(what: String) extends UserError
case class NotFound(what: String) extends UserError

val baseEndpoint = endpoint.errorOut(
  oneOf[Either[ServerError, UserError]](
    oneOfMapping(StatusCode.NotFound, jsonBody[Right[ServerError, NotFound]].description("not found")),
    oneOfMapping(StatusCode.BadRequest, jsonBody[Right[ServerError, BadRequest]].description("unauthorized")),
    oneOfMapping(StatusCode.InternalServerError, jsonBody[Left[ServerError, UserError]].description("unauthorized")),
  )
)
// error: Constructing oneOfMapping of type scala.util.Right[repl.MdocSession.App.ServerError,repl.MdocSession.App.NotFound] is not allowed because of type erasure. Using a runtime-class-based check it isn't possible to verify that the input matches the desired class. Please use oneOfMappingClassMatcher, oneOfMappingValueMatcher or oneOfMappingFromMatchType instead
//     oneOfMappingValueMatcher(StatusCode.NotFound, jsonBody[Right[ServerError, NotFound]].description("not found")) {
//                             ^
// error: Constructing oneOfMapping of type scala.util.Right[repl.MdocSession.App.ServerError,repl.MdocSession.App.BadRequest] is not allowed because of type erasure. Using a runtime-class-based check it isn't possible to verify that the input matches the desired class. Please use oneOfMappingClassMatcher, oneOfMappingValueMatcher or oneOfMappingFromMatchType instead
//     oneOfMappingValueMatcher(StatusCode.BadRequest, jsonBody[Right[ServerError, BadRequest]].description("unauthorized")) {
//                             ^
// error: Constructing oneOfMapping of type scala.util.Left[repl.MdocSession.App.ServerError,repl.MdocSession.App.UserError] is not allowed because of type erasure. Using a runtime-class-based check it isn't possible to verify that the input matches the desired class. Please use oneOfMappingClassMatcher, oneOfMappingValueMatcher or oneOfMappingFromMatchType instead
//     oneOfMappingValueMatcher(StatusCode.InternalServerError, jsonBody[Left[ServerError, UserError]].description("unauthorized")) {
//                             ^
```

The solution is therefore to handwrite a function checking that a value (of type `Any`) is of the correct type:


```scala
val baseEndpoint = endpoint.errorOut(
  oneOf[Either[ServerError, UserError]](
    oneOfMappingValueMatcher(StatusCode.NotFound, jsonBody[Right[ServerError, NotFound]].description("not found")) {
      case Right(NotFound(_)) => true
    },
    oneOfMappingValueMatcher(StatusCode.BadRequest, jsonBody[Right[ServerError, BadRequest]].description("unauthorized")) {
      case Right(BadRequest(_)) => true
    },
    oneOfMappingValueMatcher(StatusCode.InternalServerError, jsonBody[Left[ServerError, UserError]].description("unauthorized")) {
      case Left(ServerError(_)) => true
    }
  )
)
```

Of course, you could use `oneOfMappingValueMatcher` to do runtime filtering for other purpose than solving type erasure.

In the case of solving type erasure, writing by hand partial function to match value against composition of case class and sealed trait can be repetitive.
To make that more easy, we provide an **experimental** typeclass - `MatchType` - so you can automatically derive that partial function:

```scala
import sttp.tapir.typelevel.MatchType

val baseEndpoint = endpoint.errorOut(
  oneOf[Either[ServerError, UserError]](
    oneOfMappingFromMatchType(StatusCode.NotFound, jsonBody[Right[ServerError, NotFound]].description("not found")),
    oneOfMappingFromMatchType(StatusCode.BadRequest, jsonBody[Right[ServerError, BadRequest]].description("unauthorized")),
    oneOfMappingFromMatchType(StatusCode.InternalServerError, jsonBody[Left[ServerError, UserError]].description("unauthorized"))
  )
)
```

## Next

Read on about [codecs](codecs.md).
