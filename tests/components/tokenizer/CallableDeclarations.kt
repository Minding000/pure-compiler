package components.tokenizer

import org.junit.jupiter.api.Test
import util.TestUtil

internal class CallableDeclarations {

	@Test
	fun `detects 'initializer' tokens`() {
		TestUtil.assertTokenType("init", WordAtom.INITIALIZER)
	}

	@Test
	fun `detects 'deinitializer' tokens`() {
		TestUtil.assertTokenType("deinit", WordAtom.DEINITIALIZER)
	}

	@Test
	fun `detects 'function' tokens`() {
		TestUtil.assertTokenType("it", WordAtom.IT)
		TestUtil.assertTokenType("to", WordAtom.TO)
	}

	@Test
	fun `detects 'operator' tokens`() {
		TestUtil.assertTokenType("operator", WordAtom.OPERATOR)
	}

	@Test
	fun `detects 'generator' tokens`() {
		TestUtil.assertTokenType("generate", WordAtom.GENERATOR)
	}

	@Test
	fun `detects 'where' tokens`() {
		TestUtil.assertTokenType("where", WordAtom.WHERE)
	}
}
