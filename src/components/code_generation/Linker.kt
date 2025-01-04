package components.code_generation

import errors.internal.CompilerError
import util.ExitCode

object Linker {

	fun link(targetTriple: String, objectFilePath: String, executableFilePath: String) {
		val process = if(targetTriple.contains("windows")) {
			// see: https://stackoverflow.com/questions/64413414/unresolved-external-symbol-printf-in-windows-x64-assembly-programming-with-nasm
			ProcessBuilder(
				"lld-link",
				objectFilePath,
				"/out:$executableFilePath",
				"/subsystem:console",
				"/defaultlib:msvcrt",
				"legacy_stdio_definitions.lib"
			).inheritIO().start()
		} else if(targetTriple.contains("linux")) {
			val libraryPath = "/lib/x86_64-linux-gnu"
			// Alternatively using clang:
			// clang -fuse-ld=lld -o out/program.exe out/program.o
			ProcessBuilder(
				"ld.lld",
				"$libraryPath/Scrt1.o",
				"$libraryPath/crti.o",
				"/usr/lib/gcc/x86_64-linux-gnu/12/crtbeginS.o",
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
			).inheritIO().start()
		} else {
			throw CompilerError("No matching linker for target '$targetTriple' found")
		}
		val exitCode = process.waitFor()
		if(exitCode != ExitCode.SUCCESS)
			throw CompilerError("Failed to link object file. Exit code #$exitCode")
	}
}
