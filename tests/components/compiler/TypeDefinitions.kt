package components.compiler

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertContains

internal class TypeDefinitions {

	@Test
	fun `compiles objects without members`() {
		val sourceCode = """
			SimplestApp object
			""".trimIndent()
		val intermediateRepresentation = TestUtil.getIntermediateRepresentation(sourceCode)
		assertContains(intermediateRepresentation, """
			%SimplestApp_ClassStruct = type { ptr }

			@SimplestApp_ClassDefinition = global %_ClassStruct { i32 1, ptr null, ptr null }
			@SimplestApp_Global = global ptr null
			""".trimIndent())
		assertContains(intermediateRepresentation, """
			define void @SimplestApp_ClassInitializer() {
			entrypoint:
			  %memberIdArray = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32))
			  %memberOffsetArray = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32))
			  %memberIdLocation = getelementptr i32, ptr %memberIdArray, i32 0
			  store i32 0, ptr %memberIdLocation, align 4
			  store ptr %memberIdArray, ptr getelementptr inbounds (%_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 1), align 8
			  store ptr %memberOffsetArray, ptr getelementptr inbounds (%_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 2), align 8
			  ret void
			}

			define ptr @SimplestApp_Initializer() {
			entrypoint:
			  %this = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (%SimplestApp_ClassStruct, ptr null, i32 1) to i32))
			  %classDefinitionPointer = getelementptr inbounds %SimplestApp_ClassStruct, ptr %this, i32 0, i32 0
			  store ptr @SimplestApp_ClassDefinition, ptr %classDefinitionPointer, align 8
			  ret ptr %this
			}

			define void @Test_FileInitializer() {
			entrypoint:
			  call void @SimplestApp_ClassInitializer()
			  %newObjectAddress = call ptr @SimplestApp_Initializer()
			  store ptr %newObjectAddress, ptr @SimplestApp_Global, align 8
			  ret void
			}
			""".trimIndent())
	}

	@Test
	fun `compiles objects with properties`() {
		val sourceCode = """
			SimplestApp object {
				val a = 62
				to run() {}
			}
			""".trimIndent()
		val intermediateRepresentation = TestUtil.getIntermediateRepresentation(sourceCode)
		assertContains(intermediateRepresentation, """
			%SimplestApp_ClassStruct = type { ptr, i32 }

			@SimplestApp_ClassDefinition = global %_ClassStruct { i32 3, ptr null, ptr null }
			@SimplestApp_Global = global ptr null
			""".trimIndent())
		assertContains(intermediateRepresentation, """
			define void @SimplestApp_ClassInitializer() {
			entrypoint:
			  %memberIdArray = tail call ptr @malloc(i32 mul (i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32), i32 3))
			  %memberOffsetArray = tail call ptr @malloc(i32 mul (i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32), i32 3))
			  %memberIdLocation = getelementptr i32, ptr %memberIdArray, i32 0
			  %memberOffsetLocation = getelementptr i32, ptr %memberOffsetArray, i32 0
			  store i32 1, ptr %memberIdLocation, align 4
			  store i32 8, ptr %memberOffsetLocation, align 4
			  %memberIdLocation1 = getelementptr i32, ptr %memberIdArray, i32 1
			  store i32 0, ptr %memberIdLocation1, align 4
			  %memberIdLocation2 = getelementptr i32, ptr %memberIdArray, i32 2
			  store i32 0, ptr %memberIdLocation2, align 4
			  store ptr %memberIdArray, ptr getelementptr inbounds (%_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 1), align 8
			  store ptr %memberOffsetArray, ptr getelementptr inbounds (%_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 2), align 8
			  ret void
			}

			define void @"run()"() {
			entrypoint:
			  ret void
			}

			define ptr @SimplestApp_Initializer() {
			entrypoint:
			  %this = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (%SimplestApp_ClassStruct, ptr null, i32 1) to i32))
			  %classDefinitionPointer = getelementptr inbounds %SimplestApp_ClassStruct, ptr %this, i32 0, i32 0
			  store ptr @SimplestApp_ClassDefinition, ptr %classDefinitionPointer, align 8
			  %classDefinition = getelementptr inbounds %SimplestApp_ClassStruct, ptr %this, i32 0, i32 0
			  %classDefinitionAddress = load ptr, ptr %classDefinition, align 8
			  %memberIndex = call i32 @_getMemberOffset(ptr %classDefinitionAddress, i32 1)
			  %memberAddress = getelementptr i8, ptr %this, i32 %memberIndex
			  store i32 62, ptr %memberAddress, align 4
			  ret ptr %this
			}

			define void @Test_FileInitializer() {
			entrypoint:
			  call void @SimplestApp_ClassInitializer()
			  %newObjectAddress = call ptr @SimplestApp_Initializer()
			  store ptr %newObjectAddress, ptr @SimplestApp_Global, align 8
			  ret void
			}
			""".trimIndent())
	}

	@Test
	fun `compiles class instantiation`() {
		val sourceCode = """
			Application class {
				val a = 99.9
			}
			val app = Application()
			""".trimIndent()
		val intermediateRepresentation = TestUtil.getIntermediateRepresentation(sourceCode)
		assertContains(intermediateRepresentation, """
			%Application_ClassStruct = type { ptr, float }

			@Application_ClassDefinition = global %_ClassStruct { i32 2, ptr null, ptr null }
			@Application_Global = global ptr null
			@app_Global = global ptr null
			""".trimIndent())
		assertContains(intermediateRepresentation, """
			define void @Application_ClassInitializer() {
			entrypoint:
			  %memberIdArray = tail call ptr @malloc(i32 mul (i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32), i32 2))
			  %memberOffsetArray = tail call ptr @malloc(i32 mul (i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32), i32 2))
			  %memberIdLocation = getelementptr i32, ptr %memberIdArray, i32 0
			  %memberOffsetLocation = getelementptr i32, ptr %memberOffsetArray, i32 0
			  store i32 1, ptr %memberIdLocation, align 4
			  store i32 8, ptr %memberOffsetLocation, align 4
			  %memberIdLocation1 = getelementptr i32, ptr %memberIdArray, i32 1
			  store i32 0, ptr %memberIdLocation1, align 4
			  store ptr %memberIdArray, ptr getelementptr inbounds (%_ClassStruct, ptr @Application_ClassDefinition, i32 0, i32 1), align 8
			  store ptr %memberOffsetArray, ptr getelementptr inbounds (%_ClassStruct, ptr @Application_ClassDefinition, i32 0, i32 2), align 8
			  ret void
			}

			define ptr @Application_Initializer() {
			entrypoint:
			  %this = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (%Application_ClassStruct, ptr null, i32 1) to i32))
			  %classDefinition = getelementptr inbounds %Application_ClassStruct, ptr %this, i32 0, i32 0
			  %classDefinitionAddress = load ptr, ptr %classDefinition, align 8
			  %memberIndex = call i32 @_getMemberOffset(ptr %classDefinitionAddress, i32 1)
			  %memberAddress = getelementptr i8, ptr %this, i32 %memberIndex
			  store float 0x4058F999A0000000, ptr %memberAddress, align 4
			  ret ptr %this
			}

			define void @Test_FileInitializer() {
			entrypoint:
			  %newObjectAddress = call ptr @Application_Initializer()
			  store ptr %newObjectAddress, ptr @app_Global, align 8
			  ret void
			}
			""".trimIndent())
	}
}
