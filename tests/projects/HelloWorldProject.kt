package projects

import code.Main
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil

internal class HelloWorldProject {

	@Disabled @Test
	fun testParsing() {
		TestUtil.recordErrorStream()
		Main.main(arrayOf("build", "D:\\Daten\\Projekte\\Pure\\Example projects\\Hello World\\Main.pure"))
		TestUtil.assertErrorStreamEmpty()
	}
}