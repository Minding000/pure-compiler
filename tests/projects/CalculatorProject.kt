package projects

import code.Main
import org.junit.jupiter.api.Test

internal class CalculatorProject {

	@Test
	fun testParsing() {
		TestUtil.recordErrorStream()
		Main.main(arrayOf("build", "D:\\Daten\\Projekte\\Pure\\Example projects\\Calculator"))
		TestUtil.assertErrorStreamEmpty()
	}
}