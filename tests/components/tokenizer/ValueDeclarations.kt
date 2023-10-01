package components.tokenizer

import org.junit.jupiter.api.Test
import util.TestUtil

internal class ValueDeclarations {

	@Test
	fun `detects instance value tokens`() {
		TestUtil.assertTokenType("instances", WordAtom.INSTANCES)
	}

	@Test
	fun `detects constant value tokens`() {
		TestUtil.assertTokenType("const", WordAtom.CONST)
	}

	@Test
	fun `detects readonly value tokens`() {
		TestUtil.assertTokenType("val", WordAtom.VAL)
	}

	@Test
	fun `detects variable value tokens`() {
		TestUtil.assertTokenType("var", WordAtom.VAR)
	}

	@Test
	fun `detects computed property tokens`() {
		TestUtil.assertTokenType("computed", WordAtom.COMPUTED)
	}

	@Test
	fun `detects getter tokens`() {
		TestUtil.assertTokenType("gets", WordAtom.GETS)
	}

	@Test
	fun `detects setter tokens`() {
		TestUtil.assertTokenType("sets", WordAtom.SETS)
	}
}
