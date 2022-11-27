package components.tokenizer

import org.junit.jupiter.api.Test
import util.TestUtil

internal class Comments {

	@Test
	fun `ignores single line comments`() {
		TestUtil.assertTokenIsIgnored("//")
		TestUtil.assertTokenIsIgnored("// // ...")
		TestUtil.assertTokenIsIgnored("// This is a single line comment.")
	}

	@Test
	fun `ignores multi line comments`() {
		TestUtil.assertTokenIsIgnored("/**/")
		TestUtil.assertTokenIsIgnored("/* /* */")
		TestUtil.assertTokenIsIgnored("/* // */")
		TestUtil.assertTokenIsIgnored("/* * / */")
		TestUtil.assertTokenIsIgnored("/* \n */")
		TestUtil.assertTokenIsIgnored("/* This is a multi line comment. */")
	}
}
