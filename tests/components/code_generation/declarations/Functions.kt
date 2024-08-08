package components.code_generation.declarations

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class Functions {

	@Test
	fun `unwrap generic parameters`() {
		val sourceCode = """
			abstract C class {
				containing A
				abstract to getE(a: A): Int
			}
			D class: <Int>C {
				overriding to getE(a: Int): Int {
					return a + 75
				}
			}
			SimplestApp object {
				to getEightyThree(): Int {
					val d: <Int>C = D()
					return d.getE(8)
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightyThree")
		assertEquals(83, result)
	}
}
