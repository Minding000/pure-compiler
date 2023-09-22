package components.code_generation

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
			define void @SimplestApp_ClassInitializer() {
			entrypoint:
			  %staticMemberIdArray = tail call ptr @malloc(i32 0)
			  %staticMemberOffsetArray = tail call ptr @malloc(i32 0)
			  %propertyIdArray = tail call ptr @malloc(i32 0)
			  %propertyOffsetArray = tail call ptr @malloc(i32 0)
			  %functionIdArray = tail call ptr @malloc(i32 0)
			  %functionAddressArray = tail call ptr @malloc(i32 0)
			  store ptr %staticMemberIdArray, ptr getelementptr inbounds (%_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 1), align 8
			  store ptr %staticMemberOffsetArray, ptr getelementptr inbounds (%_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 2), align 8
			  store ptr %propertyIdArray, ptr getelementptr inbounds (%_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 4), align 8
			  store ptr %propertyOffsetArray, ptr getelementptr inbounds (%_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 5), align 8
			  store ptr %functionIdArray, ptr getelementptr inbounds (%_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 7), align 8
			  store ptr %functionAddressArray, ptr getelementptr inbounds (%_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 8), align 8
			  ret void
			}

			define void @SimplestApp_Initializer(ptr %0) {
			entrypoint:
			  ret void
			}

			define void @Test_FileInitializer() {
			entrypoint:
			  call void @SimplestApp_ClassInitializer()
			  %newObjectAddress = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (%SimplestApp_ClassStruct, ptr null, i32 1) to i32))
			  %classDefinitionPointer = getelementptr inbounds %SimplestApp_ClassStruct, ptr %newObjectAddress, i32 0, i32 0
			  store ptr @SimplestApp_ClassDefinition, ptr %classDefinitionPointer, align 8
			  call void @SimplestApp_Initializer(ptr %newObjectAddress)
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
			define void @SimplestApp_ClassInitializer() {
			entrypoint:
			  %staticMemberIdArray = tail call ptr @malloc(i32 0)
			  %staticMemberOffsetArray = tail call ptr @malloc(i32 0)
			  %propertyIdArray = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32))
			  %propertyOffsetArray = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32))
			  %functionIdArray = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32))
			  %functionAddressArray = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (ptr, ptr null, i32 1) to i32))
			  %propertyIdLocation = getelementptr i32, ptr %propertyIdArray, i32 0
			  %propertyOffsetLocation = getelementptr i32, ptr %propertyOffsetArray, i32 0
			  store i32 1, ptr %propertyIdLocation, align 4
			  store i32 8, ptr %propertyOffsetLocation, align 4
			  %functionIdLocation = getelementptr i32, ptr %functionIdArray, i32 0
			  %functionAddressLocation = getelementptr ptr, ptr %functionAddressArray, i32 0
			  store i32 2, ptr %functionIdLocation, align 4
			  store ptr @"run()", ptr %functionAddressLocation, align 8
			  store ptr %staticMemberIdArray, ptr getelementptr inbounds (%_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 1), align 8
			  store ptr %staticMemberOffsetArray, ptr getelementptr inbounds (%_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 2), align 8
			  store ptr %propertyIdArray, ptr getelementptr inbounds (%_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 4), align 8
			  store ptr %propertyOffsetArray, ptr getelementptr inbounds (%_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 5), align 8
			  store ptr %functionIdArray, ptr getelementptr inbounds (%_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 7), align 8
			  store ptr %functionAddressArray, ptr getelementptr inbounds (%_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 8), align 8
			  ret void
			}

			define void @"run()"(ptr %0) {
			entrypoint:
			  ret void
			}

			define void @SimplestApp_Initializer(ptr %0) {
			entrypoint:
			  %classDefinition = getelementptr inbounds %SimplestApp_ClassStruct, ptr %0, i32 0, i32 0
			  %classDefinitionAddress = load ptr, ptr %classDefinition, align 8
			  %memberOffset = call i32 @_getPropertyOffset(ptr %classDefinitionAddress, i32 1)
			  %memberAddress = getelementptr i8, ptr %0, i32 %memberOffset
			  store i32 62, ptr %memberAddress, align 4
			  ret void
			}

			define void @Test_FileInitializer() {
			entrypoint:
			  call void @SimplestApp_ClassInitializer()
			  %newObjectAddress = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (%SimplestApp_ClassStruct, ptr null, i32 1) to i32))
			  %classDefinitionPointer = getelementptr inbounds %SimplestApp_ClassStruct, ptr %newObjectAddress, i32 0, i32 0
			  store ptr @SimplestApp_ClassDefinition, ptr %classDefinitionPointer, align 8
			  call void @SimplestApp_Initializer(ptr %newObjectAddress)
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
			define void @Application_ClassInitializer() {
			entrypoint:
			  %staticMemberIdArray = tail call ptr @malloc(i32 0)
			  %staticMemberOffsetArray = tail call ptr @malloc(i32 0)
			  %propertyIdArray = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32))
			  %propertyOffsetArray = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32))
			  %functionIdArray = tail call ptr @malloc(i32 0)
			  %functionAddressArray = tail call ptr @malloc(i32 0)
			  %propertyIdLocation = getelementptr i32, ptr %propertyIdArray, i32 0
			  %propertyOffsetLocation = getelementptr i32, ptr %propertyOffsetArray, i32 0
			  store i32 1, ptr %propertyIdLocation, align 4
			  store i32 8, ptr %propertyOffsetLocation, align 4
			  store ptr %staticMemberIdArray, ptr getelementptr inbounds (%_ClassStruct, ptr @Application_ClassDefinition, i32 0, i32 1), align 8
			  store ptr %staticMemberOffsetArray, ptr getelementptr inbounds (%_ClassStruct, ptr @Application_ClassDefinition, i32 0, i32 2), align 8
			  store ptr %propertyIdArray, ptr getelementptr inbounds (%_ClassStruct, ptr @Application_ClassDefinition, i32 0, i32 4), align 8
			  store ptr %propertyOffsetArray, ptr getelementptr inbounds (%_ClassStruct, ptr @Application_ClassDefinition, i32 0, i32 5), align 8
			  store ptr %functionIdArray, ptr getelementptr inbounds (%_ClassStruct, ptr @Application_ClassDefinition, i32 0, i32 7), align 8
			  store ptr %functionAddressArray, ptr getelementptr inbounds (%_ClassStruct, ptr @Application_ClassDefinition, i32 0, i32 8), align 8
			  ret void
			}

			define void @Application_Initializer(ptr %0) {
			entrypoint:
			  %classDefinition = getelementptr inbounds %Application_ClassStruct, ptr %0, i32 0, i32 0
			  %classDefinitionAddress = load ptr, ptr %classDefinition, align 8
			  %memberOffset = call i32 @_getPropertyOffset(ptr %classDefinitionAddress, i32 1)
			  %memberAddress = getelementptr i8, ptr %0, i32 %memberOffset
			  store float 0x4058F999A0000000, ptr %memberAddress, align 4
			  ret void
			}

			define void @Test_FileInitializer() {
			entrypoint:
			  call void @Application_ClassInitializer()
			  %newObjectAddress = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (%Application_ClassStruct, ptr null, i32 1) to i32))
			  %classDefinitionPointer = getelementptr inbounds %Application_ClassStruct, ptr %newObjectAddress, i32 0, i32 0
			  store ptr @Application_ClassDefinition, ptr %classDefinitionPointer, align 8
			  call void @Application_Initializer(ptr %newObjectAddress)
			  store ptr %newObjectAddress, ptr @app_Global, align 8
			  ret void
			}
			""".trimIndent())
	}

	@Test
	fun `compiles enums`() {
		val sourceCode = """
			InternetProtocol enum {
				instances IPv4, IPv6
			}
			val protocol = InternetProtocol.IPv4
			""".trimIndent()
		val intermediateRepresentation = TestUtil.getIntermediateRepresentation(sourceCode)
		assertContains(intermediateRepresentation, """
			define void @InternetProtocol_ClassInitializer() {
			entrypoint:
			  %staticMemberIdArray = tail call ptr @malloc(i32 mul (i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32), i32 2))
			  %staticMemberOffsetArray = tail call ptr @malloc(i32 mul (i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32), i32 2))
			  %propertyIdArray = tail call ptr @malloc(i32 0)
			  %propertyOffsetArray = tail call ptr @malloc(i32 0)
			  %functionIdArray = tail call ptr @malloc(i32 0)
			  %functionAddressArray = tail call ptr @malloc(i32 0)
			  %staticMemberIdLocation = getelementptr i32, ptr %staticMemberIdArray, i32 0
			  %staticMemberOffsetLocation = getelementptr i32, ptr %staticMemberOffsetArray, i32 0
			  store i32 1, ptr %staticMemberIdLocation, align 4
			  store i32 8, ptr %staticMemberOffsetLocation, align 4
			  %staticMemberIdLocation1 = getelementptr i32, ptr %staticMemberIdArray, i32 1
			  %staticMemberOffsetLocation2 = getelementptr i32, ptr %staticMemberOffsetArray, i32 1
			  store i32 2, ptr %staticMemberIdLocation1, align 4
			  store i32 16, ptr %staticMemberOffsetLocation2, align 4
			  store ptr %staticMemberIdArray, ptr getelementptr inbounds (%_ClassStruct, ptr @InternetProtocol_ClassDefinition, i32 0, i32 1), align 8
			  store ptr %staticMemberOffsetArray, ptr getelementptr inbounds (%_ClassStruct, ptr @InternetProtocol_ClassDefinition, i32 0, i32 2), align 8
			  store ptr %propertyIdArray, ptr getelementptr inbounds (%_ClassStruct, ptr @InternetProtocol_ClassDefinition, i32 0, i32 4), align 8
			  store ptr %propertyOffsetArray, ptr getelementptr inbounds (%_ClassStruct, ptr @InternetProtocol_ClassDefinition, i32 0, i32 5), align 8
			  store ptr %functionIdArray, ptr getelementptr inbounds (%_ClassStruct, ptr @InternetProtocol_ClassDefinition, i32 0, i32 7), align 8
			  store ptr %functionAddressArray, ptr getelementptr inbounds (%_ClassStruct, ptr @InternetProtocol_ClassDefinition, i32 0, i32 8), align 8
			  ret void
			}

			define void @InternetProtocol_Initializer(ptr %0) {
			entrypoint:
			  ret void
			}

			define void @Test_FileInitializer() {
			entrypoint:
			  call void @InternetProtocol_ClassInitializer()
			  %InternetProtocol = load ptr, ptr @InternetProtocol_StaticObject, align 8
			  %classDefinition = getelementptr inbounds %InternetProtocol_StaticStruct, ptr %InternetProtocol, i32 0, i32 0
			  %classDefinitionAddress = load ptr, ptr %classDefinition, align 8
			  %memberOffset = call i32 @_getConstantOffset(ptr %classDefinitionAddress, i32 2)
			  %memberAddress = getelementptr i8, ptr %InternetProtocol, i32 %memberOffset
			  %member = load ptr, ptr %memberAddress, align 8
			  store ptr %member, ptr @protocol_Global, align 8
			  ret void
			}
			""".trimIndent())
	}
}
