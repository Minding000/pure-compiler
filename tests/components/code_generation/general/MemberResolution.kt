package components.code_generation.general

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class MemberResolution {

	@Test
	fun `resolves members`() {
		val sourceCode = """
			SimplestApp object {
				val a = 62
				to getA(): Int {
					return SimplestApp.a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(62, result)
	}

	@Test
	fun `resolves overridden members`() {
		val sourceCode = """
			Parent class {
				val a = 63
				to getThing(): Parent {
					return this
				}
			}
			SimplestApp object: Parent {
				overriding to getThing(): SimplestApp {
					return this
				}
				to getA(): Int {
					return SimplestApp.getThing().a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(63, result)
	}

	@Test
	fun `resolves implemented abstract members`() {
		val sourceCode = """
			abstract Parent class {
				val a = 64
				abstract to getThing(): Parent
			}
			SimplestApp object: Parent {
				overriding to getThing(): SimplestApp {
					return this
				}
				to getA(): Int {
					return SimplestApp.getThing().a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(64, result)
	}

	@Test
	fun `resolves implemented generic members`() {
		val sourceCode = """
			Some class {
				val a = 64
			}
			Something object: Some
			abstract Parent class {
				containing T
				abstract to getThing(): T
			}
			SimplestApp object: <Some>Parent {
				overriding to getThing(): Something {
					return Something
				}
				to getA(): Int {
					return SimplestApp.getThing().a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(64, result)
	}

	//TODO test resolution when calling member with multiple super members

	@Test
	fun `resolves member on and-union`() {
		val sourceCode = """
			Playable class {
				val duration = 55
			}
			Stoppable class
			SoundTrack class: Playable & Stoppable
			SimplestApp object {
				to getFiftyFive(): Int {
					val container: Playable & Stoppable = SoundTrack()
					return container.duration
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFiftyFive")
		assertEquals(55, result)
	}

	@Test
	fun `resolves member on or-union`() {
		val sourceCode = """
			Bottle class {
				val volume = 9
			}
			Bucket class {
				val volume = 83
			}
			SimplestApp object {
				to getEightyThree(): Int {
					val container: Bottle | Bucket = Bucket()
					return container.volume
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightyThree")
		assertEquals(83, result)
	}
}
