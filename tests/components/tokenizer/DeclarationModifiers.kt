package components.tokenizer

import org.junit.jupiter.api.Test
import util.TestUtil

internal class DeclarationModifiers {

	@Test
	fun `detects 'abstract' modifier tokens`() {
		TestUtil.assertTokenType("abstract", WordAtom.ABSTRACT)
	}

	@Test
	fun `detects 'bound' modifier tokens`() {
		TestUtil.assertTokenType("bound", WordAtom.BOUND)
	}

	@Test
	fun `detects 'copied' modifier tokens`() {
		TestUtil.assertTokenType("copied", WordAtom.COPIED)
	}

	@Test
	fun `detects 'converting' modifier tokens`() {
		TestUtil.assertTokenType("converting", WordAtom.CONVERTING)
	}

	@Test
	fun `detects 'immutable' modifier tokens`() {
		TestUtil.assertTokenType("immutable", WordAtom.IMMUTABLE)
	}

	@Test
	fun `detects 'mutable' modifier tokens`() {
		TestUtil.assertTokenType("mutable", WordAtom.MUTABLE)
	}

	@Test
	fun `detects 'mutating' modifier tokens`() {
		TestUtil.assertTokenType("mutating", WordAtom.MUTATING)
	}

	@Test
	fun `detects 'native' modifier tokens`() {
		TestUtil.assertTokenType("native", WordAtom.NATIVE)
	}

	@Test
	fun `detects 'overriding' modifier tokens`() {
		TestUtil.assertTokenType("overriding", WordAtom.OVERRIDING)
	}

	@Test
	fun `detects 'specific' modifier tokens`() {
		TestUtil.assertTokenType("specific", WordAtom.SPECIFIC)
	}
}
