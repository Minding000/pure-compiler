package components.compiler

import org.junit.jupiter.api.Test
import util.TestUtil

internal class TypeDefinitions {

	@Test
	fun `compiles objects without members`() {
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

	@Test
	fun `compiles objects with properties`() {
		val sourceCode = """
			SimplestApp object {
				val a = 62
			}
			val app: SimplestApp
			""".trimIndent()
		val intermediateRepresentation = """
			%SimplestApp = type { i32 }

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
