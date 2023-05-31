package projects

import code.Main
import org.junit.jupiter.api.Test
import util.TestUtil

internal class SimplestProject {

	@Test
	fun testParsing() {
		TestUtil.recordErrorStream()
		Main.main(arrayOf("build", "D:\\Daten\\Projekte\\Pure\\Example projects\\Simplest\\Main.pure", "Main:SimplestApp.getFive"))
		TestUtil.assertErrorStreamEmpty()
	}
}
