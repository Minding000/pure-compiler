package linting

import linting.semantic_model.definitions.TypeDefinition
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.literals.QuantifiedType
import linting.semantic_model.operations.Cast
import linting.semantic_model.operations.NullCheck
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.*

internal class Expressions {

	@Test
	fun `returns boolean from null checks`() {
		val sourceCode =
			"""
				val a: Int? = 5
				val b = a?
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val nullCheck = lintResult.find<NullCheck>()
		assertEquals(Linter.Literals.BOOLEAN, nullCheck?.type.toString())
	}

	@Test
	fun `detects missing null checks`() {
		val sourceCode =
			"""
				class Star {
					init
					native to shine()
				}
				val sun: Star? = Star()
				sun.shine()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Cannot access member of optional type 'Star?' without null check.")
	}

	@Test
	fun `detects unnecessary null checks`() {
		val sourceCode =
			"""
				class Star {
					init
					native to shine()
				}
				val sun = Star()
				sun?.shine()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.WARNING,
			"Optional member access on guaranteed type 'Star' is unnecessary.")
	}

	@Test
	fun `returns new type after force cast`() {
		val sourceCode =
			"""
				class Vehicle {}
				class Bus: Vehicle {
					init
				}
				class Cinema {}
				val roomWithSeats: Bus|Cinema = Bus()
				roomWithSeats as! Vehicle
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val vehicleClass = lintResult.find<TypeDefinition> { typeDefinition -> typeDefinition.name == "Vehicle" }
		val cast = lintResult.find<Cast>()
		assertEquals(vehicleClass, (cast?.type as? ObjectType)?.definition)
	}

	@Test
	fun `returns boolean type for conditional casts`() {
		val sourceCode =
			"""
				class Vehicle {}
				class Bus: Vehicle {
					init
				}
				class Cinema {}
				val roomWithSeats: Bus|Cinema = Bus()
				roomWithSeats is Vehicle
				roomWithSeats is! Vehicle
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val positiveCast = lintResult.find<Cast> { cast -> cast.operator == Cast.Operator.CAST_CONDITION }
		val negativeCast = lintResult.find<Cast> { cast -> cast.operator == Cast.Operator.NEGATED_CAST_CONDITION }
		assertEquals(Linter.Literals.BOOLEAN, positiveCast?.type.toString())
		assertEquals(Linter.Literals.BOOLEAN, negativeCast?.type.toString())

	}

	@Test
	fun `returns optional new type after optional cast`() {
		val sourceCode =
			"""
				class Vehicle {}
				class Bus: Vehicle {
					init
				}
				class Cinema {}
				val roomWithSeats: Bus|Cinema = Bus()
				roomWithSeats as? Vehicle
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val vehicleClass = lintResult.find<TypeDefinition> { typeDefinition -> typeDefinition.name == "Vehicle" }
		val cast = lintResult.find<Cast>()
		val castResultType = cast?.type as? QuantifiedType
		assertNotNull(castResultType)
		assertTrue(castResultType.isOptional)
		assertFalse(castResultType.hasDynamicQuantity)
		assertEquals(vehicleClass, (castResultType.baseType as? ObjectType)?.definition)
	}
}