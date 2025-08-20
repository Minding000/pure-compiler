package end_to_end

import code.Main
import org.junit.jupiter.api.Test
import util.TestUtil
import java.io.File

internal class OutputStream {

	@Test
	fun `builds project using the output stream without errors`() {
		TestUtil.assertErrorStreamEmpty {
			Main.main(
				arrayOf("build", "${TestUtil.EXAMPLE_PROJECTS_PATH}output_stream${File.separator}Main.pure", "Main:HelloWorldApp.run"))
		}
	}

	@Test
	fun `runs project using the output stream`() {
		Main.main(arrayOf("build", "${TestUtil.EXAMPLE_PROJECTS_PATH}output_stream${File.separator}Main.pure", "Main:HelloWorldApp.run"))
		TestUtil.assertExecutablePrintsLine("Hello world!")
	}
}
