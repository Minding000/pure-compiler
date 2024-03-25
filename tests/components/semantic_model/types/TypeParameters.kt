package components.semantic_model.types

import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.values.VariableValue
import logger.Severity
import logger.issues.declaration.TypeParameterCountMismatch
import logger.issues.declaration.TypeParameterNotAssignable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertNotNull

internal class TypeParameters {

	@Test
	fun `consuming type accepts base type`() {
		val sourceCode =
			"""
			Paper class
			TrashCan class {
				containing T
				to put(trash: T) {}
			}
			val recyclingBin = <Paper consuming>TrashCan()
			val trash = Paper()
			recyclingBin.put(trash)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val baseTypeVariable = lintResult.find<VariableValue> { variableValue -> variableValue.name == "trash" }
		val specificType = lintResult.find<VariableValue> { variableValue ->
			variableValue.name == "recyclingBin" }?.providedType
		val functionType = specificType?.interfaceScope?.getValueDeclaration("put")?.type as? FunctionType
		assertNotNull(functionType)
		assertNotNull(baseTypeVariable)
		val signature = functionType.getSignature(listOf(baseTypeVariable))
		assertNotNull(signature)
	}

	@Test
	fun `producing type doesn't accept base type`() {
		val sourceCode =
			"""
			SoftDrink class
			StorageRoom class {
				containing Item
				to store(item: Item) {}
				to get(): Item {}
			}
			val softDrinkSupply = <SoftDrink producing>StorageRoom()
			val softDrink = SoftDrink()
			softDrinkSupply.store(softDrink)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val baseTypeVariable = lintResult.find<VariableValue> { variableValue -> variableValue.name == "softDrink" }
		val specificType = lintResult.find<VariableValue> { variableValue ->
			variableValue.name == "softDrinkSupply" }?.providedType
		val functionType = specificType?.interfaceScope?.getValueDeclaration("store")?.type as? FunctionType
		assertNotNull(functionType)
		assertNotNull(baseTypeVariable)
		val signature = functionType.getSignature(listOf(baseTypeVariable))
		assertNull(signature)
	}

	@Test
	fun `consuming type is only assignable to 'Any' type`() {
		val sourceCode =
			"""
			referencing Pure
			SoftDrink class
			StorageRoom class {
				containing Item
				to store(item: Item) {}
				to get(): Item {}
			}
			val softDrinkDestination = <SoftDrink consuming>StorageRoom()
			val softDrink: SoftDrink = softDrinkDestination.get()
			val item: Any = softDrinkDestination.get()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val baseType = lintResult.find<ValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.name == "softDrink" }?.type
		val specificType = lintResult.find<VariableValue> { variableValue ->
			variableValue.name == "softDrinkDestination" }?.providedType
		val functionType = specificType?.interfaceScope?.getValueDeclaration("get")?.type as? FunctionType
		val anyType = lintResult.find<ValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.type.toString() == "Any" }?.type
		assertNotNull(functionType)
		assertNotNull(anyType)
		val signature = functionType.getSignature()
		assertNotNull(signature)
		assertNotNull(baseType)
		assertFalse(signature.returnType.isAssignableTo(baseType))
		assertTrue(signature.returnType.isAssignableTo(anyType))
	}

	@Test
	fun `producing type is assignable to base type`() {
		val sourceCode =
			"""
			SoftDrink class
			StorageRoom class {
				containing Item
				to store(item: Item) {}
				to get(): Item {}
			}
			val softDrinkSupply = <SoftDrink producing>StorageRoom()
			val softDrink: SoftDrink = softDrinkSupply.get()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val baseType = lintResult.find<ValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.name == "softDrink" }?.type
		val specificType = lintResult.find<VariableValue> { variableValue ->
			variableValue.name == "softDrinkSupply" }?.providedType
		val functionType = specificType?.interfaceScope?.getValueDeclaration("get")?.type as? FunctionType
		assertNotNull(functionType)
		val signature = functionType.getSignature()
		assertNotNull(signature)
		assertNotNull(baseType)
		assertTrue(signature.returnType.isAssignableTo(baseType))
	}

	@Test
	fun `detects incorrect number of type parameters`() {
		val sourceCode =
			"""
			SoftDrink class
			StorageRoom class {
				containing Item
			}
			val room: <SoftDrink, SoftDrink>StorageRoom
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<TypeParameterCountMismatch>(
			"Number of provided type parameters (2) doesn't match number of declared generic types (1).", Severity.ERROR)
	}

	@Test
	fun `detects incompatible type parameters`() {
		val sourceCode =
			"""
			SoftDrink class
			Dish class
			StorageRoom class {
				containing Item: SoftDrink
			}
			val room: <Dish>StorageRoom
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<TypeParameterNotAssignable>(
			"The type parameter 'Dish' is not assignable to 'Item: SoftDrink'.", Severity.ERROR)
	}

	@Test
	fun `allows for generic types to be type parameters`() {
		val sourceCode =
			"""
			SoftDrink class
			StorageRoom class {
				containing Item: SoftDrink
			}
			CooledStorageRoom class: <Item>StorageRoom {
				containing Item: SoftDrink
			}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeParameterNotAssignable>()
	}
}
