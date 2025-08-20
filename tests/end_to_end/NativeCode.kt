package end_to_end

import code.Main
import org.junit.jupiter.api.Test
import util.TestUtil
import java.io.File

internal class NativeCode {

	@Test
	fun `builds project with native functions without errors`() {
		TestUtil.assertErrorStreamEmpty {
			Main.main(arrayOf("build", "${TestUtil.EXAMPLE_PROJECTS_PATH}native_code", "native_code.Main:NativeAdder.run",
				"--library", "${TestUtil.EXAMPLE_PROJECTS_PATH}native_code${File.separator}NativeAdder.obj"))
		}
	}

	@Test
	fun `runs project with native functions as executable without errors`() {
		Main.main(arrayOf("build", "${TestUtil.EXAMPLE_PROJECTS_PATH}native_code", "native_code.Main:NativeAdder.run",
			"--library", "${TestUtil.EXAMPLE_PROJECTS_PATH}native_code${File.separator}NativeAdder.obj"))
		TestUtil.assertExecutablePrints("", "", ".${File.separator}out${File.separator}program.exe", 89)
	}

	@Test
	fun `runs project with native functions in memory without errors`() {
		TestUtil.assertJitExitCode(89) {
			Main.main(arrayOf("run", "${TestUtil.EXAMPLE_PROJECTS_PATH}native_code", "native_code.Main:NativeAdder.run",
				"--library", "${TestUtil.EXAMPLE_PROJECTS_PATH}native_code${File.separator}NativeAdder.obj"))
		}
	}
}
