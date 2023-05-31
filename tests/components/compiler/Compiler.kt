package components.compiler

import components.compiler.targets.llvm.Llvm
import components.compiler.targets.llvm.LlvmCompilerContext
import components.compiler.targets.llvm.LlvmList
import components.compiler.targets.llvm.LlvmType
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class Compiler {

	@Test
	fun `is able to assemble and run test program`() {
		val expectedResult = 5L
		val context = LlvmCompilerContext("Test")
		val argumentTypes = LlvmList<LlvmType>(0)
		val functionType = Llvm.buildFunctionType(argumentTypes, 0, context.i32Type)
		val function = Llvm.buildFunction(context, "getNumber", functionType)
		Llvm.createBlock(context, function, "body")
		val number = Llvm.buildInt32(context, expectedResult)
		Llvm.buildReturn(context, number)
		context.entrypoint = function
		context.verify()
		context.compile()
		val result = context.run()
		val intResult = Llvm.castToInt(result)
		context.close()
		assertEquals(expectedResult, intResult)
	}

	@Test
	fun `is able run source code`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to getFive(): Int {
					return 5
				}
			}
		""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val expectedResult = 5L
		val context = LlvmCompilerContext("Test")
		context.loadSemanticModel(lintResult.program, "Test:SimplestApp.getFive")
		context.verify()
		context.compile()
		val result = context.run()
		val intResult = Llvm.castToInt(result)
		context.close()
		assertEquals(expectedResult, intResult)
	}
}
