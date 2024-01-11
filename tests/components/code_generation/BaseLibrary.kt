package components.code_generation

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class BaseLibrary {

	@Test
	fun `compiles the base library without issues`() {
		val result = TestUtil.lint("", true)
		assertEquals(emptyList(), result.logger.issues().asSequence().toList())
	}
}
