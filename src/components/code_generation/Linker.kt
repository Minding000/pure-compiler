package components.code_generation

import errors.internal.CompilerError
import util.ExitCode
import java.util.concurrent.TimeUnit

object Linker {

	fun link(targetTriple: String, objectFilePath: String, executableFilePath: String, libraryPaths: List<String> = emptyList()) {
		val process = if(targetTriple.contains("windows")) {
			// see: https://stackoverflow.com/questions/64413414/unresolved-external-symbol-printf-in-windows-x64-assembly-programming-with-nasm
			ProcessBuilder(
				"lld-link",
				*libraryPaths.toTypedArray(),
				objectFilePath,
				"/out:$executableFilePath",
				"/subsystem:console",
				"/defaultlib:msvcrt",
				"legacy_stdio_definitions.lib"
			).inheritIO().start()
		} else if(targetTriple.contains("linux")) {
			// Alternatively using clang:
			// clang -fuse-ld=lld -o out/program.exe out/program.o
			val libcPath = getLibcPath()
			val libgccPath = getLibgccPath()
			ProcessBuilder(
				"ld.lld",
				"$libcPath/Scrt1.o",
				"$libcPath/crti.o",
				"$libgccPath/crtbeginS.o",
				*libraryPaths.toTypedArray(),
				objectFilePath,
				"-lgcc", "--as-needed",
				"-lgcc_s", "--no-as-needed",
				"-lc", "-lm",
				"-lgcc", "--as-needed",
				"-lgcc_s", "--no-as-needed",
				"$libgccPath/crtendS.o",
				"$libcPath/crtn.o",
				"-o", executableFilePath,
				"-pie",
				"--hash-style=both",
				"--build-id",
				"--eh-frame-hdr",
				"-m", "elf_x86_64",
				"-dynamic-linker", "/lib64/ld-linux-x86-64.so.2",
				"-L$libgccPath",
				"-L/usr/lib64",
				"-L$libcPath",
				"-L/lib64",
				"-L/usr/lib/x86_64-linux-gnu",
				"-L/usr/lib64",
				"-L/lib",
				"-L/usr/lib",
			).inheritIO().start()
		} else {
			throw CompilerError("No matching linker for target '$targetTriple' found")
		}
		val exitCode = process.waitFor()
		if(exitCode != ExitCode.SUCCESS)
			throw CompilerError("Failed to link object file. Exit code #$exitCode")
	}

	private fun getLibcPath(): String {
		val processBuilder = ProcessBuilder("gcc", "-print-file-name=crti.o")
		val process = processBuilder.start()
		val outputStreamReader = process.inputStream.bufferedReader()
		val errorStreamReader = process.errorStream.bufferedReader()
		val timeoutInSeconds = 1L
		val processFuture = process.onExit().completeOnTimeout(null, timeoutInSeconds, TimeUnit.SECONDS)
		val outputStreamBuilder = StringBuilder()
		val errorStreamBuilder = StringBuilder()
		val outputBuffer = CharArray(512)
		while(true) {
			val isDone = processFuture.isDone
			while(outputStreamReader.ready()) {
				val byteCount = outputStreamReader.read(outputBuffer)
				outputStreamBuilder.appendRange(outputBuffer, 0, byteCount)
			}
			while(errorStreamReader.ready()) {
				val byteCount = errorStreamReader.read(outputBuffer)
				errorStreamBuilder.appendRange(outputBuffer, 0, byteCount)
			}
			if(isDone)
				break
			try {
				Thread.sleep(20)
			} catch(_: InterruptedException) {
			}
		}
		val exitCode = processFuture.join()?.exitValue()
		val programFailed = exitCode != 0
		if(exitCode == null) {
			process.waitFor(2, TimeUnit.SECONDS)
			process.destroyForcibly()
			throw CompilerError("Failed to get libc path: timeout")
		} else if(programFailed) {
			throw CompilerError("Failed to get libc path: failed")
		}
		val errorStream = errorStreamBuilder.toString()
		if(errorStream.isNotEmpty())
			throw CompilerError("Failed to get libc path: $errorStream")
		return outputStreamBuilder.toString().replaceAfterLast("/", "").dropLast(1)
	}

	private fun getLibgccPath(): String {
		val processBuilder = ProcessBuilder("gcc", "-print-libgcc-file-name")
		val process = processBuilder.start()
		val outputStreamReader = process.inputStream.bufferedReader()
		val errorStreamReader = process.errorStream.bufferedReader()
		val timeoutInSeconds = 1L
		val processFuture = process.onExit().completeOnTimeout(null, timeoutInSeconds, TimeUnit.SECONDS)
		val outputStreamBuilder = StringBuilder()
		val errorStreamBuilder = StringBuilder()
		val outputBuffer = CharArray(512)
		while(true) {
			val isDone = processFuture.isDone
			while(outputStreamReader.ready()) {
				val byteCount = outputStreamReader.read(outputBuffer)
				outputStreamBuilder.appendRange(outputBuffer, 0, byteCount)
			}
			while(errorStreamReader.ready()) {
				val byteCount = errorStreamReader.read(outputBuffer)
				errorStreamBuilder.appendRange(outputBuffer, 0, byteCount)
			}
			if(isDone)
				break
			try {
				Thread.sleep(20)
			} catch(_: InterruptedException) {
			}
		}
		val exitCode = processFuture.join()?.exitValue()
		val programFailed = exitCode != 0
		if(exitCode == null) {
			process.waitFor(2, TimeUnit.SECONDS)
			process.destroyForcibly()
			throw CompilerError("Failed to get libgcc path: timeout")
		} else if(programFailed) {
			throw CompilerError("Failed to get libgcc path: failed")
		}
		val errorStream = errorStreamBuilder.toString()
		if(errorStream.isNotEmpty())
			throw CompilerError("Failed to get libgcc path: $errorStream")
		return outputStreamBuilder.toString().replaceAfterLast("/", "").dropLast(1)
	}
}
