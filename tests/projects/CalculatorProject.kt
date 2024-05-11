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
		val newLine = Character.toString(13) + Character.toString(10)
		TestUtil.assertExecutablePrints("Input was: 5" + newLine, "50\n")
	}
}
