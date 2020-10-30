package codegen

import codegen.openapi.models.OpenapiModels.{
  OpenapiDocument,
  OpenapiParameter,
  OpenapiPath,
  OpenapiPathMethod,
  OpenapiResponse,
  OpenapiResponseContent
}
import codegen.openapi.models.OpenapiSchemaType.{OpenapiSchemaArray, OpenapiSchemaRef, OpenapiSchemaString}
import codegen.testutils.CompileCheckTestBase

class EndpointGeneratorSpec extends CompileCheckTestBase {

  it should "generate the endpoint defs" in {
    val doc = OpenapiDocument(
      "",
      null,
      Seq(
        OpenapiPath(
          "test/{asd}",
          Seq(
            OpenapiPathMethod(
              "get",
              Seq(OpenapiParameter("asd", "path", true, None, OpenapiSchemaString(false))),
              Seq(
                OpenapiResponse(
                  "200",
                  "",
                  Seq(OpenapiResponseContent("application/json", OpenapiSchemaArray(OpenapiSchemaString(false), false)))
                ),
                OpenapiResponse("default", "", Seq(OpenapiResponseContent("text/plain", OpenapiSchemaString(false))))
              ),
              None
            )
          )
        )
      ),
      null
    )
    BasicGenerator.imports ++
      new EndpointGenerator().endpointDefs(doc) shouldCompile ()
  }

}
