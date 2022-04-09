package projects

import code.Main
import org.junit.jupiter.api.Test

internal class HelloWorldProject {

	@Test
	fun testParsing() {
		TestUtil.recordErrorStream()
		Main.main(arrayOf("build", "D:\\Daten\\Projekte\\Pure\\Example projects\\Hello World\\Main.pure"))
		TestUtil.assertErrorStreamEmpty()
	}
}