package projects

import code.Main
import org.junit.jupiter.api.Test
import util.TestUtil
import java.io.File

internal class HelloWorldProject {

	@Test
	fun `builds without errors`() {
		TestUtil.assertErrorStreamEmpty {
			Main.main(
				arrayOf("build", "${TestUtil.EXAMPLE_PROJECTS_PATH}${File.separator}Hello World\\Main.pure", "Main:HelloWorldApp.run"))
		}
	}

	@Test
	fun `prints 'Hello world!'`() {
		Main.main(arrayOf("build", "${TestUtil.EXAMPLE_PROJECTS_PATH}${File.separator}Hello World\\Main.pure", "Main:HelloWorldApp.run"))
		TestUtil.assertExecutablePrintsLine("Hello world!")
	}
}
