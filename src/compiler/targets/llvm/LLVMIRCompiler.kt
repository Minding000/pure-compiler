package compiler.targets.llvm

import linter.elements.general.Program
import org.bytedeco.javacpp.*
import org.bytedeco.llvm.LLVM.LLVMExecutionEngineRef
import org.bytedeco.llvm.LLVM.LLVMMCJITCompilerOptions
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.global.LLVM.*

/**
 * @see: https://github.com/bytedeco/javacpp-presets/tree/master/llvm
 */
object LLVMIRCompiler {
	val LLVM_NO = 0
	val LLVM_YES = 1
	val LLVM_OK = 0

	fun compile(program: Program) {
		initialize()
		build(program)
	}

	private fun initialize() {
		LLVMInitializeCore(LLVMGetGlobalPassRegistry())
		LLVMLinkInMCJIT()
		LLVMInitializeNativeAsmPrinter()
		LLVMInitializeNativeAsmParser()
		LLVMInitializeNativeTarget()
	}

	private fun build(program: Program) {
		// Compile program
		val context = BuildContext("test")
		program.compile(context)
		/*
		// Create function signature
		val argumentTypes = PointerPointer<Pointer>(0)
		val functionType = LLVMFunctionType(i32Type, argumentTypes, 0, LLVM_NO) // FunctionDefinition
		val function = LLVMAddFunction(module, "getFive", functionType)
		LLVMSetFunctionCallConv(function, LLVMCCallConv)
		// Get values
		val five = LLVMConstInt(i32Type, 5, LLVM_NO) // NumberLiteral
		// Define block
		val body = LLVMAppendBasicBlockInContext(context, function, "body") // FunctionDefinition
		LLVMPositionBuilderAtEnd(builder, body)
		LLVMBuildRet(builder, five) // ReturnStatement
		*/
		// Verify module
		verifyModule(context.module)
		// Create and run pass pipeline
		runPassPipeline(context.module)
		// Print LLVM IR code
		LLVMDumpModule(context.module)
		// Create JIT compiler
		run(context.module) { engine ->
			// Run code
			val arguments = PointerPointer<Pointer>(0)
			val result = LLVMRunFunction(engine, context.entrypoint, 0, arguments)
			val intResult = LLVMGenericValueToInt(result, LLVM_NO)
			println()
			println("Running 'getFive()'...")
			println("Result: '${intResult}'")
		}
		context.close()
	}

	private fun verifyModule(module: LLVMModuleRef) {
		val error = BytePointer()
		if(LLVMVerifyModule(module, LLVMPrintMessageAction, error) != LLVM_OK) {
			LLVMDisposeMessage(error)
			return
		}
	}

	private fun runPassPipeline(module: LLVMModuleRef) {
		val passManager = LLVMCreatePassManager()
		LLVMAddAggressiveInstCombinerPass(passManager)
		LLVMAddNewGVNPass(passManager)
		LLVMAddCFGSimplificationPass(passManager)
		LLVMRunPassManager(passManager, module)
		LLVMDisposePassManager(passManager)
	}

	private fun run(module: LLVMModuleRef, runner: (engine: LLVMExecutionEngineRef) -> Unit) {
		val engine = LLVMExecutionEngineRef()
		val options = LLVMMCJITCompilerOptions()
		val error = BytePointer()
		if(LLVMCreateMCJITCompilerForModule(engine, module, options, 3, error) != LLVM_OK) {
			System.err.println("Failed to create JIT compiler: $error")
			LLVMDisposeMessage(error)
			return
		}
		runner(engine)
		LLVMDisposeExecutionEngine(engine)
	}

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
}