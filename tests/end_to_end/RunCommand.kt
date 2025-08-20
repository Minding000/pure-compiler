package end_to_end

import code.Main
import org.junit.jupiter.api.Test
import util.TestUtil
import java.io.File

internal class RunCommand {

	@Test
	fun `runs application in memory without errors`() {
		TestUtil.assertErrorStreamEmpty {
			Main.main(arrayOf("run", "${TestUtil.EXAMPLE_PROJECTS_PATH}run_command${File.separator}Main.pure",
				"Main:SimplestApp.getFive"))
		}
	}
}
