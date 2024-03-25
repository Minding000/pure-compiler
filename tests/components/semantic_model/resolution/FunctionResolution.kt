package components.semantic_model.resolution

import components.semantic_model.operations.FunctionCall
import components.semantic_model.operations.MemberAccess
import components.semantic_model.types.FunctionType
import components.semantic_model.values.VariableValue
import logger.Severity
import logger.issues.access.AbstractMonomorphicAccess
import logger.issues.access.WhereClauseUnfulfilled
import logger.issues.modifiers.MissingOverridingKeyword
import logger.issues.modifiers.OverriddenSuperMissing
import logger.issues.modifiers.OverridingPropertyTypeMismatch
import logger.issues.modifiers.OverridingPropertyTypeNotAssignable
import logger.issues.resolution.NotFound
import logger.issues.resolution.SignatureAmbiguity
import logger.issues.resolution.SignatureMismatch
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

internal class FunctionResolution {

	@Test
	fun `resolves function calls`() {
		val sourceCode =
			"""
				Door object {
					to open()
				}
				Door.open()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "Door" }
		val functionType = variableValue?.providedType?.interfaceScope?.getValueDeclaration("open")?.type
		assertIs<FunctionType>(functionType)
		val signature = functionType.getSignature()
		assertNotNull(signature)
	}

	@Test
	fun `resolves calls to super function`() {
		val sourceCode =
			"""
				Speed class
				Door class {
					to open()
				}
				TransparentDoor class: Door
				GlassDoor object: TransparentDoor {
					to open(speed: Speed)
				}
				GlassDoor.open()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<OverridingPropertyTypeNotAssignable>()
		lintResult.assertIssueNotDetected<OverridingPropertyTypeMismatch>()
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val functionType = variableValue?.providedType?.interfaceScope?.getValueDeclaration("open")?.type
		assertIs<FunctionType>(functionType)
		val signature = functionType.getSignature()
		assertNotNull(signature)
	}

