package components.semantic_model.resolution

import components.semantic_model.types.ObjectType
import components.semantic_model.values.BooleanLiteral
import components.semantic_model.values.NullLiteral
import components.semantic_model.values.NumberLiteral
import components.semantic_model.values.StringLiteral
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertIs
import kotlin.test.assertNotNull

internal class LiteralResolution {

	@Test
	fun `loads string literal type`() {
		val sourceCode = """ "" """
		val lintResult = TestUtil.lint(sourceCode, true)
		val stringLiteralType = lintResult.find<StringLiteral>()?.providedType
		assertIs<ObjectType>(stringLiteralType)
		assertNotNull(stringLiteralType.getTypeDeclaration())
	}

	@Test
	fun `loads number literal type`() {
		val sourceCode = "0"
		val lintResult = TestUtil.lint(sourceCode, true)
		val numberLiteralType = lintResult.find<NumberLiteral>()?.providedType
		assertIs<ObjectType>(numberLiteralType)
		assertNotNull(numberLiteralType.getTypeDeclaration())
	}

	@Test
	fun `loads boolean literal type`() {
		val sourceCode = "yes"
		val lintResult = TestUtil.lint(sourceCode, true)
		val booleanLiteralType = lintResult.find<BooleanLiteral>()?.providedType
		assertIs<ObjectType>(booleanLiteralType)
		assertNotNull(booleanLiteralType.getTypeDeclaration())
	}

	@Test
	fun `loads null literal type`() {
		val sourceCode = "null"
		val lintResult = TestUtil.lint(sourceCode, true)
		val nullLiteralType = lintResult.find<NullLiteral>()?.providedType
		assertIs<ObjectType>(nullLiteralType)
		assertNotNull(nullLiteralType.getTypeDeclaration())
	}
}
