package components.tokenizer

import org.junit.jupiter.api.Test
import util.TestUtil

internal class Operators {

	@Test
	fun `detects arrow tokens`() {
		TestUtil.assertTokenType("=>", WordAtom.ARROW)
	}

	@Test
	fun `detects capped arrow tokens`() {
		TestUtil.assertTokenType("=>|", WordAtom.CAPPED_ARROW)
	}

	@Test
	fun `detects increment tokens`() {
		TestUtil.assertTokenType("++", WordAtom.INCREMENT)
	}

	@Test
	fun `detects decrement tokens`() {
		TestUtil.assertTokenType("--", WordAtom.DECREMENT)
	}

	@Test
	fun `detects and tokens`() {
		TestUtil.assertTokenType("and", WordAtom.AND_OPERATOR)
	}

	@Test
	fun `detects or tokens`() {
		TestUtil.assertTokenType("or", WordAtom.OR_OPERATOR)
	}

	@Test
	fun `detects plus tokens`() {
		TestUtil.assertTokenType("+", WordAtom.PLUS)
	}

	@Test
	fun `detects minus tokens`() {
		TestUtil.assertTokenType("-", WordAtom.MINUS)
	}

	@Test
	fun `detects star tokens`() {
		TestUtil.assertTokenType("*", WordAtom.STAR)
	}

	@Test
	fun `detects slash tokens`() {
		TestUtil.assertTokenType("/", WordAtom.SLASH)
	}

	@Test
	fun `detects add tokens`() {
		TestUtil.assertTokenType("+=", WordAtom.ADD)
	}

	@Test
	fun `detects subtract tokens`() {
		TestUtil.assertTokenType("-=", WordAtom.SUBTRACT)
	}

	@Test
	fun `detects multiply tokens`() {
		TestUtil.assertTokenType("*=", WordAtom.MULTIPLY)
	}

	@Test
	fun `detects divide tokens`() {
		TestUtil.assertTokenType("/=", WordAtom.DIVIDE)
	}

	@Test
	fun `detects identical tokens`() {
		TestUtil.assertTokenType("===", WordAtom.IDENTICAL)
	}

	@Test
	fun `detects negated identical tokens`() {
		TestUtil.assertTokenType("!==", WordAtom.NOT_IDENTICAL)
	}

	@Test
	fun `detects equals tokens`() {
		TestUtil.assertTokenType("==", WordAtom.EQUALS)
	}

	@Test
	fun `detects negated equals tokens`() {
		TestUtil.assertTokenType("!=", WordAtom.NOT_EQUALS)
	}

	@Test
	fun `detects greater than tokens`() {
		TestUtil.assertTokenType(">", WordAtom.GREATER_THAN)
	}

	@Test
	fun `detects lower than tokens`() {
		TestUtil.assertTokenType("<", WordAtom.LOWER_THAN)
	}

	@Test
	fun `detects greater or equals than tokens`() {
		TestUtil.assertTokenType(">=", WordAtom.GREATER_OR_EQUALS_THAN)
	}

	@Test
	fun `detects lower or equals than tokens`() {
		TestUtil.assertTokenType("<=", WordAtom.LOWER_OR_EQUALS_THAN)
	}

	@Test
	fun `detects negation tokens`() {
		TestUtil.assertTokenType("!", WordAtom.NOT)
	}

	@Test
	fun `detects null coalescence tokens`() {
		TestUtil.assertTokenType("??", WordAtom.NULL_COALESCENCE)
	}

	@Test
	fun `detects optional accessor tokens`() {
		TestUtil.assertTokenType("?.", WordAtom.OPTIONAL_ACCESSOR)
	}

	@Test
	fun `detects foreign expression tokens`() {
		TestUtil.assertTokenType("::", WordAtom.FOREIGN_EXPRESSION)
	}
}
