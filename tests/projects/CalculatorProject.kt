package projects

import code.Main
import org.junit.jupiter.api.Test
import util.TestUtil

internal class CalculatorProject {

	@Test
	fun `builds without errors`() {
		TestUtil.recordErrorStream()
		Main.main(arrayOf("build", "D:\\Daten\\Projekte\\Pure\\Example projects\\Calculator", "Calculator.Main:CalculatorApp.run"))
		TestUtil.assertErrorStreamEmpty()
	}

	@Test
	fun `runs without errors`() {
		Main.main(arrayOf("build", "D:\\Daten\\Projekte\\Pure\\Example projects\\Calculator", "Calculator.Main:CalculatorApp.run"))
		val expectedOutput = """
				Please enter an equation:
				The result is: 5
			""".trimIndent()
		TestUtil.assertExecutablePrintsLine(expectedOutput, "5" + Character.toString(10))
	}
}
