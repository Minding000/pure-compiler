package components.code_generation.llvm

import components.semantic_model.general.Program
import source_structure.Project

/**
 * @see: https://github.com/bytedeco/javacpp-presets/tree/master/llvm
 */
object LlvmCompiler {

	fun buildAndRun(project: Project, semanticModel: Program, entryPointPath: String) {
		val program = LlvmProgram(project.name)
		program.loadSemanticModel(semanticModel, entryPointPath)
		program.verify()
		program.compile()
		val result = program.run()
		val intResult = Llvm.castToSignedInteger(result)
		println()
		println("Running program...")
		println("Result: '${intResult}'")
		program.dispose()
	}

	/*
	fun runExampleProgram() {
		initialize()
		// Create context
		val context = LLVMContextCreate()
		val module = LLVMModuleCreateWithNameInContext("test", context)
		val builder = LLVMCreateBuilderInContext(context)
		// Create types
		val i32Type = LLVMInt32TypeInContext(context)
		// Create function signature
		val factorialFunctionType = LLVMFunctionType(i32Type, i32Type, 1, LLVM_NO)
		val factorialFunction = LLVMAddFunction(module, "factorial", factorialFunctionType)
		LLVMSetFunctionCallConv(factorialFunction, LLVMCCallConv)



		// Get values
		val n = LLVMGetParam(factorialFunction, 0)
		val zero = LLVMConstInt(i32Type, 0, LLVM_NO)
		val one = LLVMConstInt(i32Type, 1, LLVM_NO)
		// Define blocks
		val entry = LLVMAppendBasicBlockInContext(context, factorialFunction, "entry")
		val exit = LLVMAppendBasicBlockInContext(context, factorialFunction, "exit")
		val ifFalse = LLVMAppendBasicBlockInContext(context, factorialFunction, "if_false")
		// Define entry block (if condition)
		LLVMPositionBuilderAtEnd(builder, entry)
		val condition = LLVMBuildICmp(builder, LLVMIntEQ, n, zero, "condition = n == 0")
		LLVMBuildCondBr(builder, condition, exit, ifFalse)
		// Define ifFalse block
		LLVMPositionBuilderAtEnd(builder, ifFalse)
		val nMinusOne = LLVMBuildSub(builder, n, one, "nMinusOne = n - 1")
		val arguments = PointerPointer<Pointer>(1).put(0, nMinusOne)
		val factorialResult = LLVMBuildCall(builder, factorialFunction, arguments, 1, "factorialResult = factorial(nMinusOne)")
		val resultIfFalse = LLVMBuildMul(builder, n, factorialResult, "resultIfFalse = n * factorialResult")
		LLVMBuildBr(builder, exit)
		// Define exit block
		LLVMPositionBuilderAtEnd(builder, exit)
		val phi = LLVMBuildPhi(builder, i32Type, "result")
		val phiValues = PointerPointer<Pointer>(2)
			.put(0, one).put(1, resultIfFalse)
		val phiBlocks = PointerPointer<Pointer>(2)
			.put(0, entry).put(1, ifFalse)
		LLVMAddIncoming(phi, phiValues, phiBlocks, 2)
		LLVMBuildRet(builder, phi)
		LLVMDisposeBuilder(builder)



		// Verify module
		verifyModule(module)
		// Create and run pass pipeline
		runPassPipeline(module)
		// Print LLVM IR code
		LLVMDumpModule(module)
		// Create JIT compiler
		run(module) { engine ->
			// Run code
			val argument = LLVMCreateGenericValueOfInt(i32Type, 10, LLVM_NO)
			val result = LLVMRunFunction(engine, factorialFunction, 1, argument)
			val intResult = LLVMGenericValueToInt(result, LLVM_NO)
			println()
			println("Running 'factorial(10)'...")
			println("Result: '${intResult}'")
		}
		// Dispose of resources
		LLVMContextDispose(context)
	}
	*/
}
