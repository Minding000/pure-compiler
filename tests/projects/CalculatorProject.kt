package projects

import code.Main
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class CalculatorProject {

	@Disabled @Test
	fun testParsing() {
		TestUtil.recordErrorStream()
		Main.main(arrayOf("build", "D:\\Daten\\Projekte\\Pure\\Example projects\\Calculator"))
		TestUtil.assertErrorStreamEmpty()
	}
}