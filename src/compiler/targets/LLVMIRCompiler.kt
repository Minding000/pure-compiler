package compiler.targets

import org.bytedeco.javacpp.*
import org.bytedeco.llvm.LLVM.LLVMExecutionEngineRef
import org.bytedeco.llvm.LLVM.LLVMMCJITCompilerOptions
import org.bytedeco.llvm.global.LLVM.*

/**
 * @see: https://github.com/bytedeco/javacpp-presets/tree/master/llvm
 */
object LLVMIRCompiler {

	fun compile() {
		initialize()
		buildExampleProgram()
	}

	private fun initialize() {
		LLVMInitializeCore(LLVMGetGlobalPassRegistry())
		LLVMLinkInMCJIT()
		LLVMInitializeNativeAsmPrinter()
		LLVMInitializeNativeAsmParser()
		LLVMInitializeNativeTarget()
	}

	private fun buildExampleProgram() {
		val error = BytePointer()
		// Create context
		val context = LLVMContextCreate()
		val module = LLVMModuleCreateWithNameInContext("test", context)
		val builder = LLVMCreateBuilderInContext(context)
		// Create types
		val i32Type = LLVMInt32TypeInContext(context)
		// Create function signature
		val factorialFunctionType = LLVMFunctionType(i32Type, i32Type, 1, 0)
		val factorialFunction = LLVMAddFunction(module, "factorial", factorialFunctionType)
		LLVMSetFunctionCallConv(factorialFunction, LLVMCCallConv)
		// Get values
		val n = LLVMGetParam(factorialFunction, 0)
		val zero = LLVMConstInt(i32Type, 0, 0)
		val one = LLVMConstInt(i32Type, 1, 0)
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
		// Verify module
		if(LLVMVerifyModule(module, LLVMPrintMessageAction, error) != 0) {
			LLVMDisposeMessage(error)
			return
		}
		// Create and run pass pipeline
		val passManager = LLVMCreatePassManager()
		LLVMAddAggressiveInstCombinerPass(passManager)
		LLVMAddNewGVNPass(passManager)
		LLVMAddCFGSimplificationPass(passManager)
		LLVMRunPassManager(passManager, module)
		// Print LLVM IR code
		LLVMDumpModule(module)
		// Create JIT compiler
		val engine = LLVMExecutionEngineRef()
		val options = LLVMMCJITCompilerOptions()
		if(LLVMCreateMCJITCompilerForModule(engine, module, options, 3, error) != 0) {
			System.err.println("Failed to create JIT compiler: $error")
			LLVMDisposeMessage(error)
			return
		}
		// Run code
		val argument = LLVMCreateGenericValueOfInt(i32Type, 10, 0)
		val result = LLVMRunFunction(engine, factorialFunction, 1, argument)
		val intResult = LLVMGenericValueToInt(result, 0)
		println()
		println("Running 'factorial(10)'...")
		println("Result: '${intResult}'")
		// Dispose of resources
		LLVMDisposeExecutionEngine(engine)
		LLVMDisposePassManager(passManager)
		LLVMDisposeBuilder(builder)
		LLVMContextDispose(context)
	}
}