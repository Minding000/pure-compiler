package components.code_generation.native_implementations

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import util.TestUtil
import kotlin.test.assertEquals

internal class Math {

	@TestFactory
	fun getRemainder() = listOf(
		listOf(0, 1) to 0,
		listOf(25, 20) to 5,
	).map { (parameters, remainder) ->
		val (dividend, divisor) = parameters
		DynamicTest.dynamicTest("Remainder of $dividend in base $divisor should be $remainder") {
			val sourceCode = """
				SimplestApp object {
					to getRemainder(): Int {
						return Math.getRemainder($dividend, $divisor)
					}
				}
				Math object {
					native to getRemainder(dividend: Int, divisor: Int): Int
				}
			""".trimIndent()
			val result = TestUtil.run(sourceCode, "Test:SimplestApp.getRemainder")
			assertEquals(remainder, result)
		}
	}
}
