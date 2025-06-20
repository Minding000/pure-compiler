package projects

import code.Main
import org.junit.jupiter.api.Test
import util.TestUtil
import java.io.File

internal class CalculatorProject {

	@Test
	fun `builds without errors`() {
		TestUtil.assertErrorStreamEmpty {
			Main.main(arrayOf("build", "${TestUtil.EXAMPLE_PROJECTS_PATH}${File.separator}Calculator", "Calculator.Main:CalculatorApp.run"))
		}
	}

	@Test
	fun `runs without errors`() {
		Main.main(arrayOf("build", "${TestUtil.EXAMPLE_PROJECTS_PATH}${File.separator}Calculator", "Calculator.Main:CalculatorApp.run"))
		val expectedOutput = """
				Please enter an equation:
				The result is: 9
			""".trimIndent()
		TestUtil.assertExecutablePrintsLine(expectedOutput, "3 * 3" + Character.toString(10))
	}
}
