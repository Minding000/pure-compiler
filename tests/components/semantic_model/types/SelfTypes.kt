package components.semantic_model.types

import components.semantic_model.declarations.LocalVariableDeclaration
import logger.Severity
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.declaration.InvalidSelfTypeLocation
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertIs
import kotlin.test.assertNotNull

internal class SelfTypes {

	@Test
	fun `self types can be used in classes`() {
		val sourceCode =
			"""
				Camera class {
					to getDevice(): Self
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<InvalidSelfTypeLocation>()
	}

	@Test
	fun `self types can not be used outside of classes`() {
		val sourceCode =
			"""
				val camera: Self
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<InvalidSelfTypeLocation>("The self type is only allowed in type declarations.",
			Severity.ERROR)
	}

	@Test
	fun `self types are assignable to super type`() {
		val sourceCode =
			"""
				var device: Device? = null
				Device class
				Camera class: Device {
					init {
						device = this
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}

	@Test
	fun `sub-types are assignable to self type`() {
		val sourceCode =
			"""
				Device class {
					var backup: Self? = null
					init {
						backup = Camera()
					}
				}
				Camera class: Device
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}

	@Test
	fun `is preserved in member access in own type definition`() {
		val sourceCode =
			"""
				Device object {
					var backup: Self? = null
					to getAndLog(): Self? {
						var device = backup
						return device
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableType = lintResult.find<LocalVariableDeclaration> { localVariableDeclaration ->
			localVariableDeclaration.name == "device" }?.providedType
		assertNotNull(variableType)
		assertIs<OptionalType>(variableType)
		assertIs<SelfType>(variableType.baseType)
	}

	@Test
	fun `is converted in member access outside of own type definition`() {
		val sourceCode =
			"""
				Device object {
					var backup: Self? = null
				}
				DeviceManager object {
					to getAndLogDevice(): Device? {
						var device = Device.backup
						return device
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableType = lintResult.find<LocalVariableDeclaration> { localVariableDeclaration ->
			localVariableDeclaration.name == "device" }?.providedType
		assertNotNull(variableType)
		assertIs<OptionalType>(variableType)
		assertIs<ObjectType>(variableType.baseType)
	}

	@Test
	fun `is preserved in function call in own type definition`() {
		val sourceCode =
			"""
				Device object {
					to getAndLog(): Self {
						var device = getIfAllowed()
						return device
					}
					to getIfAllowed(): Self {
						return this
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableType = lintResult.find<LocalVariableDeclaration> { localVariableDeclaration ->
			localVariableDeclaration.name == "device" }?.providedType
		assertNotNull(variableType)
		assertIs<SelfType>(variableType)
	}

	@Test
	fun `is converted in function call outside of own type definition`() {
		val sourceCode =
			"""
				Device object {
					to getIfAllowed(): Self {
						return this
					}
				}
				DeviceManager object {
					to getAndLogDevice(): Device {
						var device = Device.getIfAllowed()
						return device
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableType = lintResult.find<LocalVariableDeclaration> { localVariableDeclaration ->
			localVariableDeclaration.name == "device" }?.providedType
		assertNotNull(variableType)
		assertIs<ObjectType>(variableType)
	}

	@Test
	fun `is preserved in binary operator call in own type definition`() {
		val sourceCode =
			"""
				Device object {
					to getAndLog(): Self {
						var device = this + this
						return device
					}
					monomorphic operator +(other: Self): Self {
						return this
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableType = lintResult.find<LocalVariableDeclaration> { localVariableDeclaration ->
			localVariableDeclaration.name == "device" }?.providedType
		assertNotNull(variableType)
		assertIs<SelfType>(variableType)
	}

	@Test
	fun `is converted in binary operator call outside of own type definition`() {
		val sourceCode =
			"""
				Device object {
					monomorphic operator +(other: Self): Self {
						return this
					}
				}
				DeviceManager object {
					to getAndLogDevice(): Device {
						var device = Device + Device
						return device
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableType = lintResult.find<LocalVariableDeclaration> { localVariableDeclaration ->
			localVariableDeclaration.name == "device" }?.providedType
		assertNotNull(variableType)
		assertIs<ObjectType>(variableType)
	}

	@Test
	fun `is preserved in unary operator call in own type definition`() {
		val sourceCode =
			"""
				Device object {
					to getAndLog(): Self {
						var device = !this
						return device
					}
					operator !: Self {
						return this
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableType = lintResult.find<LocalVariableDeclaration> { localVariableDeclaration ->
			localVariableDeclaration.name == "device" }?.providedType
		assertNotNull(variableType)
		assertIs<SelfType>(variableType)
	}

	@Test
	fun `is converted in unary operator call outside of own type definition`() {
		val sourceCode =
			"""
				Device object {
					operator !: Self {
						return this
					}
				}
				DeviceManager object {
					to getAndLogDevice(): Device {
						var device = !Device
						return device
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableType = lintResult.find<LocalVariableDeclaration> { localVariableDeclaration ->
			localVariableDeclaration.name == "device" }?.providedType
		assertNotNull(variableType)
		assertIs<ObjectType>(variableType)
	}

	@Test
	fun `is preserved in index operator call in own type definition`() {
		val sourceCode =
			"""
				Device object {
					to getAndLog(): Self {
						var device = this[]
						return device
					}
					operator []: Self {
						return this
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableType = lintResult.find<LocalVariableDeclaration> { localVariableDeclaration ->
			localVariableDeclaration.name == "device" }?.providedType
		assertNotNull(variableType)
		assertIs<SelfType>(variableType)
	}

	@Test
	fun `is converted in index operator call outside of own type definition`() {
		val sourceCode =
			"""
				Device object {
					operator []: Self {
						return this
					}
				}
				DeviceManager object {
					to getAndLogDevice(): Device {
						var device = Device[]
						return device
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableType = lintResult.find<LocalVariableDeclaration> { localVariableDeclaration ->
			localVariableDeclaration.name == "device" }?.providedType
		assertNotNull(variableType)
		assertIs<ObjectType>(variableType)
	}
}
