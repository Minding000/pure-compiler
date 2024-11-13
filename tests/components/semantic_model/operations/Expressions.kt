package components.semantic_model.operations

import components.semantic_model.context.SpecialType
import components.semantic_model.control_flow.Try
import components.semantic_model.declarations.Class
import components.semantic_model.types.ObjectType
import components.semantic_model.types.OptionalType
import components.semantic_model.values.VariableValue
import logger.Severity
import logger.issues.access.GuaranteedAccessWithHasValueCheck
import logger.issues.access.OptionalAccessWithoutHasValueCheck
import logger.issues.constant_conditions.StaticHasValueCheckResult
import logger.issues.constant_conditions.TypeSpecificationOutsideOfInitializerCall
import logger.issues.resolution.NotFound
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class Expressions {

	@Test
	fun `returns boolean from has-value checks`() {
		val sourceCode =
			"""
				referencing Pure
				val a: Int? = 5
				val b = a?
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val hasValueCheck = lintResult.find<HasValueCheck>()
		assertTrue(SpecialType.BOOLEAN.matches(hasValueCheck?.providedType))
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
		assertIs<OptionalType>(memberAccess?.providedType)
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
		val variableType = lintResult.find<VariableValue> { variableValue -> variableValue.name == "seat" }?.providedType
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
		lintResult.assertIssueDetected<OptionalAccessWithoutHasValueCheck>(
			"Cannot access member of optional type 'Null' without has-value check.", Severity.ERROR)
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
		lintResult.assertIssueNotDetected<OptionalAccessWithoutHasValueCheck>()
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
		lintResult.assertIssueDetected<GuaranteedAccessWithHasValueCheck>(
			"Optional member access on guaranteed type 'Star' is unnecessary.", Severity.WARNING)
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
		val tryType = lintResult.find<Try>()?.providedType
		assertIs<ObjectType>(tryType)
		assertEquals(typeDefinition, tryType.getTypeDeclaration())
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
		val tryType = lintResult.find<Try>()?.providedType
		assertIs<OptionalType>(tryType)
		assertEquals(typeDefinition, (tryType.baseType as? ObjectType)?.getTypeDeclaration())
	}

	@Test
	fun `detects has-value checks that always return yes`() {
		val sourceCode =
			"""
				Cable class
				var cable: Cable? = null
				cable = Cable()
				if cable? {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<StaticHasValueCheckResult>("Has-value check always returns 'yes'.", Severity.WARNING)
	}

	@Test
	fun `detects has-value checks that always return no`() {
		val sourceCode =
			"""
				Cable class
				var noCable: Cable? = Cable()
				noCable = null
				if noCable? {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<StaticHasValueCheckResult>("Has-value check always returns 'no'.", Severity.WARNING)
	}

	//TODO extract LLVM compilation from semantic model
	// advantages:
	//  - smaller classes
	//  - clear separation of properties in their domain (e.g. effective type could be a special LLVM type)
	//  - allows for desugaring
	//    - not desired in most cases
	//  - easier support for other targets
	//    - not very important
	//  - context and constructor can be added as properties when construction the new classes to avoid passing them around
	//  - context can be split into LlvmContext and SemanticModelContext
	// disadvantages:
	//  - more code / "duplication" of classes

	//TODO implement this
	// Options:
	//   1 - change type temporarily
	//   	 - not possible, because type may be read out-of-order
	//   2 - take scope into account when accessing type
	//       - not very efficient
	//       - unless done only once per variable value <-- try this first; before that split semantic model and compiler
	//   3 - create an implicit copy of the variable
	//       - may interfere with other systems that track the variable e.g. shadow lint issue
	//   4 - change the syntax to create an explicit variable
	//   	- extra syntax
	//      - choosing a new name for a non-optional version is not helpful
	//   5 - just use 'is! Null' - does this work? (or use not-as?)
	//		- probably doesn't as this is the same concept
	//      - using 'as' is not an option as it requires the user to define another variable with a different name
	@Disabled
	@Test
	fun `variable is non-optional after has-value check`() {
		val sourceCode =
			"""
				Cable class
				var cable: Cable? = Cable()
				if cable? {
					cable
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val type =
			lintResult.find<VariableValue> { variableValue -> variableValue.name == "cable" && variableValue.parent !is HasValueCheck }?.providedType
		assertIs<ObjectType>(type)
		assertEquals("Cable", type.toString())
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

	@Test
	fun `doesn't require null coalescence operator implementation`() {
		val sourceCode =
			"""
				1 ?? 0
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotFound>()
	}

	@Test
	fun `null coalescence operator returns or union of operand types`() {
		val sourceCode =
			"""
				1 ?? yes
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val nullCoalescenceOperator = lintResult.find<BinaryOperator>()
		assertNotNull(nullCoalescenceOperator)
		assertEquals("Bool | Int", nullCoalescenceOperator.providedType.toString())
	}

	@Test
	fun `null coalescence operator returns non-optional type if right operand type is non-optional`() {
		val sourceCode =
			"""
				Int class
				var number: Int? = null
				number ?? yes
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val nullCoalescenceOperator = lintResult.find<BinaryOperator>()
		assertNotNull(nullCoalescenceOperator)
		assertEquals("Bool | Int", nullCoalescenceOperator.providedType.toString())
	}

	@Test
	fun `null coalescence operator ignores left operand if it is null`() {
		val sourceCode =
			"""
				null ?? yes
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val nullCoalescenceOperator = lintResult.find<BinaryOperator>()
		assertNotNull(nullCoalescenceOperator)
		assertEquals("Bool", nullCoalescenceOperator.providedType.toString())
	}
}
