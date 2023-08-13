package components.semantic_analysis.declarations

import components.semantic_analysis.semantic_model.declarations.FunctionImplementation
import logger.Severity
import logger.issues.declaration.InvalidVariadicParameterPosition
import logger.issues.declaration.MultipleVariadicParameters
import logger.issues.declaration.Redeclaration
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class Functions {

	@Test
	fun `allows function declarations`() {
		val sourceCode =
			"""
				Int class
				Human class {
					to push()
					to push(pressure: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<Redeclaration>()
	}

	@Test
	fun `detects function redeclarations`() {
		val sourceCode =
			"""
				Pressure class
				alias P = Pressure
				Human class {
					to push(): Pressure
					to push(pressure: P)
					to push(pressure: Pressure)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<Redeclaration>(
			"Redeclaration of function 'Human.push(Pressure)', previously declared in Test.Test:5:4.", Severity.ERROR)
	}

	@Test
	fun `allows single variadic parameter`() {
		val sourceCode =
			"""
				Window class
				House object {
					to add(...windows: ...Window)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MultipleVariadicParameters>()
		lintResult.assertIssueNotDetected<InvalidVariadicParameterPosition>()
	}

	@Test
	fun `detects multiple variadic parameters`() {
		val sourceCode =
			"""
				Window class
				House object {
					to add(...openWindows: ...Window, ...closedWindows: ...Window)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MultipleVariadicParameters>("Signatures can have at most one variadic parameter.",
			Severity.ERROR)
	}

	@Test
	fun `detects variadic parameters not positioned at the parameter list end`() {
		val sourceCode =
			"""
				Window class
				House object {
					to add(...windows: ...Window, selectedWindow: Window)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<InvalidVariadicParameterPosition>("Variadic parameters have to be the last parameter.",
			Severity.ERROR)
	}

	@Test
	fun `links function without parameters to identically named super function`() {
		val sourceCode =
			"""
				House class {
					to build()
				}
				WoodenHouse class: House {
					overriding to build()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNotNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `links function without parameters to identically named super function in second order parent`() {
		val sourceCode =
			"""
				House class {
					to build()
				}
				NaturalHouse class: House
				WoodenHouse class: NaturalHouse {
					overriding to build()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNotNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `doesn't link function without parameters to differently named super function`() {
		val sourceCode =
			"""
				House class {
					to build()
				}
				WoodenHouse class: House {
					overriding to demolish()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `links function to super function with identically typed parameter`() {
		val sourceCode =
			"""
				Int class
				House class {
					to build(a: Int)
				}
				WoodenHouse class: House {
					overriding to build(b: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNotNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `doesn't link function to super function with differently typed parameter`() {
		val sourceCode =
			"""
				Int class
				Float class
				House class {
					to build(a: Int)
				}
				WoodenHouse class: House {
					overriding to build(a: Float)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `links function to super function with super-type parameter`() {
		val sourceCode =
			"""
				Number class
				Int class: Number
				House class {
					to build(a: Int)
				}
				WoodenHouse class: House {
					overriding to build(a: Number)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNotNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `doesn't link function to super function with sub-type parameter`() {
		val sourceCode =
			"""
				Number class
				Int class: Number
				House class {
					to build(a: Number)
				}
				WoodenHouse class: House {
					overriding to build(a: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `links function to super function with identically typed variadic parameter`() {
		val sourceCode =
			"""
				Int class
				House class {
					to build(...a: ...Int)
				}
				WoodenHouse class: House {
					overriding to build(...a: ...Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNotNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `doesn't link function to super function with differently typed variadic parameter`() {
		val sourceCode =
			"""
				Int class
				Float class
				House class {
					to build(...a: ...Int)
				}
				WoodenHouse class: House {
					overriding to build(...a: ...Float)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `doesn't link variadic function to non-variadic super function`() {
		val sourceCode =
			"""
				Int class
				House class {
					to build()
				}
				WoodenHouse class: House {
					overriding to build(...a: ...Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNull(function.signature.superFunctionSignature)
	}
}
