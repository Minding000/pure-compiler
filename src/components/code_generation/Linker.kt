package components.code_generation

import errors.internal.CompilerError
import util.ExitCode

object Linker {

	fun link(targetTriple: String, objectFilePath: String, executableFilePath: String, libraryPaths: List<String> = emptyList()) {
		val processBuilder = if(targetTriple.startsWith("wasm32-")) {
			createWasmLinkingProcess(libraryPaths, objectFilePath, executableFilePath)
		} else if(targetTriple.contains("windows")) {
			createWindowsLinkingProcess(libraryPaths, objectFilePath, executableFilePath)
		} else if(targetTriple.contains("linux")) {
			createLinuxLinkingProcess(libraryPaths, objectFilePath, executableFilePath)
		} else {
			throw CompilerError("No matching linker for target '$targetTriple' found")
		}
		val exitCode = processBuilder.inheritIO().start().waitFor()
		if(exitCode != ExitCode.SUCCESS)
			throw CompilerError("Failed to link object file. Exit code #$exitCode")
	}

	private fun createWasmLinkingProcess(libraryPaths: List<String>, objectFilePath: String, executableFilePath: String): ProcessBuilder {
		//TODO use emcc instead?
		//return ProcessBuilder(
		//	"wasm-ld",
		//	*libraryPaths.toTypedArray(),
		//	objectFilePath,
		//	"-o",
		//	executableFilePath,
		//	"--no-entry",
		//	"--export=main",
		//	"--import-memory"
		//)
		return ProcessBuilder(
			"D:\\Daten\\Temporary\\emsdk\\python\\3.13.3_64bit\\python.exe",
			"D:\\Daten\\Temporary\\emsdk\\upstream\\emscripten\\emcc.py",
			*libraryPaths.toTypedArray(),
			objectFilePath,
			"-o",
			executableFilePath,
			"-sSTANDALONE_WASM=1",
			"-sENVIRONMENT=node",
			"-v"
		)
	}

	private fun createWindowsLinkingProcess(libraryPaths: List<String>, objectFilePath: String, executableFilePath: String): ProcessBuilder {
		// see: https://stackoverflow.com/questions/64413414/unresolved-external-symbol-printf-in-windows-x64-assembly-programming-with-nasm
		return ProcessBuilder(
			"lld-link",
			*libraryPaths.toTypedArray(),
			objectFilePath,
			"/out:$executableFilePath",
			"/subsystem:console",
			"/defaultlib:msvcrt",
			"legacy_stdio_definitions.lib"
		)
	}

	private fun createLinuxLinkingProcess(libraryPaths: List<String>, objectFilePath: String, executableFilePath: String): ProcessBuilder {
		// Alternatively using clang:
		// clang -fuse-ld=lld -o out/program.exe out/program.o
		val libraryPath = "/lib/x86_64-linux-gnu"
		return ProcessBuilder(
			"ld.lld",
			"$libraryPath/Scrt1.o",
			"$libraryPath/crti.o",
			"/usr/lib/gcc/x86_64-linux-gnu/12/crtbeginS.o",
			*libraryPaths.toTypedArray(),
			objectFilePath,
			"-lgcc", "--as-needed",
			"-lgcc_s", "--no-as-needed",
			"-lc", "-lm",
			"-lgcc", "--as-needed",
			"-lgcc_s", "--no-as-needed",
			"/usr/lib/gcc/x86_64-linux-gnu/12/crtendS.o",
			"$libraryPath/crtn.o",
			"-o", executableFilePath,
			"-pie",
			"--hash-style=both",
			"--build-id",
			"--eh-frame-hdr",
			"-m", "elf_x86_64",
			"-dynamic-linker", "/lib64/ld-linux-x86-64.so.2",
			"-L/usr/lib/gcc/x86_64-linux-gnu/12",
			"-L/usr/lib64",
			"-L$libraryPath",
			"-L/lib64",
			"-L/usr/lib/x86_64-linux-gnu",
			"-L/usr/lib64",
			"-L/lib",
			"-L/usr/lib",
		)
	}
}
