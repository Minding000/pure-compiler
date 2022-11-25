package components.semantic_analysis

import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.values.VariableValue
import components.semantic_analysis.semantic_model.values.VariableValueDeclaration
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertNotNull

internal class TypeParameters {

	@Test
	fun `consuming type accepts base type`() {
		val sourceCode =
			"""
			Paper class {
				init
			}
			TrashCan class {
				containing T

				init

				to put(trash: T) {}
			}
			val recyclingBin = <Paper consuming>TrashCan()
			val trash = Paper()
			recyclingBin.put(trash)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val baseTypeVariable = lintResult.find<VariableValue> { variableValue -> variableValue.name == "trash" }
		val specificType = lintResult.find<VariableValue> { variableValue -> variableValue.name == "recyclingBin" }?.type
		val functionType = specificType?.scope?.resolveValue("put")?.type as? FunctionType
		assertNotNull(functionType)
		assertNotNull(baseTypeVariable)
		val signature = functionType.resolveSignature(listOf(baseTypeVariable))
		assertNotNull(signature)
	}

	@Test
	fun `producing type doesn't accept base type`() {
		val sourceCode =
			"""
			SoftDrink class {
				init
			}
			StorageRoom class {
				containing Item

				init

				to store(item: Item) {}
				to get(): Item {}
			}
			val softDrinkSupply = <SoftDrink producing>StorageRoom()
			val softDrink = SoftDrink()
			softDrinkSupply.store(softDrink)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val baseTypeVariable = lintResult.find<VariableValue> { variableValue -> variableValue.name == "softDrink" }
		val specificType = lintResult.find<VariableValue> { variableValue -> variableValue.name == "softDrinkSupply" }?.type
		val functionType = specificType?.scope?.resolveValue("store")?.type as? FunctionType
		assertNotNull(functionType)
		assertNotNull(baseTypeVariable)
		val signature = functionType.resolveSignature(listOf(baseTypeVariable))
		assertNull(signature)
	}

	@Test
	fun `consuming type is only assignable to 'Any' type`() {
		val sourceCode =
			"""
			SoftDrink class {
				init
			}
			StorageRoom class {
				containing Item

				init

				to store(item: Item) {}
				to get(): Item {}
			}
			val softDrinkDestination = <SoftDrink consuming>StorageRoom()
			val softDrink: SoftDrink = softDrinkDestination.get()
			val item: Any = softDrinkDestination.get()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val baseType = lintResult.find<VariableValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.name == "softDrink" }?.type
		val specificType = lintResult.find<VariableValue> { variableValue -> variableValue.name == "softDrinkDestination" }?.type
		val functionType = specificType?.scope?.resolveValue("get")?.type as? FunctionType
		val anyType = lintResult.find<VariableValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.type.toString() == "Any" }?.type
		assertNotNull(functionType)
		assertNotNull(anyType)
		val signature = functionType.resolveSignature()
		assertNotNull(signature)
		assertNotNull(baseType)
		assertFalse(signature.returnType.isAssignableTo(baseType))
		assertTrue(signature.returnType.isAssignableTo(anyType))
	}

	@Test
	fun `producing type is assignable to base type`() {
		val sourceCode =
			"""
			SoftDrink class {
				init
			}
			StorageRoom class {
				containing Item

				init

				to store(item: Item) {}
				to get(): Item {}
			}
			val softDrinkSupply = <SoftDrink producing>StorageRoom()
			val softDrink: SoftDrink = softDrinkSupply.get()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val baseType = lintResult.find<VariableValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.name == "softDrink" }?.type
		val specificType = lintResult.find<VariableValue> { variableValue -> variableValue.name == "softDrinkSupply" }?.type
		val functionType = specificType?.scope?.resolveValue("get")?.type as? FunctionType
		assertNotNull(functionType)
		val signature = functionType.resolveSignature()
		assertNotNull(signature)
		assertNotNull(baseType)
		assertTrue(signature.returnType.isAssignableTo(baseType))
	}
}
