package components.semantic_model.resolution

import components.semantic_model.declarations.ComputedPropertyDeclaration
import components.semantic_model.declarations.Parameter
import components.semantic_model.declarations.PropertyDeclaration
import components.semantic_model.values.ValueDeclaration
import components.semantic_model.values.VariableValue
import logger.Severity
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.declaration.ComputedPropertyMissingType
import logger.issues.declaration.DeclarationMissingTypeOrValue
import logger.issues.initialization.CircularAssignment
import logger.issues.resolution.NotCallable
import logger.issues.resolution.NotFound
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class ValueResolution {

	@Test
	fun `emits error for undeclared variables`() {
		val sourceCode =
			"""
				numberOfDogs
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<NotFound>("Value 'numberOfDogs' hasn't been declared yet.")
	}

	@Test
	fun `resolves local variables`() {
		val sourceCode =
			"""
				val numberOfCats = 2
				numberOfCats
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "numberOfCats" }
		assertNotNull(variableValue?.declaration)
	}

	@Test
	fun `resolves instance members`() {
		val sourceCode =
			"""
				House object {
					val livingAreaInSquareMeters = 120
				}
				House.livingAreaInSquareMeters
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "livingAreaInSquareMeters" }
		assertNotNull(variableValue?.declaration)
	}

	@Test
	fun `infers type of property set to property with inferred type`() {
		val sourceCode =
			"""
				Int class
				MyHood object {
					val size = DefaultHouse.livingAreaInSquareMeters
				}
				DefaultHouse object {
					val livingAreaInSquareMeters = Int()
				}
				MyHood.size
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "size" }
		assertEquals("Int", variableValue?.type.toString())
	}

	@Test
	fun `infers type of property parameter set to property with inferred type`() {
		val sourceCode =
			"""
				Int class
				House class {
					var size = DefaultHouse.livingAreaInSquareMeters
					init(size)
				}
				DefaultHouse object {
					val livingAreaInSquareMeters: Int = Int()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DeclarationMissingTypeOrValue>()
		lintResult.assertIssueNotDetected<CircularAssignment>()
		val parameter = lintResult.find<Parameter>()
		assertEquals("Int", parameter?.type.toString())
	}

	@Test
	fun `infers type of computed properties from get expressions`() {
		val sourceCode =
			"""
				Int class
				House class {
					computed size gets Int()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ComputedPropertyMissingType>()
		val computedPropertyDeclaration = lintResult.find<ComputedPropertyDeclaration>()
		assertEquals("Int", computedPropertyDeclaration?.type.toString())
	}

	@Test
	fun `infers type of property parameter with initializer call value`() {
		val sourceCode =
			"""
				House class {
					var livingAreaInSquareMeters = Int()
					init(livingAreaInSquareMeters)
				}
				Int class
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val propertyParameter = lintResult.find<Parameter>()
		assertEquals("Int", propertyParameter?.type.toString())
	}

	@Test
	fun `infers type of property parameter with type that requires an unlinked converting initializer`() {
		val sourceCode =
			"""
				Int class: Number
				House class {
					containing N: Number
					var livingAreaInSquareMeters: N = Int()
					init(livingAreaInSquareMeters)
				}
				abstract Number class {
					converting init(value: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}

	@Test
	fun `disallows circular type inference`() {
		val sourceCode =
			"""
				House object {
					val livingAreaInSquareMeters = Flat.size
				}
				Flat object {
					val size = House.livingAreaInSquareMeters
				}
				House.livingAreaInSquareMeters
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<CircularAssignment>(
			"'livingAreaInSquareMeters' has no value, because it's part of a circular assignment.", Severity.ERROR)
		lintResult.assertIssueDetected<CircularAssignment>(
			"'size' has no value, because it's part of a circular assignment.")
	}

	@Test
	fun `disallows circular type inference including in get expressions`() {
		val sourceCode =
			"""
				House object {
					computed livingAreaInSquareMeters gets Flat.size
				}
				Flat object {
					val size = House.livingAreaInSquareMeters
				}
				House.livingAreaInSquareMeters
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<CircularAssignment>(
			"'livingAreaInSquareMeters' has no value, because it's part of a circular assignment.", Severity.ERROR)
		lintResult.assertIssueDetected<CircularAssignment>(
			"'size' has no value, because it's part of a circular assignment.")
	}

	@Test
	fun `allows assignment from same property if it is not circular`() {
		val sourceCode =
			"""
				Int class
				Tree class {
					val length: Int
					val parent: Tree?
					val totalLength: Int = length + parent?.totalLength ?? 0
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<CircularAssignment>()
	}

	@Test
	fun `resolves parameters`() {
		val sourceCode =
			"""
				House object {
					to openDoor(speed: Int) {
						speed
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "speed" }
		assertNotNull(variableValue?.declaration)
	}

	@Test
	fun `resolves super properties`() {
		val sourceCode =
			"""
				Door class {
					val isOpen = yes
				}
				GlassDoor object: Door
				GlassDoor.isOpen
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val member = variableValue?.type?.interfaceScope?.getValueDeclaration("isOpen")
		assertNotNull(member)
	}

	@Test
	fun `resolves overriding properties`() {
		val sourceCode =
			"""
				Door class {
					val isOpen = yes
				}
				TransparentDoor class: Door
				GlassDoor object: TransparentDoor {
					overriding val isOpen = no
				}
				GlassDoor.isOpen
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val member = variableValue?.type?.interfaceScope?.getValueDeclaration("isOpen")?.first
		assertIs<PropertyDeclaration>(member)
		assertEquals("no", member.value?.source?.getValue())
	}

	@Test
	fun `doesn't emit error for calls to callable value`() {
		val sourceCode =
			"""
				Bird object {
					to fly()
				}
				Bird.fly()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotCallable>()
	}

	@Test
	fun `doesn't emit error for calls to unresolved value`() {
		val sourceCode =
			"""
				Bird object
				Bird.fly()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotCallable>()
	}

	@Test
	fun `emits error for calls to uncallable value`() {
		val sourceCode =
			"""
				Bird object {
					var age = 0
				}
				Bird.age()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<NotCallable>("'Bird.age' is not callable.", Severity.ERROR)
	}

	@Test
	fun `doesn't resolve values enclosed by enclosing type of super type`() {
		val sourceCode =
			"""
				Int class
				Editor class {
					val id: Int
					Input class
				}
				AccessibleInput class: Editor.Input {
					var value = id
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val declaration = lintResult.find<ValueDeclaration> { declaration -> declaration.name == "value" }
		assertNotNull(declaration)
		val value = declaration.value
		assertNotNull(value)
		assertIs<VariableValue>(value)
		assertNull(value.declaration)
	}
}
