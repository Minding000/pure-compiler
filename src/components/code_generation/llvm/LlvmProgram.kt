package components.code_generation.llvm

import components.semantic_model.general.Program
import errors.internal.CompilerError
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMTargetRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*
import util.ExitCode

class LlvmProgram(name: String) {
	val targetTriple = "x86_64-pc-windows"
	val constructor = LlvmConstructor(name)

	private var _entrypoint: LLVMValueRef? = null

	var entrypoint: LLVMValueRef
		get() = _entrypoint ?: throw CompilerError("No entrypoint in current build context.")
		set(value) {
			_entrypoint = value
		}

	fun loadSemanticModel(program: Program, entryPointPath: String? = null) {
		constructor.setTargetTriple(targetTriple)
		entrypoint = program.compile(constructor, entryPointPath)
		constructor.debug.finish()
	}

	fun verify() {
		val error = BytePointer()
		if(LLVMVerifyModule(constructor.module, LLVMPrintMessageAction, error) != Llvm.OK) {
			LLVMDisposeMessage(error)
			throw CompilerError("Failed to compile to LLVM target.")
		}
	}

	fun compile() {
		val passManager = LLVMCreatePassManager()
		LLVMRunPassManager(passManager, constructor.module)
		LLVMDisposePassManager(passManager)
	}

	fun writeTo(path: String) {
		val objectFilePath = "${path}\\program.o"
		val executableFilePath = "${path}\\program.exe"

		LLVMInitializeAllTargetInfos()
		LLVMInitializeAllTargets()
		LLVMInitializeAllTargetMCs()
		LLVMInitializeAllAsmParsers()
		LLVMInitializeAllAsmPrinters()
		val target = LLVMTargetRef()
		val error = BytePointer()
		val triplePointer = BytePointer(targetTriple)
		if(LLVMGetTargetFromTriple(triplePointer, target, error) != Llvm.OK) {
			LLVMDisposeMessage(error)
			throw CompilerError("Failed get LLVM target from target triple.")
		}
		val cpu = "generic"
		val features = ""
		val targetMachine = LLVMCreateTargetMachine(target, targetTriple, cpu, features, Llvm.OptimizationLevel.DEBUGGABLE, LLVMRelocPIC,
			LLVMCodeModelDefault)
		val dataLayout = LLVMCreateTargetDataLayout(targetMachine)
		val dataLayoutPointer = BytePointer(dataLayout)
		LLVMSetDataLayout(constructor.module, dataLayoutPointer)
		if(LLVMTargetMachineEmitToFile(targetMachine, constructor.module, objectFilePath, LLVMObjectFile, error) != Llvm.OK) {
			LLVMDisposeMessage(error)
			throw CompilerError("Failed get LLVM target from target triple.")
		}
		// see: https://stackoverflow.com/questions/64413414/unresolved-external-symbol-printf-in-windows-x64-assembly-programming-with-nasm
		val process = ProcessBuilder("D:\\Programme\\LLVM\\bin\\lld-link.exe", objectFilePath, "/out:$executableFilePath",
			"/subsystem:console", "/defaultlib:msvcrt", "legacy_stdio_definitions.lib").inheritIO().start()
		val exitCode = process.onExit().join().exitValue()
		if(exitCode != ExitCode.SUCCESS)
			throw CompilerError("Failed to link object file. Exit code #$exitCode")
	}

	fun getIntermediateRepresentation(): String {
		val bytes = LLVMPrintModuleToString(constructor.module)
		val message = bytes.string
		LLVMDisposeMessage(bytes)
		return message
	}

	fun run(): LlvmGenericValue {
		var result: LlvmGenericValue
		LlvmEngine.run(constructor.module) { engine ->
			result = Llvm.runFunction(engine, entrypoint)
		}
		return result
	}

	fun dispose() {
		constructor.dispose()
	}
}