	@Test
	fun `resolves calls to overriding function`() {
		val sourceCode =
			"""
				Door class {
					to open()
				}
				TransparentDoor class: Door
				GlassDoor object: TransparentDoor {
					overriding to open()
				}
				GlassDoor.open()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val functionType = variableValue?.providedType?.interfaceScope?.getValueDeclaration("open")?.type
		assertIs<FunctionType>(functionType)
		val signature = functionType.getSignature()
		assertNotNull(signature)
	}

	@Test
	fun `detects missing overriding keyword on function`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Food class {
					to getNutritionScore(): Number
				}
				Vegetable class: Food
				Potato class: Vegetable {
					to getNutritionScore(): Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingOverridingKeyword>(
			"Function 'Potato.getNutritionScore(): Float' is missing the 'overriding' keyword.")
	}

	@Test
	fun `allows for functions to be overridden`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Food class {
					to getNutritionScore(): Number
				}
				Vegetable class: Food
				Potato class: Vegetable {
					overriding to getNutritionScore(): Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingOverridingKeyword>()
		lintResult.assertIssueNotDetected<OverriddenSuperMissing>()
	}

	@Test
	fun `detects overriding keyword being used without super function`() {
		val sourceCode =
			"""
				Room class {
					overriding to clean()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverriddenSuperMissing>(
			"'overriding' keyword is used, but the function doesn't have a super function.", Severity.WARNING)
	}

	@Test
	fun `emits error for function calls with wrong parameters`() {
		val sourceCode =
			"""
				Bright object
				Light object {
					to shine()
				}
				Light.shine(Bright)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<SignatureMismatch>(
			"The provided parameters (Bright) don't match any signature of function 'Light.shine'.", Severity.ERROR)
	}

	@Test
	fun `emits error for ambiguous function calls`() {
		val sourceCode =
			"""
				Number class
				Int class: Number
				List class {
					containing Element
					it exists(index: Int)
					it exists(element: Element)
					it exists(N: Number; index: N)
				}
				val numbers = <Int>List()
				numbers.exists(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueDetected<SignatureAmbiguity>("""
			Call to function '<Int>List.exists(Int)' is ambiguous. Matching signatures:
			 - '(Int) =>|' declared at Test.Test:5:4
			 - '(Element) =>|' declared at Test.Test:6:4
			 - '(N: Number; N) =>|' declared at Test.Test:7:4
		""".trimIndent(), Severity.ERROR)
	}

	@Test
	fun `resolves non-variadic function calls with plural type`() {
		val sourceCode =
			"""
				Int class
				IntegerList object {
					to add(...integers: ...Int) {
						addAll(integers)
					}
					to addAll(integers: ...Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> (functionCall.function as? VariableValue)?.name == "addAll" }
		assertNotNull(functionCall?.providedType)
	}

	@Test
	fun `resolves variadic function calls without variadic parameters`() {
		val sourceCode =
			"""
				Int class
				IntegerList object {
					to add(capacity: Int, ...integers: ...Int)
				}
				IntegerList.add(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertNotNull(functionCall?.providedType)
	}

	@Test
	fun `resolves variadic function calls with one variadic parameter`() {
		val sourceCode =
			"""
				Int class
				IntegerList object {
					to add(capacity: Int, ...integers: ...Int)
				}
				IntegerList.add(Int(), Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertNotNull(functionCall?.providedType)
	}

	@Test
	fun `resolves variadic function calls with multiple variadic parameters`() {
		val sourceCode =
			"""
				Int class
				IntegerList object {
					to add(capacity: Int, ...integers: ...Int)
				}
				IntegerList.add(Int(), Int(), Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertNotNull(functionCall?.providedType)
	}

	@Test
	fun `resolves the most specific signature between parameters`() {
		val sourceCode =
			"""
				Int class
				Number class
				Bottle object {
					to setVolume(volume: Number)
					to setVolume(volume: Int)
				}
				Bottle.setVolume(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertEquals("(Int) =>|", functionCall?.targetSignature.toString())
	}

	@Test
	fun `resolves the most specific signature ignoring extraneous variadic parameter`() {
		val sourceCode =
			"""
				Int class
				Bottle object {
					to setVolume(volume: Int)
					to setVolume(volume: Int, ...idBytes: ...Int)
				}
				Bottle.setVolume(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertEquals("(Int) =>|", functionCall?.targetSignature.toString())
	}

	@Test
	fun `resolves the most specific signature between variadic parameters`() {
		val sourceCode =
			"""
				Int class
				Number class
				Bottle object {
					to setId(...idBytes: ...Number)
					to setId(...idBytes: ...Int)
				}
				Bottle.setId(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertEquals("(...Int) =>|", functionCall?.targetSignature.toString())
	}

	@Test
	fun `resolves the most specific signature considering the number of required conversions`() {
		val sourceCode =
			"""
				Cup class
				Bowl class {
					converting init(cup: Cup)
				}
				Bottle object {
					to setSource(cup: Cup)
					to setSource(bowl: Bowl)
				}
				Bottle.setSource(Cup())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertEquals("(Cup) =>|", functionCall?.targetSignature.toString())
	}

	@Test
	fun `allows accessing functions without where clause`() {
		val sourceCode = """
			abstract Addable class
			IntegerList class {
				to sum(): Addable
			}
			Int class: Addable
			val integerList = IntegerList()
			integerList.sum()
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<WhereClauseUnfulfilled>()
	}

	@Test
	fun `allows accessing functions with where clause when type condition is met`() {
		val sourceCode = """
			ID class
			String class
			Map class {
				containing Key, Value
				to lowercase() where Value is String and Key is ID {}
			}
			val stringMap = <ID, String>Map()
			stringMap.lowercase()
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueNotDetected<WhereClauseUnfulfilled>()
	}

	@Test
	fun `disallows accessing functions with where clause when type condition is not met`() {
		val sourceCode = """
			abstract Addable class
			List class {
				containing Element
				to sum(): Element where Element is specific Addable
			}
			Parachute class
			val parachuteList = <Parachute>List()
			parachuteList.sum()
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueDetected<WhereClauseUnfulfilled>(
			"Function 'sum()' cannot be accessed on object of type '<Parachute>List'," +
				" because the condition 'Element is specific Addable' is not met.", Severity.ERROR)
	}

	@Test
	fun `allows accessing inherited functions with where clause when type condition is met`() {
		val sourceCode = """
			ID class
			String class
			Map class {
				containing Key, Value
				to lowercase() where Value is String and Key is ID {}
			}
			InvertedMap class: <Value, Key>Map {
				containing Key, Value
			}
			val stringMap = <ID, String>Map()
			stringMap.lowercase()
			val invertedStringMap = <String, ID>InvertedMap()
			invertedStringMap.lowercase()
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueNotDetected<WhereClauseUnfulfilled>()
	}

	@Test
	fun `disallows accessing inherited functions with where clause when type condition is not met`() {
		val sourceCode = """
			abstract Addable class
			List class {
				containing Element
				to sum(): Element where Element is specific Addable
			}
			LinkedList class: <Element>List {
				containing Element
			}
			Parachute class
			val parachuteList = <Parachute>LinkedList()
			parachuteList.sum()
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueDetected<WhereClauseUnfulfilled>(
			"Function 'sum()' cannot be accessed on object of type '<Parachute>LinkedList'," +
				" because the condition 'Element is specific Addable' is not met.", Severity.ERROR)
	}

	@Test
	fun `allows accessing inherited functions with statically fulfilled where clause`() {
		val sourceCode = """
			abstract Addable class
			List class {
				containing Element
				to sum(): Element where Element is specific Addable
			}
			Int class: Addable
			IntegerList class: <Int>List
			val integerList = IntegerList()
			integerList.sum()
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<WhereClauseUnfulfilled>()
	}

	@Test
	fun `disallows accessing inherited functions with statically unfulfilled where clause`() {
		val sourceCode = """
			abstract Addable class
			List class {
				containing Element
				to sum(): Element where Element is specific Addable
			}
			Parachute class
			ParachuteList class: <Parachute>List
			val parachuteList = ParachuteList()
			parachuteList.sum()
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueDetected<WhereClauseUnfulfilled>(
			"Function 'sum()' cannot be accessed on object of type 'ParachuteList'," +
				" because the condition 'Element is specific Addable' is not met.", Severity.ERROR)
	}

	@Test
	fun `converts supplied parameters to match signature if possible`() {
		val sourceCode =
			"""
				Int class
				Float class {
					converting init(value: Int)
				}
				Bottle object {
					to setHeight(height: Float)
				}
				Bottle.setHeight(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<SignatureMismatch>()
	}

	@Test
	fun `resolves implementation using conversion for monomorphic functions`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract monomorphic it times(right: Self): Self
				}
				Byte class: Number {
					native overriding monomorphic it times(right: Self): Self
				}
				Int class: Number {
					converting init(value: Byte)
					init
					native overriding monomorphic it times(right: Self): Self
				}
				Int().times(Byte())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotFound>()
		lintResult.assertIssueNotDetected<SignatureMismatch>()
		lintResult.assertIssueNotDetected<AbstractMonomorphicAccess>()
	}
}
