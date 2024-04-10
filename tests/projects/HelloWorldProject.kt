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

	@Test
	fun `prints 'Hello world!'`() {
		Main.main(arrayOf("build", "D:\\Daten\\Projekte\\Pure\\Example projects\\Hello World\\Main.pure", "Main:HelloWorldApp.run"))
		TestUtil.assertExecutablePrints("Hello world!")
	}
}
