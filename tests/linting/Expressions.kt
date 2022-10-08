package linting

import linting.semantic_model.access.MemberAccess
import linting.semantic_model.control_flow.Try
import linting.semantic_model.definitions.Class
import linting.semantic_model.definitions.TypeDefinition
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.literals.OptionalType
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
		assertTrue(Linter.LiteralType.BOOLEAN.matches(nullCheck?.type))
	}

	@Test
	fun `returns optional type from optional member access`() {
		val sourceCode =
			"""
				class Brightness {}
				class Star {
					var brightness: Brightness
					init
				}
				val sun: Star? = Star()
				sun?.brightness
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val memberAccess = lintResult.find<MemberAccess>()
		assertIs<OptionalType>(memberAccess?.type)
	}

	@Test
	fun `detects member access on optional type`() {
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
	fun `detects unnecessary optional member access`() {
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
		assertTrue(Linter.LiteralType.BOOLEAN.matches(positiveCast?.type))
		assertTrue(Linter.LiteralType.BOOLEAN.matches(negativeCast?.type))
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
		val castResultType = cast?.type as? OptionalType
		assertNotNull(castResultType)
		assertEquals(vehicleClass, (castResultType.baseType as? ObjectType)?.definition)
	}

	@Test
	fun `detects missing casts conditions`() {
		val sourceCode =
			"""
				class Orange {}
				object Apple {}
				Apple as Orange
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Cannot safely cast 'Apple' to 'Orange'.")
	}

	@Test
	fun `detects unnecessary cast conditions`() {
		val sourceCode =
			"""
				class Fruit {}
				object Apple: Fruit {}
				Apple as? Fruit
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Cast from 'Apple' to 'Fruit' is safe.")
	}

	@Test
	fun `returns source expression type from uncheck try`() {
		val sourceCode =
			"""
				class PrintResult {}
				object Printer {
					to print(): PrintResult {}
				}
				try! Printer.print()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val typeDefinition = lintResult.find<Class> { `class` -> `class`.name == "PrintResult" }
		val tryType = lintResult.find<Try>()?.type
		assertIs<ObjectType>(tryType)
		assertEquals(typeDefinition, tryType.definition)
	}

	@Test
	fun `returns optional type from optional try`() {
		val sourceCode =
			"""
				class PrintResult {}
				object Printer {
					to print(): PrintResult {}
				}
				try? Printer.print()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val typeDefinition = lintResult.find<Class> { `class` -> `class`.name == "PrintResult" }
		val tryType = lintResult.find<Try>()?.type
		assertIs<OptionalType>(tryType)
		assertEquals(typeDefinition, (tryType.baseType as? ObjectType)?.definition)
	}
}