package end_to_end

import code.Main
import org.junit.jupiter.api.Test
import util.TestUtil

internal class MultipleFiles {

	@Test
	fun `builds project with multiple source files without errors`() {
		TestUtil.assertErrorStreamEmpty {
			Main.main(arrayOf("build", "${TestUtil.EXAMPLE_PROJECTS_PATH}multiple_files", "multiple_files.Main:CalculatorApp.run"))
		}
	}

	@Test
	fun `runs project with multiple source files without errors`() {
		Main.main(arrayOf("build", "${TestUtil.EXAMPLE_PROJECTS_PATH}multiple_files", "multiple_files.Main:CalculatorApp.run"))
		val expectedOutput = """
				Please enter an equation:
				The result is: 9
			""".trimIndent()
		TestUtil.assertExecutablePrintsLine(expectedOutput, "3 * 3" + Character.toString(10))
	}
}
