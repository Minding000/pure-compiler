package components.compiler

import components.compiler.targets.llvm.Llvm
import components.compiler.targets.llvm.LlvmContext
import components.compiler.targets.llvm.LlvmGenericValueReference
import components.compiler.targets.llvm.LlvmList
import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.global.LLVM
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class Compiler {

	@Test
	fun `is able to assemble and run test program`() {
		val expectedResult = 5L
		var actualResult: Long? = null
		val context = LlvmContext("Test")
		val argumentTypes = PointerPointer<Pointer>(0)
		val functionType = LLVM.LLVMFunctionType(context.i32Type, argumentTypes, 0, Llvm.NO)
		val function = LLVM.LLVMAddFunction(context.module, "getNumber", functionType)
		context.entrypoint = function
		LLVM.LLVMSetFunctionCallConv(function, LLVM.LLVMCCallConv)
		val body = LLVM.LLVMAppendBasicBlockInContext(context.context, function, "body")
		LLVM.LLVMPositionBuilderAtEnd(context.builder, body)
		val number = LLVM.LLVMConstInt(context.i32Type, expectedResult, Llvm.NO)
		LLVM.LLVMBuildRet(context.builder, number)
		context.verify()
		context.compile()
		context.run { engine ->
			val arguments = PointerPointer<Pointer>(0)
			val result = LLVM.LLVMRunFunction(engine, context.entrypoint, 0, arguments)
			actualResult = LLVM.LLVMGenericValueToInt(result, Llvm.NO)
		}
		context.close()
		assertEquals(expectedResult, actualResult)
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
		var actualResult: Long? = null
		val context = LlvmContext("Test")
		context.loadSemanticModel(lintResult.program, "Test:SimplestApp.getFive")
		context.verify()
		context.compile()
		context.run { engine ->
			val arguments = LlvmList<LlvmGenericValueReference>(0)
			val result = LLVM.LLVMRunFunction(engine, context.entrypoint, 0, arguments)
			actualResult = LLVM.LLVMGenericValueToInt(result, Llvm.NO)
		}
		context.close()
		assertEquals(expectedResult, actualResult)
	}
}
