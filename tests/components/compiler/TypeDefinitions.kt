package components.compiler

import org.junit.jupiter.api.Test
import util.TestUtil

internal class TypeDefinitions {

	@Test
	fun `compiles objects`() {
		val sourceCode = """
			SimplestApp object
			val app: SimplestApp
			""".trimIndent()
		val intermediateRepresentation = """
			%SimplestApp = type {}

			define void @Test() {
			file:
			  %SimplestApp = alloca %SimplestApp, align 8
			  %app = alloca %SimplestApp, align 8
			  ret void
			}
			""".trimIndent()
		TestUtil.assertIntermediateRepresentationEquals(sourceCode, intermediateRepresentation)
	}
}
