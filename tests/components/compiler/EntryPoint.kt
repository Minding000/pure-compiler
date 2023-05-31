package components.compiler

import errors.user.UserError
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import util.TestUtil

internal class EntryPoint {

	@Disabled("Global functions are not implemented yet")
	@Test
	fun `works with global functions`() {
		val sourceCode = """
			to run() {}
		""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		assertDoesNotThrow {
			lintResult.program.getEntryPoint("Test:run")
		}
	}

	@Test
	fun `works with member functions`() {
		val sourceCode = """
			Main object {
				to run() {}
			}
		""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		assertDoesNotThrow {
			lintResult.program.getEntryPoint("Test:Main.run")
		}
	}

	@Test
	fun `detects malformed entry point path`() {
		val lintResult = TestUtil.lint("")
		assertThrows<UserError>("Malformed entry point path '123'.") {
			lintResult.program.getEntryPoint("123")
		}
	}

	@Test
	fun `handles missing file name`() {
		val lintResult = TestUtil.lint("")
		assertThrows<UserError>("File '' not found.") {
			lintResult.program.getEntryPoint(":run")
		}
	}

	@Test
	fun `handles missing function name`() {
		val lintResult = TestUtil.lint("")
		assertThrows<UserError>("Function '' not found.") {
			lintResult.program.getEntryPoint("Main:")
		}
	}

	@Test
	fun `detects nonexistent files`() {
		val lintResult = TestUtil.lint("")
		assertThrows<UserError>("File 'Nonexistent' not found.") {
			lintResult.program.getEntryPoint("Nonexistent:main")
		}
	}

	@Test
	fun `detects nonexistent objects`() {
		val lintResult = TestUtil.lint("")
		assertThrows<UserError>("Object 'Main' not found.") {
			lintResult.program.getEntryPoint("Test:Main.run")
		}
	}

	@Disabled("Global functions are not implemented yet")
	@Test
	fun `detects nonexistent global functions`() {
		val lintResult = TestUtil.lint("")
		assertThrows<UserError>("Function 'run' not found.") {
			lintResult.program.getEntryPoint("Test:run")
		}
	}

	@Test
	fun `detects nonexistent member functions`() {
		val sourceCode = """
			Main object
		""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		assertThrows<UserError>("Function 'run' not found.") {
			lintResult.program.getEntryPoint("Test:Main.run")
		}
	}
}
