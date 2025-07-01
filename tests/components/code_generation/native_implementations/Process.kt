package components.code_generation.native_implementations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.ExitCode
import util.TestApp

internal class Process {

	@Test
	fun `initializes environment variable map`() {
		val sourceCode = """
			SimplestApp object {
				bound Process object {
					val environmentVariables = getEnvironmentVariables()
					native to getEnvironmentVariables(): <String, String>Map
				}
				to getNumberOfEnvironmentVariables(): Int {
					return Process.environmentVariables.size
				}
			}
			native ByteArray class {
				val size: Int
			}
			copied String class {
				var bytes: ByteArray
				init(bytes)
			}
			Map class {
				containing Key, Value
				var size = 0
				operator [key: Key](value: Value) {
					size++
				}
			}
		""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.getNumberOfEnvironmentVariables")
		app.setSpecialTypeDeclarations(SpecialType.MAP, SpecialType.BYTE_ARRAY, SpecialType.STRING)
		val environmentVariables = mapOf("SYSTEMROOT" to "C:\\WINDOWS", "A" to "a", "B" to "b", "C" to "c", "D" to "d")
		app.shouldExitWith(environmentVariables.size, environmentVariables)
	}

	@Test
	fun `splits environment strings into keys and values`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object: Application {
				to printC() {
					Process.outputStream.writeBytes(Process.environmentVariables["C"].bytes)
				}
			}
		""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.printC")
		app.includeRequiredModules = true
		val environmentVariables = mapOf("A" to "a", "B" to "b", "C" to "c", "D" to "d")
		app.shouldPrint("c", "", ExitCode.SUCCESS, environmentVariables)
	}

	@Test
	fun `initializes program argument array`() {
		val sourceCode = """
			SimplestApp object {
				bound Process object {
					val arguments = getArguments()
					native to getArguments(): <String>Array
				}
				to getNumberOfArguments(): Int {
					return Process.arguments.size
				}
			}
			native ByteArray class {
				val size: Int
			}
			copied String class {
				var bytes: ByteArray
				init(bytes)
			}
			native Array class {
				containing Element
				val size: Int
			}
		""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.getNumberOfArguments")
		app.setSpecialTypeDeclarations(SpecialType.ARRAY, SpecialType.BYTE_ARRAY, SpecialType.STRING)
		val programArguments = listOf("I", "II")
		app.shouldExitWith(programArguments.size + 1, programArguments)
	}
}
