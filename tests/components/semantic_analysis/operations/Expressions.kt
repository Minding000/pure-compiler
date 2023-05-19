package components.semantic_analysis.operations

import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.control_flow.Try
import components.semantic_analysis.semantic_model.definitions.Class
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.operations.Cast
import components.semantic_analysis.semantic_model.operations.MemberAccess
import components.semantic_analysis.semantic_model.operations.NullCheck
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.OptionalType
import components.semantic_analysis.semantic_model.values.VariableValue
import logger.Severity
import logger.issues.access.GuaranteedAccessWithNullCheck
import logger.issues.access.OptionalAccessWithoutNullCheck
import logger.issues.constant_conditions.*
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.*

internal class Expressions {

	@Test
	fun `returns boolean from null checks`() {
		val sourceCode =
			"""
				referencing Pure
				val a: Int? = 5
				val b = a?
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val nullCheck = lintResult.find<NullCheck>()
		assertTrue(SpecialType.BOOLEAN.matches(nullCheck?.type))
	}

	@Test
	fun `returns optional type from optional member access`() {
		val sourceCode =
			"""
				Brightness class
				Star class {
					var brightness: Brightness
				}
				val sun: Star? = Star()
				sun?.brightness
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val memberAccess = lintResult.find<MemberAccess>()
		assertIs<OptionalType>(memberAccess?.type)
	}

	@Test
	fun `doesn't wrap optionally accessed optional members in an additional optional type`() {
		val sourceCode =
			"""
				Seat class
				Car class {
					var driverSeat: Seat? = null
				}
				val carInDriveway: Car? = Car()
				val seat = carInDriveway?.driverSeat
				seat
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableType = lintResult.find<VariableValue> { variableValue -> variableValue.name == "seat" }?.type
		assertIs<OptionalType>(variableType)
		assertIs<ObjectType>(variableType.baseType)
	}

	@Test
	fun `detects member access on optional type`() {
		val sourceCode =
			"""
				Star class {
					native to shine()
				}
				val sun: Star? = null
				sun.shine()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OptionalAccessWithoutNullCheck>(
			"Cannot access member of optional type 'Null' without null check.", Severity.ERROR)
	}

	@Test
	fun `allows member access on optional type with non-null value`() {
		val sourceCode =
			"""
				Star class {
					native to shine()
				}
				val sun: Star? = Star()
				sun.shine()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<OptionalAccessWithoutNullCheck>()
	}

	@Test
	fun `detects unnecessary optional member access`() {
		val sourceCode =
			"""
				Star class {
					native to shine()
				}
				val sun: Star? = Star()
				sun?.shine()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<GuaranteedAccessWithNullCheck>(
			"Optional member access on guaranteed type 'Star' is unnecessary.", Severity.WARNING)
	}

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
		val vehicleClass = lintResult.find<TypeDefinition> { typeDefinition -> typeDefinition.name == "Vehicle" }
		val cast = lintResult.find<Cast>()
		assertEquals(vehicleClass, (cast?.type as? ObjectType)?.definition)
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
		assertNull(variableValue.definition)
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
		assertNotNull(variableValue.definition)
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
		assertNull(variableValue.definition)
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
		assertNotNull(variableValue.definition)
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
		val vehicleClass = lintResult.find<TypeDefinition> { typeDefinition -> typeDefinition.name == "Vehicle" }
		val cast = lintResult.find<Cast>()
		val castResultType = cast?.type as? OptionalType
		assertNotNull(castResultType)
		assertEquals(vehicleClass, (castResultType.baseType as? ObjectType)?.definition)
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

	@Test
	fun `returns source expression type from uncheck try`() {
		val sourceCode =
			"""
				PrintResult class
				Printer object {
					to print(): PrintResult
				}
				try! Printer.print()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val typeDefinition = lintResult.find<Class> { `class` -> `class`.name == "PrintResult" }
		val tryType = lintResult.find<Try>()?.type
		assertIs<ObjectType>(tryType)
		assertEquals(typeDefinition, tryType.definition)
	}

	@Test
	fun `returns optional type from optional try`() {
		val sourceCode =
			"""
				PrintResult class
				Printer object {
					to print(): PrintResult
				}
				try? Printer.print()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val typeDefinition = lintResult.find<Class> { `class` -> `class`.name == "PrintResult" }
		val tryType = lintResult.find<Try>()?.type
		assertIs<OptionalType>(tryType)
		assertEquals(typeDefinition, (tryType.baseType as? ObjectType)?.definition)
	}

	@Test
	fun `detects null checks that always return yes`() {
		val sourceCode =
			"""
				Cable class
				var cable: Cable? = null
				cable = Cable()
				if cable? {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<StaticNullCheckValue>("Null check always returns 'yes'.", Severity.WARNING)
	}

	@Test
	fun `detects null checks that always return no`() {
		val sourceCode =
			"""
				Cable class
				var noCable: Cable? = Cable()
				noCable = null
				if noCable? {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<StaticNullCheckValue>("Null check always returns 'no'.", Severity.WARNING)
	}

	@Test
	fun `allows type specifications on initializers`() {
		val sourceCode =
			"""
				Metal class
				List class
				<Metal>List()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeSpecificationOutsideOfInitializerCall>()
	}

	@Test
	fun `disallows type specifications on functions`() {
		val sourceCode =
			"""
				Metal class
				Cable object {
					to transmit()
				}
				Cable.<Metal>transmit()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<TypeSpecificationOutsideOfInitializerCall>(
			"Type specifications can only be used on initializers.", Severity.ERROR)
	}
}
