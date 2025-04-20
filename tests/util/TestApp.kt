package util

import code.Builder
import components.code_generation.Linker
import components.semantic_model.context.SpecialType
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.test.assertEquals

class TestApp(private val files: Map<String, String>, private val entryPointPath: String = "") {
	private var specialTypePaths: Map<SpecialType, List<String>> = Builder.specialTypePaths
	var includeRequiredModules: Boolean = false

	companion object {
		var testProgramCounter = AtomicLong()
	}

	constructor(sourceCode: String, entryPointPath: String = ""): this(mapOf(TestUtil.TEST_FILE_NAME to sourceCode), entryPointPath)

	fun setSpecialTypeDeclarations(vararg types: SpecialType) = setSpecialTypeDeclarations(types.toList())
	fun setSpecialTypeDeclarations(types: List<SpecialType>, source: String = TestUtil.TEST_FILE_NAME) {
		specialTypePaths = types.associateWith { listOf(source) }
	}

	fun shouldExitWith(expectedValue: Int) {
		TestUtil.run(files, entryPointPath, includeRequiredModules, specialTypePaths) { program ->
			val result = program.runAndReturnInt()
			assertEquals(expectedValue, result, "The test app exited with an unexpected value.")
		}
	}

	fun shouldExitWith(expectedValue: Float) {
		TestUtil.run(files, entryPointPath, includeRequiredModules, specialTypePaths) { program ->
			val result = program.runAndReturnFloat()
			assertEquals(expectedValue, result, "The test app exited with an unexpected value.")
		}
	}

	fun shouldExitWith(expectedValue: Int, input: String) {
		shouldPrint("", input, expectedValue)
	}

	fun shouldPrintLine(expectedOutput: String, input: String = "", expectedExitCode: Int = ExitCode.SUCCESS) {
		shouldPrint("$expectedOutput\n", input, expectedExitCode)
	}

	fun shouldPrint(expectedOutput: String, input: String = "", expectedExitCode: Int = ExitCode.SUCCESS) {
		TestUtil.run(files, entryPointPath, includeRequiredModules, specialTypePaths) { program ->
			val basePath = ".${File.separator}out${File.separator}tests"
			val id = testProgramCounter.getAndIncrement()
			val objectFilePath = "${basePath}${File.separator}${id}.o"
			val executablePath = "${basePath}${File.separator}${id}.exe"
			try {
				program.writeObjectFileTo(objectFilePath)
				Linker.link(program.targetTriple, objectFilePath, executablePath)
				TestUtil.assertExecutablePrints(expectedOutput, input, executablePath, expectedExitCode)
			} finally {
				Path(objectFilePath).deleteIfExists()
				Path(executablePath).deleteIfExists()
			}
		}
	}
}
