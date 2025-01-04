package projects

import code.Main
import org.junit.jupiter.api.Test
import util.TestUtil
import java.io.File

internal class SimplestProject {

	@Test
	fun `runs without errors`() {
		TestUtil.recordErrorStream()
		Main.main(arrayOf("run", "${TestUtil.EXAMPLE_PROJECTS_PATH}${File.separator}Simplest${File.separator}Main.pure",
			"Main:SimplestApp.getFive"))
		TestUtil.assertErrorStreamEmpty()
	}
}
