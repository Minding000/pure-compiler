package components.semantic_model.modifiers

import logger.Severity
import logger.issues.access.AbstractMonomorphicAccess
import logger.issues.declaration.MonomorphicInheritance
import logger.issues.modifiers.DisallowedModifier
import logger.issues.modifiers.ExtraneousMonomorphicModifier
import logger.issues.modifiers.MissingMonomorphicKeyword
import org.junit.jupiter.api.Test
import util.TestUtil

internal class MonomorphicModifier {

	@Test
	fun `is not allowed on classes`() {
		val sourceCode = "monomorphic Goldfish class"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on objects`() {
		val sourceCode = "monomorphic Earth object"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on enums`() {
		val sourceCode = "monomorphic Tire enum"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on properties`() {
		val sourceCode =
			"""
				Goldfish class {
					monomorphic val brain: Brain
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on computed properties`() {
		val sourceCode =
			"""
				Goldfish class {
					monomorphic computed name: String
						gets "Bernd"
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on initializers`() {
		val sourceCode =
			"""
				Dictionary class {
					monomorphic init()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is allowed on functions`() {
		val sourceCode =
			"""
				Goldfish class {
					monomorphic to swim()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `is allowed on operators`() {
		val sourceCode =
			"""
				Goldfish class {
					monomorphic operator ++
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `doesn't require function without a self type parameter to be marked as monomorphic`() {
		val sourceCode =
			"""
				Int class {
					native to add(other: Int): Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingMonomorphicKeyword>()
	}

	@Test
	fun `detects function taking self type parameters without being marked as monomorphic`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract to add(other: Self): Self
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingMonomorphicKeyword>(
			"Function 'Number.add(Self): Self' is missing the 'monomorphic' keyword.", Severity.ERROR)
	}

	@Test
	fun `allows monomorphic function to take self type parameters`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract monomorphic to add(other: Self): Self
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingMonomorphicKeyword>()
	}

	@Test
	fun `detects monomorphic function not taking self type parameters`() {
		val sourceCode =
			"""
				Int class {
					monomorphic to add(other: Int): Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ExtraneousMonomorphicModifier>(
			"'monomorphic' keyword is used, but the function doesn't used its own type as Self.", Severity.WARNING)
	}

	@Test
	fun `doesn't require binary operator without a self type parameter to be marked as monomorphic`() {
		val sourceCode =
			"""
				Int class {
					native operator +(other: Int): Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingMonomorphicKeyword>()
	}

	@Test
	fun `detects binary operator taking self type parameters without being marked as monomorphic`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract operator +(other: Self): Self
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingMonomorphicKeyword>(
			"Operator 'Number + Self: Self' is missing the 'monomorphic' keyword.", Severity.ERROR)
	}

	@Test
	fun `allows monomorphic binary operator to take self type parameters`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract monomorphic operator +(other: Self): Self
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingMonomorphicKeyword>()
	}

	@Test
	fun `detects monomorphic binary operator not taking self type parameters`() {
		val sourceCode =
			"""
				Int class {
					monomorphic operator +(other: Int): Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ExtraneousMonomorphicModifier>(
			"'monomorphic' keyword is used, but the operator doesn't used its own type as Self.", Severity.WARNING)
	}

	@Test
	fun `doesn't require binary modification without a self type parameter to be marked as monomorphic`() {
		val sourceCode =
			"""
				Int class {
					native operator +=(other: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingMonomorphicKeyword>()
	}

	@Test
	fun `detects binary modification taking self type parameters without being marked as monomorphic`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract operator +=(other: Self)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingMonomorphicKeyword>(
			"Operator 'Number += Self' is missing the 'monomorphic' keyword.", Severity.ERROR)
	}

	@Test
	fun `allows monomorphic binary modification to take self type parameters`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract monomorphic operator +=(other: Self)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingMonomorphicKeyword>()
	}

	@Test
	fun `detects monomorphic binary modification not taking self type parameters`() {
		val sourceCode =
			"""
				Int class {
					monomorphic operator +=(other: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ExtraneousMonomorphicModifier>(
			"'monomorphic' keyword is used, but the operator doesn't used its own type as Self.", Severity.WARNING)
	}

	@Test
	fun `detects monomorphic unary operator`() {
		val sourceCode =
			"""
				Int class {
					monomorphic operator -: Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ExtraneousMonomorphicModifier>(
			"'monomorphic' keyword is used, but the operator doesn't used its own type as Self.", Severity.WARNING)
	}

	@Test
	fun `detects monomorphic unary modification`() {
		val sourceCode =
			"""
				Int class {
					monomorphic operator ++
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ExtraneousMonomorphicModifier>(
			"'monomorphic' keyword is used, but the operator doesn't used its own type as Self.", Severity.WARNING)
	}

	@Test
	fun `doesn't require index operator without a self type parameter to be marked as monomorphic`() {
		val sourceCode =
			"""
				abstract IntNode class {
					abstract operator [index: Int](value: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingMonomorphicKeyword>()
	}

	@Test
	fun `detects index operator taking self type parameters without being marked as monomorphic`() {
		val sourceCode =
			"""
				abstract NumberNode class {
					abstract operator [index: Int](value: Self)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingMonomorphicKeyword>(
			"Operator 'NumberNode[Int](Self)' is missing the 'monomorphic' keyword.", Severity.ERROR)
	}

	@Test
	fun `allows monomorphic index operator to take self type parameters`() {
		val sourceCode =
			"""
				abstract NumberNode class {
					abstract monomorphic operator [index: Int](value: Self)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingMonomorphicKeyword>()
	}

	@Test
	fun `detects monomorphic index operator not taking self type parameters`() {
		val sourceCode =
			"""
				IntNode class {
					monomorphic operator [index: Int](value: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ExtraneousMonomorphicModifier>(
			"'monomorphic' keyword is used, but the operator doesn't used its own type as Self.", Severity.WARNING)
	}

	@Test
	fun `disallows inheriting from non-abstract classes with monomorphic members`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract monomorphic operator +(other: Self): Self
				}
				Int class: Number {
					native to add(other: Int): Int
					overriding monomorphic operator +(other: Self): Self {
						return add(other)
					}
				}
				SafeInt class: Int
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MonomorphicInheritance>("Class 'Int' cannot be inherited from.", Severity.ERROR)
	}

	@Test
	fun `allows accessing non-monomorphic members on non-specific object type`() {
		val sourceCode =
			"""
				Counter object {
					to increment()
				}
				Counter.increment()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<AbstractMonomorphicAccess>()
	}

	@Test
	fun `allows accessing monomorphic binary operator on non-abstract object type`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract monomorphic operator +(other: Self): Self
				}
				Int class: Number {
					overriding monomorphic operator +(other: Self): Self
				}
				Int() + Int()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<AbstractMonomorphicAccess>()
	}

	@Test
	fun `disallows accessing monomorphic binary operator on non-specific abstract object type`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract monomorphic operator +(other: Self): Self
				}
				val a: Number
				val b: Number
				a + b
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<AbstractMonomorphicAccess>(
			"Monomorphic operator ' + Self: Self' accessed through abstract type 'Number'.", Severity.ERROR)
	}

	@Test
	fun `allows accessing monomorphic binary operator on specific object type`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract monomorphic operator +(other: Self): Self
				}
				val a: specific Number
				val b: specific Number
				a + b
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<AbstractMonomorphicAccess>()
	}

	@Test
	fun `allows accessing monomorphic binary modification on non-abstract object type`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract monomorphic operator +=(other: Self)
				}
				Int class: Number {
					overriding monomorphic operator +=(other: Self)
				}
				var a: Int
				a += Int()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<AbstractMonomorphicAccess>()
	}

	@Test
	fun `disallows accessing monomorphic binary modification on non-specific abstract object type`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract monomorphic operator +=(other: Self)
				}
				val a: Number
				val b: Number
				a += b
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<AbstractMonomorphicAccess>(
			"Monomorphic operator ' += Self' accessed through abstract type 'Number'.", Severity.ERROR)
	}

	@Test
	fun `allows accessing monomorphic binary modification on specific object type`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract monomorphic operator +=(other: Self)
				}
				val a: specific Number
				val b: specific Number
				a += b
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<AbstractMonomorphicAccess>()
	}

	@Test
	fun `allows accessing monomorphic index operator on non-abstract object type`() {
		val sourceCode =
			"""
				abstract NumberNode class {
					abstract monomorphic operator [index: Int](value: Self)
				}
				IntNode class: NumberNode {
					overriding monomorphic operator [index: Int](value: Self)
				}
				IntNode()[0] = IntNode()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<AbstractMonomorphicAccess>()
	}

	@Test
	fun `disallows accessing monomorphic index operator on non-specific abstract object type`() {
		val sourceCode =
			"""
				abstract NumberNode class {
					abstract monomorphic operator [index: Int](value: Self)
				}
				val a: NumberNode
				val b: NumberNode
				a[0] = b
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<AbstractMonomorphicAccess>(
			"Monomorphic operator '[Int](Self)' accessed through abstract type 'NumberNode'.", Severity.ERROR)
	}

	@Test
	fun `allows accessing monomorphic index operator on specific object type`() {
		val sourceCode =
			"""
				abstract NumberNode class {
					abstract monomorphic operator [index: Int](value: Self)
				}
				val a: specific NumberNode
				val b: specific NumberNode
				a[0] = b
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<AbstractMonomorphicAccess>()
	}

	@Test
	fun `allows accessing monomorphic function on non-abstract object type`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract monomorphic to plus(other: Self): Self
				}
				Int class: Number {
					overriding monomorphic to plus(other: Self): Self
				}
				Int().plus(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<AbstractMonomorphicAccess>()
	}

	@Test
	fun `disallows accessing monomorphic function on non-specific abstract object type`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract monomorphic to plus(other: Self): Self
				}
				val a: Number
				val b: Number
				a.plus(b)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<AbstractMonomorphicAccess>(
			"Monomorphic function '(Self): Self' accessed through abstract type 'Number'.", Severity.ERROR)
	}

	@Test
	fun `allows accessing monomorphic function on specific object type`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract monomorphic to plus(other: Self): Self
				}
				val a: specific Number
				val b: specific Number
				a.plus(b)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<AbstractMonomorphicAccess>()
	}
}
