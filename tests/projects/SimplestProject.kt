package projects

import code.Main
import org.junit.jupiter.api.Test
import util.TestUtil

internal class SimplestProject {

	@Test
	fun `runs without errors`() {
		TestUtil.recordErrorStream()
		Main.main(arrayOf("run", "D:\\Daten\\Projekte\\Pure\\Example projects\\Simplest\\Main.pure", "Main:SimplestApp.getFive"))
		TestUtil.assertErrorStreamEmpty()
	}
}
