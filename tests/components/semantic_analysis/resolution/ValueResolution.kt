package components.semantic_analysis.resolution

import components.semantic_analysis.semantic_model.declarations.ComputedPropertyDeclaration
import components.semantic_analysis.semantic_model.declarations.Parameter
import components.semantic_analysis.semantic_model.declarations.PropertyDeclaration
import components.semantic_analysis.semantic_model.values.VariableValue
import logger.Severity
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.declaration.ComputedPropertyMissingType
import logger.issues.declaration.DeclarationMissingTypeOrValue
import logger.issues.initialization.CircularAssignment
import logger.issues.modifiers.*
import logger.issues.resolution.NotCallable
import logger.issues.resolution.NotFound
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

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
					val size gets Int()
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
					val livingAreaInSquareMeters gets Flat.size
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
		val member = variableValue?.type?.interfaceScope?.getValueDeclaration("isOpen")
		assertIs<PropertyDeclaration>(member)
		assertEquals("no", member.value?.source?.getValue())
	}

	@Test
	fun `allows for properties to be overridden`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Food class {
					val nutritionScore: Number
				}
				Vegetable class: Food
				Potato class: Vegetable {
					overriding val nutritionScore: Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingOverridingKeyword>()
		lintResult.assertIssueNotDetected<OverriddenSuperMissing>()
		lintResult.assertIssueNotDetected<OverridingPropertyTypeNotAssignable>()
		lintResult.assertIssueNotDetected<OverridingPropertyTypeMismatch>()
		lintResult.assertIssueNotDetected<VariablePropertyOverriddenByValue>()
	}

	@Test
	fun `detects missing overriding keyword on properties`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Food class {
					val nutritionScore: Number
				}
				Vegetable class: Food
				Potato class: Vegetable {
					val nutritionScore: Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingOverridingKeyword>(
			"Property 'nutritionScore: Float' is missing the 'overriding' keyword.")
	}

	@Test
	fun `detects overriding keyword being used without super property`() {
		val sourceCode =
			"""
				Room class {
					overriding val isClean = yes
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverriddenSuperMissing>(
			"'overriding' keyword is used, but the property doesn't have a super property.")
	}

	@Test
	fun `detects overriding value property with incompatible type`() { //TODO write similar tests for function & operator return types
		val sourceCode =
			"""
				Number class
				Float class: Number
				Food class {
					val nutritionScore: Number
				}
				Potato class: Food {
					overriding val nutritionScore: Food
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverridingPropertyTypeNotAssignable>(
			"Type of overriding property 'Food' is not assignable to the type of the overridden property 'Number'.",
			Severity.ERROR)
	}

	@Test
	fun `detects overriding variable property with incompatible type`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Food class {
					var nutritionScore: Number
				}
				Potato class: Food {
					overriding var nutritionScore: Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverridingPropertyTypeMismatch>(
			"Type of overriding property 'Float' does not match the type of the overridden property 'Number'.", Severity.ERROR)
	}

	@Test
	fun `detects variable property being overridden by value property`() {
		val sourceCode =
			"""
				Number class
				Food class {
					var nutritionScore: Number
				}
				Potato class: Food {
					overriding val nutritionScore: Number
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<VariablePropertyOverriddenByValue>(
			"Variable super property 'nutritionScore' cannot be overridden by value property.", Severity.ERROR)
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
}
