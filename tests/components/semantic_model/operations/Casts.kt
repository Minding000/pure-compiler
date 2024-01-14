package components.semantic_model.operations

import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.types.ObjectType
import components.semantic_model.types.OptionalType
import components.semantic_model.values.VariableValue
import logger.Severity
import logger.issues.constant_conditions.*
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class Casts {

	@Test
	fun `returns new type after force cast`() {
		val sourceCode =
			"""
				Vehicle class
				Bus class: Vehicle
				Cinema class
				val roomWithSeats: Bus|Cinema = Bus()
				roomWithSeats as! Vehicle
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val vehicleClass = lintResult.find<TypeDeclaration> { typeDefinition -> typeDefinition.name == "Vehicle" }
		val cast = lintResult.find<Cast>()
		assertEquals(vehicleClass, (cast?.type as? ObjectType)?.getTypeDeclaration())
	}

	@Test
	fun `returns boolean type for conditional casts`() {
		val sourceCode =
			"""
				Vehicle class
				Bus class: Vehicle
				Cinema class
				val roomWithSeats: Bus|Cinema = Bus()
				val isRoomVehicle = roomWithSeats is Vehicle
				val isRoomNotVehicle = roomWithSeats is! Vehicle
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val positiveCast = lintResult.find<Cast> { cast -> cast.operator == Cast.Operator.CAST_CONDITION }
		val negativeCast = lintResult.find<Cast> { cast -> cast.operator == Cast.Operator.NEGATED_CAST_CONDITION }
		assertTrue(SpecialType.BOOLEAN.matches(positiveCast?.type))
		assertTrue(SpecialType.BOOLEAN.matches(negativeCast?.type))
	}

	@Test
	fun `declares variable in conditional casts without negation`() {
		val sourceCode =
			"""
				Vehicle class
				Bus class: Vehicle
				Cinema class
				val roomWithSeats: Bus|Cinema = Bus()
				if roomWithSeats is vehicle: Vehicle {
					vehicle
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val castVariable = lintResult.find<VariableValue> { variableValue -> variableValue.name == "vehicle" }
		assertNotNull(castVariable?.type)
	}

	@Test
	fun `declares variable in conditional casts with negation`() {
		val sourceCode =
			"""
				Vehicle class
				Bus class: Vehicle
				Cinema class
				val roomWithSeats: Bus|Cinema = Bus()
				loop {
					if roomWithSeats is! vehicle: Vehicle
						break
					vehicle
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val castVariable = lintResult.find<VariableValue> { variableValue -> variableValue.name == "vehicle" }
		assertNotNull(castVariable?.type)
	}

	@Test
	fun `cast variable is not available before cast`() {
		val sourceCode =
			"""
				Car class
				{
					val something: Car
					car
					if something is car: Car {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "car" }
		assertNotNull(variableValue)
		assertNull(variableValue.declaration)
	}

	@Test
	fun `cast variable is not available in negative branch`() {
		val sourceCode =
			"""
				Car class
				val something: Car
				if something is car: Car {
				} else {
					car
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<CastVariableAccessInNegativeBranch>(
			"Cannot access cast variable in negative branch.", Severity.ERROR)
	}

	@Test
	fun `cast variable is not available after if statement`() {
		val sourceCode =
			"""
				Car class
				val something: Car
				if something is car: Car {}
				car
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<CastVariableAccessAfterIfStatement>(
			"Cannot access cast variable after if statement.", Severity.ERROR)
	}

	@Test
	fun `cast variable is available after if statement if negative branch interrupts execution`() {
		val sourceCode =
			"""
				Car class
				val something: Car
				loop {
					if something is car: Car {
					} else {
						break
					}
					car
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "car" }
		assertNotNull(variableValue)
		assertNotNull(variableValue.declaration)
	}

	@Test
	fun `negated cast variable is not available before cast`() {
		val sourceCode =
			"""
				Car class
				{
					val something: Car
					car
					if something is! car: Car {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "car" }
		assertNotNull(variableValue)
		assertNull(variableValue.declaration)
	}

	@Test
	fun `negated cast variable is not available in positive branch`() {
		val sourceCode =
			"""
				Car class
				val something: Car
				if something is! car: Car {
					car
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<NegatedCastVariableAccessInPositiveBranch>(
			"Cannot access negated cast variable in positive branch.", Severity.ERROR)
	}

	@Test
	fun `negated cast variable is not available after if statement`() {
		val sourceCode =
			"""
				Car class
				val something: Car
				if something is! car: Car {}
				car
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<CastVariableAccessAfterIfStatement>(
			"Cannot access cast variable after if statement.", Severity.ERROR)
	}

	@Test
	fun `negated cast variable is available after if statement if positive branch interrupts execution`() {
		val sourceCode =
			"""
				Car class
				val something: Car
				loop {
					if something is car: Car
						break
					car
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "car" }
		assertNotNull(variableValue)
		assertNotNull(variableValue.declaration)
	}

	@Test
	fun `returns optional new type after optional cast`() {
		val sourceCode =
			"""
				Vehicle class
				Bus class: Vehicle
				Cinema class
				val roomWithSeats: Bus|Cinema = Bus()
				roomWithSeats as? Vehicle
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val vehicleClass = lintResult.find<TypeDeclaration> { typeDefinition -> typeDefinition.name == "Vehicle" }
		val cast = lintResult.find<Cast>()
		val castResultType = cast?.type as? OptionalType
		assertNotNull(castResultType)
		assertEquals(vehicleClass, (castResultType.baseType as? ObjectType)?.getTypeDeclaration())
	}

	@Test
	fun `detects missing casts conditions`() {
		val sourceCode =
			"""
				Orange class
				Apple object
				Apple as Orange
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<UnsafeSafeCast>("Cannot safely cast 'Apple' to 'Orange'.", Severity.ERROR)
	}

	@Test
	fun `detects unnecessary cast conditions`() {
		val sourceCode =
			"""
				Fruit class
				Apple object: Fruit
				Apple as? Fruit
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ConditionalCastIsSafe>("Cast from 'Apple' to 'Fruit' is safe.", Severity.WARNING)
	}
}
