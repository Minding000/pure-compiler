package linter

import TestUtil
import org.junit.jupiter.api.Test

internal class RedeclarationTest {

	@Test
	fun testRedeclarationError() {
		val sourceCode =
			"""
				var car: Int
				car = 5
				var a: String, car: Int
            """.trimIndent()
		TestUtil.assertUserError("Cannot redeclare identifier 'car'", sourceCode)
	}
}