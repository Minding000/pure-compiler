package components.compiler

import components.compiler.targets.llvm.BuildContext
import components.compiler.targets.llvm.LLVMIRCompiler
import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.global.LLVM
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class Compiler {

	@Test
	fun `is able to assemble and run test program`() {
		val expectedResult = 5L
		var actualResult: Long? = null
		val context = BuildContext("Test")
		val argumentTypes = PointerPointer<Pointer>(0)
		val functionType = LLVM.LLVMFunctionType(context.i32Type, argumentTypes, 0, LLVMIRCompiler.LLVM_NO)
		val function = LLVM.LLVMAddFunction(context.module, "getNumber", functionType)
		context.entrypoint = function
		LLVM.LLVMSetFunctionCallConv(function, LLVM.LLVMCCallConv)
		val five = LLVM.LLVMConstInt(context.i32Type, expectedResult, LLVMIRCompiler.LLVM_NO)
		val body = LLVM.LLVMAppendBasicBlockInContext(context.context, function, "body")
		LLVM.LLVMPositionBuilderAtEnd(context.builder, body)
		LLVM.LLVMBuildRet(context.builder, five)
		context.verify()
		context.compile()
		context.run { engine ->
			val arguments = PointerPointer<Pointer>(0)
			val result = LLVM.LLVMRunFunction(engine, context.entrypoint, 0, arguments)
			actualResult = LLVM.LLVMGenericValueToInt(result, LLVMIRCompiler.LLVM_NO)
		}
		context.close()
		assertEquals(expectedResult, actualResult)
	}
}
