package projects

import code.Main
import org.junit.jupiter.api.Test
import util.TestUtil
import java.io.File

internal class NativeAdderProject {

	@Test
	fun `builds without errors`() {
		TestUtil.assertErrorStreamEmpty {
			Main.main(arrayOf("build", "${TestUtil.EXAMPLE_PROJECTS_PATH}${File.separator}NativeAdder", "NativeAdder.Main:NativeAdder.run",
				"--library", "tests/resources/NativeAdder.obj"))
		}
	}

	@Test
	fun `runs executable without errors`() {
		Main.main(arrayOf("build", "${TestUtil.EXAMPLE_PROJECTS_PATH}${File.separator}NativeAdder", "NativeAdder.Main:NativeAdder.run",
			"--library", "tests/resources/NativeAdder.obj"))
		TestUtil.assertExecutablePrints("", "", ".${File.separator}out${File.separator}program.exe", 89)
	}

	@Test
	fun `runs in memory without errors`() {
		TestUtil.assertJitExitCode(89) {
			Main.main(arrayOf("run", "${TestUtil.EXAMPLE_PROJECTS_PATH}${File.separator}NativeAdder", "NativeAdder.Main:NativeAdder.run",
				"--library", "tests/resources/NativeAdder.obj"))
		}
	}
}
