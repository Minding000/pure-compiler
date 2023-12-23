package projects

import code.Main
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil

internal class CalculatorProject {

	@Disabled @Test
	fun `runs without errors`() {
		TestUtil.recordErrorStream()
		Main.main(arrayOf("run", "D:\\Daten\\Projekte\\Pure\\Example projects\\Calculator\\Main.pure", "Main:CalculatorApp.run"))
		TestUtil.assertErrorStreamEmpty()
	}
}
