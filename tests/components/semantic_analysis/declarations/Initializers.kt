package components.semantic_analysis.declarations

import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.Parameter
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class Initializers {

	@Test
	fun `allows initializers in classes`() {
		val sourceCode =
			"""
				Human class {
					init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Initializers are only allowed inside")
	}

	@Test
	fun `allows initializers in enums`() {
		val sourceCode =
			"""
				Mood enum {
					init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Initializers are only allowed inside")
	}

	@Test
	fun `allows initializers inside objects without parameters`() {
		val sourceCode =
			"""
				Root object {
					init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "can not take parameters")
	}

	@Test
	fun `detects initializers inside objects with type parameters`() {
		val sourceCode =
			"""
				Root object {
					init(ChildType;)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Object initializers can not take type parameters")
	}

	@Test
	fun `detects initializers inside objects with parameters`() {
		val sourceCode =
			"""
				Root object {
					init(childCount: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Object initializers can not take parameters")
	}

	@Test
	fun `detects redeclarations of initializer signatures`() {
		val sourceCode =
			"""
				Trait class
				alias T = Trait
				Human class {
					init
					init(t: T)
					init(t: Trait)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Redeclaration of initializer 'Human(Trait)'")
	}

	@Test
	fun `creates default initializer if no initializer is defined`() {
		val sourceCode =
			"""
				Human class
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val defaultInitializer = lintResult.find<InitializerDefinition>()
		assertNotNull(defaultInitializer)
	}

	@Test
	fun `resolves property parameters in initializers`() {
		val sourceCode =
			"""
				Int class
				Human class {
					val age: Int
					init(age)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Property parameters are only allowed in initializers")
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Property parameter doesn't match any property")
		val parameter = lintResult.find<Parameter>()
		assertEquals("Int", parameter?.type.toString())
	}

	@Test
	fun `disallows property parameters without matching property`() {
		val sourceCode =
			"""
				Human class {
					init(age)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Property parameter doesn't match any property")
	}

	@Test
	fun `disallows property parameters outside of initializers`() {
		val sourceCode =
			"""
				Human class {
					val age: Int
					to set(age)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Property parameters are only allowed in initializers")
	}

	@Test
	fun `allows abstract classes to contain abstract initializers`() {
		val sourceCode =
			"""
				Int class
				abstract Plant class {
					abstract init(size: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not allowed in non-abstract class")
	}

	@Test
	fun `disallows non-abstract classes to contain abstract initializers`() {
		val sourceCode =
			"""
				Int class
				Plant class {
					abstract init(size: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Abstract member 'init(Int)' is not allowed in non-abstract class 'Plant'")
	}

	@Test
	fun `allows abstract classes to not override abstract initializers`() {
		val sourceCode =
			"""
				Int class
				abstract Plant class {
					abstract init(size: Int)
				}
				abstract Tree class: Plant
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "does not implement the following inherited members")
	}

	@Test
	fun `allows non-abstract classes to not override non-abstract initializers`() {
		val sourceCode =
			"""
				Int class
				abstract Plant class {
					init(size: Int)
				}
				Tree class: Plant
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "does not implement the following inherited members")
	}

	@Test
	fun `allows abstract classes to override abstract initializers`() {
		val sourceCode =
			"""
				Int class
				abstract Plant class {
					abstract init(size: Int)
				}
				Tree class: Plant {
					overriding init(size: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "does not implement the following inherited members")
	}

	@Test
	fun `disallows non-abstract classes to not override abstract initializers`() {
		val sourceCode =
			"""
				Int class
				abstract Plant class {
					abstract init(size: Int)
				}
				Tree class: Plant
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, """
			Non-abstract class 'Tree' does not implement the following inherited members:
			 - Plant
			   - init(Int)
		""".trimIndent())
	}

	@Test
	fun `allows overriding converting initializers with converting initializer`() {
		val sourceCode =
			"""
				Int class
				abstract Number class {
					converting abstract init(value: Int)
				}
				Float class: Number {
					overriding converting init(value: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"Overriding initializer of converting initializer needs to be converting")
	}

	@Test
	fun `allows overriding non-converting initializers with non-converting initializer`() {
		val sourceCode =
			"""
				Int class
				abstract Number class {
					abstract init(value: Int)
				}
				Float class: Number {
					overriding init(value: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"Overriding initializer of converting initializer needs to be converting")
	}

	@Test
	fun `allows overriding non-converting initializers with converting initializer`() {
		val sourceCode =
			"""
				Float class
				abstract Number class {
					abstract init(value: Float)
				}
				Double class: Number {
					overriding converting init(value: Float)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"Overriding initializer of converting initializer needs to be converting")
	}

	@Test
	fun `disallows overriding converting initializers with non-converting initializer`() {
		val sourceCode =
			"""
				Int class
				abstract Number class {
					converting abstract init(value: Int)
				}
				Float class: Number {
					overriding init(value: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Overriding initializer of converting initializer needs to be converting")
	}
}
