package components.code_generation.llvm.wrapper

import code.Main
import components.code_generation.ForeignFunctionInterface
import components.semantic_model.general.Program
import errors.internal.CompilerError
import org.bytedeco.javacpp.BoolPointer
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.FloatPointer
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.llvm.LLVM.LLVMTargetRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM
import util.isRunningOnWindows
import java.io.File

class LlvmProgram(name: String,
				  val targetTriple: String = if(isRunningOnWindows()) "x86_64-unknown-windows-msvc" else "x86_64-unknown-linux-gnu") {
	val constructor = LlvmConstructor(name)

	private var _entrypoint: LLVMValueRef? = null

	var entrypoint: LLVMValueRef
		get() = _entrypoint ?: throw CompilerError("No entrypoint in current build context.")
		set(value) {
			_entrypoint = value
		}

	fun loadSemanticModel(program: Program, entryPointPath: String? = null) {
		constructor.setTargetTriple(targetTriple)
		val unit = program.toUnit()
		entrypoint = unit.compile(constructor, entryPointPath)
		constructor.debug.finish()
		if(Main.shouldWriteIntermediateRepresentation)
			File("out${File.separator}program.ll").printWriter().use { out -> out.print(getIntermediateRepresentation()) }
	}

	fun verify() {
		val error = BytePointer()
		if(LLVM.LLVMVerifyModule(constructor.module, Llvm.ModuleVerificationAction.PRINT, error) != Llvm.OK) {
			LLVM.LLVMDisposeMessage(error)
			//println(getIntermediateRepresentation())
			throw CompilerError("Failed to compile to LLVM target.")
		}
	}

	fun compile() {
		val passManager = LLVM.LLVMCreatePassManager()
		LLVM.LLVMRunPassManager(passManager, constructor.module)
		LLVM.LLVMDisposePassManager(passManager)
	}

	fun writeObjectFileTo(objectFilePath: String) {
		LLVM.LLVMInitializeAllTargetInfos()
		LLVM.LLVMInitializeAllTargets()
		LLVM.LLVMInitializeAllTargetMCs()
		LLVM.LLVMInitializeAllAsmParsers()
		LLVM.LLVMInitializeAllAsmPrinters()
		val target = LLVMTargetRef()
		val error = BytePointer()
		val triplePointer = BytePointer(targetTriple)
		if(LLVM.LLVMGetTargetFromTriple(triplePointer, target, error) != Llvm.OK)
			throw CompilerError("Failed get LLVM target from target triple: ${Llvm.getMessage(error)}")
		val cpu = "generic"
		val features = ""
		val targetMachine =
			LLVM.LLVMCreateTargetMachine(target, targetTriple, cpu, features, Llvm.OptimizationLevel.DEBUGGABLE, LLVM.LLVMRelocPIC,
				LLVM.LLVMCodeModelDefault)
		val dataLayout = LLVM.LLVMCreateTargetDataLayout(targetMachine)
		val dataLayoutPointer = BytePointer(dataLayout)
		LLVM.LLVMSetDataLayout(constructor.module, dataLayoutPointer)
		if(LLVM.LLVMTargetMachineEmitToFile(targetMachine, constructor.module, objectFilePath, LLVM.LLVMObjectFile, error) != Llvm.OK)
			throw CompilerError("Failed to emit object file: ${Llvm.getMessage(error)}")
	}

	fun getIntermediateRepresentation(): String {
		val string = LLVM.LLVMPrintModuleToString(constructor.module)
		return Llvm.getMessage(string)
	}

	fun run(entrypoint: String = Program.GLOBAL_ENTRYPOINT_NAME) {
		LlvmRunner.run(constructor, entrypoint) { address ->
			val functionInterface = ForeignFunctionInterface()
			functionInterface.setSignature(emptyList(), ForeignFunctionInterface.voidType)
			functionInterface.call(address)
		}
	}

	fun runAndReturnBoolean(entrypoint: String = Program.GLOBAL_ENTRYPOINT_NAME): Boolean {
		val result: Boolean
		LlvmRunner.run(constructor, entrypoint) { address ->
			val functionInterface = ForeignFunctionInterface()
			functionInterface.setSignature(emptyList(), ForeignFunctionInterface.booleanType)
			val returnValue = BoolPointer(1)
			functionInterface.call(address, emptyList(), returnValue)
			result = returnValue.get()
		}
		return result
	}

	fun runAndReturnByte(entrypoint: String = Program.GLOBAL_ENTRYPOINT_NAME): Byte {
		val result: Byte
		LlvmRunner.run(constructor, entrypoint) { address ->
			val functionInterface = ForeignFunctionInterface()
			functionInterface.setSignature(emptyList(), ForeignFunctionInterface.signedByteType)
			val returnValue = BytePointer(1L)
			functionInterface.call(address, emptyList(), returnValue)
			result = returnValue.get()
		}
		return result
	}

	fun runAndReturnInt(entrypoint: String = Program.GLOBAL_ENTRYPOINT_NAME): Int {
		val result: Int
		LlvmRunner.run(constructor, entrypoint) { address ->
			val functionInterface = ForeignFunctionInterface()
			functionInterface.setSignature(emptyList(), ForeignFunctionInterface.signedIntegerType)
			val returnValue = IntPointer(1)
			functionInterface.call(address, emptyList(), returnValue)
			result = returnValue.get()
		}
		return result
	}

	fun runAndReturnFloat(entrypoint: String = Program.GLOBAL_ENTRYPOINT_NAME): Float {
		val result: Float
		LlvmRunner.run(constructor, entrypoint) { address ->
			val functionInterface = ForeignFunctionInterface()
			functionInterface.setSignature(emptyList(), ForeignFunctionInterface.floatType)
			val returnValue = FloatPointer(1)
			functionInterface.call(address, emptyList(), returnValue)
			result = returnValue.get()
		}
		return result
	}

	fun dispose() {
		constructor.dispose()
	}
}
