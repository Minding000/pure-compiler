package components.code_generation.declarations

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertContains
import kotlin.test.assertEquals

internal class TypeDefinitions {

	@Test
	fun `compiles objects without members`() {
		val sourceCode = """
			SimplestApp object
			""".trimIndent()
		val intermediateRepresentation = TestUtil.getIntermediateRepresentation(sourceCode)
		assertContains(intermediateRepresentation, """
			define void @SimplestApp_ClassInitializer(ptr %0) {
			entrypoint:
			  %staticMemberIdArray = tail call ptr @malloc(i32 0)
			  %staticMemberOffsetArray = tail call ptr @malloc(i32 0)
			  %propertyIdArray = tail call ptr @malloc(i32 0)
			  %propertyOffsetArray = tail call ptr @malloc(i32 0)
			  %functionIdArray = tail call ptr @malloc(i32 0)
			  %functionAddressArray = tail call ptr @malloc(i32 0)
			  store ptr %staticMemberIdArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 1), align 8
			  store ptr %staticMemberOffsetArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 2), align 8
			  store ptr %propertyIdArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 4), align 8
			  store ptr %propertyOffsetArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 5), align 8
			  store ptr %functionIdArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 7), align 8
			  store ptr %functionAddressArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 8), align 8
			  ret void
			}

			define void @SimplestApp_CommonPreInitializer(ptr %0, ptr %1) {
			entrypoint:
			  ret void
			}

			define void @SimplestApp_Initializer(ptr %0, ptr %1) {
			entrypoint:
			  ret void
			}

			define void @Test_FileInitializer(ptr %0) {
			entrypoint:
			  call void @SimplestApp_ClassInitializer(ptr %0)
			  %exception = load ptr, ptr %0, align 8
			  %doesExceptionExist = icmp ne ptr %exception, null
			  br i1 %doesExceptionExist, label %exception1, label %noException

			exception1:                                       ; preds = %entrypoint
			  ret void

			noException:                                      ; preds = %entrypoint
			  ret void
			}

			define void @Test_FileRunner(ptr %0) {
			entrypoint:
			  %newObject = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (%SimplestApp_ClassStruct, ptr null, i32 1) to i32))
			  %classDefinitionProperty = getelementptr inbounds %SimplestApp_ClassStruct, ptr %newObject, i32 0, i32 0
			  store ptr @SimplestApp_ClassDefinition, ptr %classDefinitionProperty, align 8
			  call void @SimplestApp_CommonPreInitializer(ptr %0, ptr %newObject)
			  %exception = load ptr, ptr %0, align 8
			  %doesExceptionExist = icmp ne ptr %exception, null
			  br i1 %doesExceptionExist, label %exception1, label %noException

			exception1:                                       ; preds = %entrypoint
			  ret void

			noException:                                      ; preds = %entrypoint
			  call void @SimplestApp_Initializer(ptr %0, ptr %newObject)
			  %exception2 = load ptr, ptr %0, align 8
			  %doesExceptionExist3 = icmp ne ptr %exception2, null
			  br i1 %doesExceptionExist3, label %exception4, label %noException5

			exception4:                                       ; preds = %noException
			  ret void

			noException5:                                     ; preds = %noException
			  store ptr %newObject, ptr @SimplestApp_Global, align 8
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
			define void @SimplestApp_ClassInitializer(ptr %0) {
			entrypoint:
			  %staticMemberIdArray = tail call ptr @malloc(i32 0)
			  %staticMemberOffsetArray = tail call ptr @malloc(i32 0)
			  %propertyIdArray = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32))
			  %propertyOffsetArray = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32))
			  %functionIdArray = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32))
			  %functionAddressArray = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (ptr, ptr null, i32 1) to i32))
			  %propertyIdElement = getelementptr i32, ptr %propertyIdArray, i32 0
			  %propertyOffsetElement = getelementptr i32, ptr %propertyOffsetArray, i32 0
			  store i32 1, ptr %propertyIdElement, align 4
			  store i32 8, ptr %propertyOffsetElement, align 4
			  %functionIdElement = getelementptr i32, ptr %functionIdArray, i32 0
			  %functionAddressElement = getelementptr ptr, ptr %functionAddressArray, i32 0
			  store i32 2, ptr %functionIdElement, align 4
			  store ptr @"run()", ptr %functionAddressElement, align 8
			  store ptr %staticMemberIdArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 1), align 8
			  store ptr %staticMemberOffsetArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 2), align 8
			  store ptr %propertyIdArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 4), align 8
			  store ptr %propertyOffsetArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 5), align 8
			  store ptr %functionIdArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 7), align 8
			  store ptr %functionAddressArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @SimplestApp_ClassDefinition, i32 0, i32 8), align 8
			  ret void
			}

			define void @SimplestApp_CommonPreInitializer(ptr %0, ptr %1) {
			entrypoint:
			  %_classDefinition = load ptr, ptr %1, align 8
			  %_memberOffset = call i32 @pure_runtime_getPropertyOffset(ptr %_classDefinition, i32 1)
			  %_memberAddress = getelementptr i8, ptr %1, i32 %_memberOffset
			  store i32 62, ptr %_memberAddress, align 4
			  ret void
			}

			define void @"run()"(ptr %0, ptr %1) {
			entrypoint:
			  ret void
			}

			define void @SimplestApp_Initializer(ptr %0, ptr %1) {
			entrypoint:
			  ret void
			}

			define void @Test_FileInitializer(ptr %0) {
			entrypoint:
			  call void @SimplestApp_ClassInitializer(ptr %0)
			  %exception = load ptr, ptr %0, align 8
			  %doesExceptionExist = icmp ne ptr %exception, null
			  br i1 %doesExceptionExist, label %exception1, label %noException

			exception1:                                       ; preds = %entrypoint
			  ret void

			noException:                                      ; preds = %entrypoint
			  ret void
			}

			define void @Test_FileRunner(ptr %0) {
			entrypoint:
			  %newObject = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (%SimplestApp_ClassStruct, ptr null, i32 1) to i32))
			  %classDefinitionProperty = getelementptr inbounds %SimplestApp_ClassStruct, ptr %newObject, i32 0, i32 0
			  store ptr @SimplestApp_ClassDefinition, ptr %classDefinitionProperty, align 8
			  call void @SimplestApp_CommonPreInitializer(ptr %0, ptr %newObject)
			  %exception = load ptr, ptr %0, align 8
			  %doesExceptionExist = icmp ne ptr %exception, null
			  br i1 %doesExceptionExist, label %exception1, label %noException

			exception1:                                       ; preds = %entrypoint
			  ret void

			noException:                                      ; preds = %entrypoint
			  call void @SimplestApp_Initializer(ptr %0, ptr %newObject)
			  %exception2 = load ptr, ptr %0, align 8
			  %doesExceptionExist3 = icmp ne ptr %exception2, null
			  br i1 %doesExceptionExist3, label %exception4, label %noException5

			exception4:                                       ; preds = %noException
			  ret void

			noException5:                                     ; preds = %noException
			  store ptr %newObject, ptr @SimplestApp_Global, align 8
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
			define void @Application_ClassInitializer(ptr %0) {
			entrypoint:
			  %staticMemberIdArray = tail call ptr @malloc(i32 0)
			  %staticMemberOffsetArray = tail call ptr @malloc(i32 0)
			  %propertyIdArray = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32))
			  %propertyOffsetArray = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32))
			  %functionIdArray = tail call ptr @malloc(i32 0)
			  %functionAddressArray = tail call ptr @malloc(i32 0)
			  %propertyIdElement = getelementptr i32, ptr %propertyIdArray, i32 0
			  %propertyOffsetElement = getelementptr i32, ptr %propertyOffsetArray, i32 0
			  store i32 1, ptr %propertyIdElement, align 4
			  store i32 8, ptr %propertyOffsetElement, align 4
			  store ptr %staticMemberIdArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @Application_ClassDefinition, i32 0, i32 1), align 8
			  store ptr %staticMemberOffsetArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @Application_ClassDefinition, i32 0, i32 2), align 8
			  store ptr %propertyIdArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @Application_ClassDefinition, i32 0, i32 4), align 8
			  store ptr %propertyOffsetArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @Application_ClassDefinition, i32 0, i32 5), align 8
			  store ptr %functionIdArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @Application_ClassDefinition, i32 0, i32 7), align 8
			  store ptr %functionAddressArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @Application_ClassDefinition, i32 0, i32 8), align 8
			  ret void
			}

			define void @Application_CommonPreInitializer(ptr %0, ptr %1) {
			entrypoint:
			  %_classDefinition = load ptr, ptr %1, align 8
			  %_memberOffset = call i32 @pure_runtime_getPropertyOffset(ptr %_classDefinition, i32 1)
			  %_memberAddress = getelementptr i8, ptr %1, i32 %_memberOffset
			  store float 0x4058F999A0000000, ptr %_memberAddress, align 4
			  ret void
			}

			define void @Application_Initializer(ptr %0, ptr %1) {
			entrypoint:
			  ret void
			}

			define void @Test_FileInitializer(ptr %0) {
			entrypoint:
			  call void @Application_ClassInitializer(ptr %0)
			  %exception = load ptr, ptr %0, align 8
			  %doesExceptionExist = icmp ne ptr %exception, null
			  br i1 %doesExceptionExist, label %exception1, label %noException

			exception1:                                       ; preds = %entrypoint
			  ret void

			noException:                                      ; preds = %entrypoint
			  ret void
			}

			define void @Test_FileRunner(ptr %0) {
			entrypoint:
			  %newObject = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (%Application_ClassStruct, ptr null, i32 1) to i32))
			  %classDefinitionProperty = getelementptr inbounds %Application_ClassStruct, ptr %newObject, i32 0, i32 0
			  store ptr @Application_ClassDefinition, ptr %classDefinitionProperty, align 8
			  call void @Application_CommonPreInitializer(ptr %0, ptr %newObject)
			  %exception = load ptr, ptr %0, align 8
			  %doesExceptionExist = icmp ne ptr %exception, null
			  br i1 %doesExceptionExist, label %exception1, label %noException

			exception1:                                       ; preds = %entrypoint
			  ret void

			noException:                                      ; preds = %entrypoint
			  call void @Application_Initializer(ptr %0, ptr %newObject)
			  %exception2 = load ptr, ptr %0, align 8
			  %doesExceptionExist3 = icmp ne ptr %exception2, null
			  br i1 %doesExceptionExist3, label %exception4, label %noException5

			exception4:                                       ; preds = %noException
			  ret void

			noException5:                                     ; preds = %noException
			  store ptr %newObject, ptr @app_Global, align 8
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
			define void @InternetProtocol_ClassInitializer(ptr %0) {
			entrypoint:
			  %staticMemberIdArray = tail call ptr @malloc(i32 mul (i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32), i32 2))
			  %staticMemberOffsetArray = tail call ptr @malloc(i32 mul (i32 ptrtoint (ptr getelementptr (i32, ptr null, i32 1) to i32), i32 2))
			  %propertyIdArray = tail call ptr @malloc(i32 0)
			  %propertyOffsetArray = tail call ptr @malloc(i32 0)
			  %functionIdArray = tail call ptr @malloc(i32 0)
			  %functionAddressArray = tail call ptr @malloc(i32 0)
			  %staticMemberIdElement = getelementptr i32, ptr %staticMemberIdArray, i32 0
			  %staticMemberOffsetElement = getelementptr i32, ptr %staticMemberOffsetArray, i32 0
			  store i32 1, ptr %staticMemberIdElement, align 4
			  store i32 8, ptr %staticMemberOffsetElement, align 4
			  %staticMemberIdElement1 = getelementptr i32, ptr %staticMemberIdArray, i32 1
			  %staticMemberOffsetElement2 = getelementptr i32, ptr %staticMemberOffsetArray, i32 1
			  store i32 2, ptr %staticMemberIdElement1, align 4
			  store i32 16, ptr %staticMemberOffsetElement2, align 4
			  store ptr %staticMemberIdArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @InternetProtocol_ClassDefinition, i32 0, i32 1), align 8
			  store ptr %staticMemberOffsetArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @InternetProtocol_ClassDefinition, i32 0, i32 2), align 8
			  store ptr %propertyIdArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @InternetProtocol_ClassDefinition, i32 0, i32 4), align 8
			  store ptr %propertyOffsetArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @InternetProtocol_ClassDefinition, i32 0, i32 5), align 8
			  store ptr %functionIdArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @InternetProtocol_ClassDefinition, i32 0, i32 7), align 8
			  store ptr %functionAddressArray, ptr getelementptr inbounds (%pure_runtime_ClassStruct, ptr @InternetProtocol_ClassDefinition, i32 0, i32 8), align 8
			  %InternetProtocol_IPv6_Instance = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (%InternetProtocol_ClassStruct, ptr null, i32 1) to i32))
			  %classDefinitionProperty = getelementptr inbounds %InternetProtocol_ClassStruct, ptr %InternetProtocol_IPv6_Instance, i32 0, i32 0
			  store ptr @InternetProtocol_ClassDefinition, ptr %classDefinitionProperty, align 8
			  call void @InternetProtocol_CommonPreInitializer(ptr %0, ptr %InternetProtocol_IPv6_Instance)
			  %exception = load ptr, ptr %0, align 8
			  %doesExceptionExist = icmp ne ptr %exception, null
			  br i1 %doesExceptionExist, label %exception3, label %noException

			exception3:                                       ; preds = %entrypoint
			  ret void

			noException:                                      ; preds = %entrypoint
			  call void @InternetProtocol_Initializer(ptr %0, ptr %InternetProtocol_IPv6_Instance)
			  %exception4 = load ptr, ptr %0, align 8
			  %doesExceptionExist5 = icmp ne ptr %exception4, null
			  br i1 %doesExceptionExist5, label %exception6, label %noException7

			exception6:                                       ; preds = %noException
			  ret void

			noException7:                                     ; preds = %noException
			  store ptr %InternetProtocol_IPv6_Instance, ptr getelementptr (i8, ptr @InternetProtocol_StaticObject, i32 8), align 8
			  %InternetProtocol_IPv4_Instance = tail call ptr @malloc(i32 ptrtoint (ptr getelementptr (%InternetProtocol_ClassStruct, ptr null, i32 1) to i32))
			  %classDefinitionProperty8 = getelementptr inbounds %InternetProtocol_ClassStruct, ptr %InternetProtocol_IPv4_Instance, i32 0, i32 0
			  store ptr @InternetProtocol_ClassDefinition, ptr %classDefinitionProperty8, align 8
			  call void @InternetProtocol_CommonPreInitializer(ptr %0, ptr %InternetProtocol_IPv4_Instance)
			  %exception9 = load ptr, ptr %0, align 8
			  %doesExceptionExist10 = icmp ne ptr %exception9, null
			  br i1 %doesExceptionExist10, label %exception11, label %noException12

			exception11:                                      ; preds = %noException7
			  ret void

			noException12:                                    ; preds = %noException7
			  call void @InternetProtocol_Initializer(ptr %0, ptr %InternetProtocol_IPv4_Instance)
			  %exception13 = load ptr, ptr %0, align 8
			  %doesExceptionExist14 = icmp ne ptr %exception13, null
			  br i1 %doesExceptionExist14, label %exception15, label %noException16

			exception15:                                      ; preds = %noException12
			  ret void

			noException16:                                    ; preds = %noException12
			  store ptr %InternetProtocol_IPv4_Instance, ptr getelementptr (i8, ptr @InternetProtocol_StaticObject, i32 16), align 8
			  ret void
			}

			define void @InternetProtocol_CommonPreInitializer(ptr %0, ptr %1) {
			entrypoint:
			  ret void
			}

			define void @InternetProtocol_Initializer(ptr %0, ptr %1) {
			entrypoint:
			  ret void
			}

			define void @Test_FileInitializer(ptr %0) {
			entrypoint:
			  call void @InternetProtocol_ClassInitializer(ptr %0)
			  %exception = load ptr, ptr %0, align 8
			  %doesExceptionExist = icmp ne ptr %exception, null
			  br i1 %doesExceptionExist, label %exception1, label %noException

			exception1:                                       ; preds = %entrypoint
			  ret void

			noException:                                      ; preds = %entrypoint
			  ret void
			}

			define void @Test_FileRunner(ptr %0) {
			entrypoint:
			  %_classDefinition = load ptr, ptr @InternetProtocol_StaticObject, align 8
			  %_memberOffset = call i32 @pure_runtime_getConstantOffset(ptr %_classDefinition, i32 2)
			  %_memberAddress = getelementptr i8, ptr @InternetProtocol_StaticObject, i32 %_memberOffset
			  %member = load ptr, ptr %_memberAddress, align 8
			  store ptr %member, ptr @protocol_Global, align 8
			  ret void
			}
			""".trimIndent())
	}

	@Test
	fun `compiles inherited bound objects`() {
		val sourceCode = """
			Application class {
				bound Process object {
					val a = 58
				}
			}
			SimplestApp object: Application {
				to getFiftyEight(): Int {
					return Process.a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFiftyEight")
		assertEquals(58, result)
	}
}
