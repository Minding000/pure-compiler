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
			%SimplestAppStruct = type {}

			@SimplestAppPointer = global %SimplestAppStruct zeroinitializer

			define void @SimplestApp_initializer(%SimplestAppStruct* %0) {
			initializer_entry:
			  ret void
			}

			define void @Test() {
			file:
			  call void @SimplestApp_initializer(%SimplestAppStruct* @SimplestAppPointer)
			  ret void
			}

			define void @entrypoint() {
			entrypoint:
			  call void @Test()
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
				to run() {}
			}
			""".trimIndent()
		val intermediateRepresentation = """
			%SimplestAppStruct = type { i32 }

			@SimplestAppPointer = global %SimplestAppStruct { i32 62 }

			define void @"run()"() {
			function_entry:
			  ret void
			}

			define void @SimplestApp_initializer(%SimplestAppStruct* %0) {
			initializer_entry:
			  %aPointer = getelementptr inbounds %SimplestAppStruct, %SimplestAppStruct* %0, i32 0, i32 0
			  store i32 62, i32* %aPointer, align 4
			  ret void
			}

			define void @Test() {
			file:
			  call void @SimplestApp_initializer(%SimplestAppStruct* @SimplestAppPointer)
			  ret void
			}

			define void @entrypoint() {
			entrypoint:
			  call void @Test()
			  ret void
			}
			""".trimIndent()
		TestUtil.assertIntermediateRepresentationEquals(sourceCode, intermediateRepresentation)
		TestUtil.run(sourceCode, "Test:SimplestApp.run")
	}
}
