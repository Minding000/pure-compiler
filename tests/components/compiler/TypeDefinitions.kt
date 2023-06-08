package components.compiler

import org.junit.jupiter.api.Test
import util.TestUtil

internal class TypeDefinitions {

	@Test
	fun `compiles objects without members`() {
		val sourceCode = """
			SimplestApp object
			""".trimIndent()
		val intermediateRepresentation = """
			%SimplestApp = type {}

			define void @initializer(%SimplestApp* %0) {
			initializer_entry:
			  ret void
			}

			define void @Test() {
			file:
			  %SimplestApp = alloca %SimplestApp, align 8
			  %new_pointer = alloca %SimplestApp, align 8
			  call void @initializer(%SimplestApp* %new_pointer)
			  %new = load %SimplestApp, %SimplestApp* %new_pointer, align 1
			  store %SimplestApp %new, %SimplestApp* %SimplestApp, align 1
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
			""".trimIndent()
		val intermediateRepresentation = """
			%SimplestApp = type { i32 }

			define void @initializer(%SimplestApp* %0) {
			initializer_entry:
			  %a = getelementptr inbounds %SimplestApp, %SimplestApp* %0, i32 0, i32 0
			  store i32 62, i32* %a, align 4
			  ret void
			}

			define void @Test() {
			file:
			  %SimplestApp = alloca %SimplestApp, align 8
			  %new_pointer = alloca %SimplestApp, align 8
			  call void @initializer(%SimplestApp* %new_pointer)
			  %new = load %SimplestApp, %SimplestApp* %new_pointer, align 4
			  store %SimplestApp %new, %SimplestApp* %SimplestApp, align 4
			  ret void
			}
			""".trimIndent()
		TestUtil.assertIntermediateRepresentationEquals(sourceCode, intermediateRepresentation)
	}

	//TODO continue with object value initialization (and then de-allocation)
}
