package projects

import code.Main
import org.junit.jupiter.api.Test
import util.TestUtil

internal class HelloWorldProject {

	@Test
	fun `builds without errors`() {
		TestUtil.recordErrorStream()
		Main.main(arrayOf("build", "D:\\Daten\\Projekte\\Pure\\Example projects\\Hello World\\Main.pure", "Main:HelloWorldApp.run"))
		TestUtil.assertErrorStreamEmpty()
	}

	//TODO check why this is crashing
	@Test
	fun `runs without errors`() {
		TestUtil.recordErrorStream()
		Main.main(arrayOf("run", "D:\\Daten\\Projekte\\Pure\\Example projects\\Hello World\\Main.pure", "Main:HelloWorldApp.run"))
		TestUtil.assertErrorStreamEmpty()
	}
}
