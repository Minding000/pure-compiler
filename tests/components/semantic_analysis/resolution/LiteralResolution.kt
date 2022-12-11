package components.semantic_analysis.resolution

import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.semantic_analysis.semantic_model.values.NullLiteral
import components.semantic_analysis.semantic_model.values.NumberLiteral
import components.semantic_analysis.semantic_model.values.StringLiteral
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertIs
import kotlin.test.assertNotNull

internal class LiteralResolution {

	@Test
	fun `loads string literal type`() {
		val sourceCode = """ "" """
		val lintResult = TestUtil.lint(sourceCode, true)
		val stringLiteralType = lintResult.find<StringLiteral>()?.type
		assertIs<ObjectType>(stringLiteralType)
		assertNotNull(stringLiteralType.definition)
	}

	@Test
	fun `loads number literal type`() {
		val sourceCode = "0"
		val lintResult = TestUtil.lint(sourceCode, true)
		val numberLiteralType = lintResult.find<NumberLiteral>()?.type
		assertIs<ObjectType>(numberLiteralType)
		assertNotNull(numberLiteralType.definition)
	}

	@Test
	fun `loads boolean literal type`() {
		val sourceCode = "yes"
		val lintResult = TestUtil.lint(sourceCode, true)
		val booleanLiteralType = lintResult.find<BooleanLiteral>()?.type
		assertIs<ObjectType>(booleanLiteralType)
		assertNotNull(booleanLiteralType.definition)
	}

	@Test
	fun `loads null literal type`() {
		val sourceCode = "null"
		val lintResult = TestUtil.lint(sourceCode, true)
		val nullLiteralType = lintResult.find<NullLiteral>()?.type
		assertIs<ObjectType>(nullLiteralType)
		assertNotNull(nullLiteralType.definition)
	}
}
