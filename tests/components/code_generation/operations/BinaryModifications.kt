package components.code_generation.operations

import org.junit.jupiter.api.Test
import util.TestApp
import util.TestUtil
import kotlin.test.assertEquals

internal class BinaryModifications {

	//TODO implement these tests
	// - also test other operators on bytes
	//@Test
	//fun `compiles byte addition assignments`() {
	//	val sourceCode = """
	//		SimplestApp object {
	//			to getFive(): Byte {
	//				var a: Byte = 3
	//				a += 2
	//				return a
	//			}
	//		}
	//		""".trimIndent()
	//	val result = TestUtil.runAndReturnByte(sourceCode, "Test:SimplestApp.getFive")
	//	assertEquals(5, result)
	//}
	//
	//@Test
	//fun `compiles byte subtraction assignments`() {
	//	val sourceCode = """
	//		SimplestApp object {
	//			to getFive(): Byte {
	//				var a: Byte = 8
	//				a -= 3
	//				return a
	//			}
	//		}
	//		""".trimIndent()
	//	val result = TestUtil.runAndReturnByte(sourceCode, "Test:SimplestApp.getFive")
	//	assertEquals(5, result)
	//}
	//
	//@Test
	//fun `compiles byte multiplication assignments`() {
	//	val sourceCode = """
	//		SimplestApp object {
	//			to getFive(): Byte {
	//				var a: Byte = 1
	//				a *= 5
	//				return a
	//			}
	//		}
	//		""".trimIndent()
	//	val result = TestUtil.runAndReturnByte(sourceCode, "Test:SimplestApp.getFive")
	//	assertEquals(5, result)
	//}
	//
	//@Test
	//fun `compiles byte division assignments`() {
	//	val sourceCode = """
	//		SimplestApp object {
	//			to getFive(): Byte {
	//				var a: Byte = 20
	//				a /= 4
	//				return a
	//			}
	//		}
	//		""".trimIndent()
	//	val result = TestUtil.runAndReturnByte(sourceCode, "Test:SimplestApp.getFive")
	//	assertEquals(5, result)
	//}

	@Test
	fun `compiles integer addition assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					var a = 3
					a += 2
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `throws on overflowing byte addition`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to run() {
					var a: Byte = 127
					a += 1
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Addition overflowed
			 at Test:Test:5:SimplestApp.run()
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `throws on overflowing integer addition`() {
		val sourceCode = """
			SimplestApp object {
				to run() {
					var a = 2147483647
					a += 1
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Addition overflowed
			 at Test:Test:4:SimplestApp.run()
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `compiles integer subtraction assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					var a = 8
					a -= 3
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `throws on overflowing byte subtraction`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to run() {
					var a: Byte = -128
					a -= 1
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Subtraction overflowed
			 at Test:Test:5:SimplestApp.run()
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `throws on overflowing integer subtraction`() {
		val sourceCode = """
			SimplestApp object {
				to run() {
					var a = -2147483648
					a -= 1
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Subtraction overflowed
			 at Test:Test:4:SimplestApp.run()
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `compiles integer multiplication assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					var a = 1
					a *= 5
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `throws on overflowing byte multiplication`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to run() {
					var a: Byte = 120
					a *= 2
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Multiplication overflowed
			 at Test:Test:5:SimplestApp.run()
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `throws on overflowing integer multiplication`() {
		val sourceCode = """
			SimplestApp object {
				to run() {
					var a = 2100200300
					a *= 2
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Multiplication overflowed
			 at Test:Test:4:SimplestApp.run()
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `compiles integer division assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					var a = 20
					a /= 4
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `throws on division by zero`() {
		val sourceCode = """
			SimplestApp object {
				to run() {
					var a = 20
					a /= 0
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Division by zero
			 at Test:Test:4:SimplestApp.run()
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `throws on overflowing division`() {
		val sourceCode = """
			SimplestApp object {
				to run() {
					var a = -2147483648
					a /= -1
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Division overflowed
			 at Test:Test:4:SimplestApp.run()
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `compiles float addition assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					var a = 2.3
					a += 2.7
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnFloat(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, result)
	}

	@Test
	fun `compiles float subtraction assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					var a = 9.5
					a -= 4.5
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnFloat(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, result)
	}

	@Test
	fun `compiles float multiplication assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					var a = 2.5
					a *= 2.0
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnFloat(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, result)
	}

	@Test
	fun `compiles float division assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					var a = 7.5
					a /= 1.5
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnFloat(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, result)
	}

	@Test
	fun `compiles addition assignments with float target and integer modifier`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					var a = 2.0
					a += 3
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnFloat(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, result)
	}

	@Test
	fun `compiles subtraction assignments with float target and integer modifier`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					var a = 7.0
					a -= 2
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnFloat(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, result)
	}

	@Test
	fun `compiles multiplication assignments with float target and integer modifier`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					var a = 2.5
					a *= 2
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnFloat(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, result)
	}

	@Test
	fun `compiles division assignments with float target and integer modifier`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					var a = 10.0
					a /= 2
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnFloat(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, result)
	}

	@Test
	fun `compiles primitive binary modifications on member accesses`() {
		val sourceCode = """
			City object {
				var residentCount = 0
			}
			SimplestApp object {
				to getOneHundred(): Int {
					City.residentCount += 100
					return City.residentCount
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOneHundred")
		assertEquals(100, result)
	}

	@Test
	fun `compiles custom operator calls`() {
		val sourceCode = """
			Pool class {
				var subPoolCount = 0
				operator +=(other: Pool) {
					subPoolCount++
				}
			}
			SimplestApp object {
				to getOne(): Int {
					val pool = Pool()
					pool += Pool()
					return pool.subPoolCount
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne")
		assertEquals(1, result)
	}

	@Test
	fun `compiles modification on primitive generic properties`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
			}
			SimplestApp object {
				to getEleven(): Int {
					val container = Container(8)
					container.a += 3
					return container.a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEleven")
		assertEquals(11, result)
	}

	@Test
	fun `compiles modification with primitive generic properties`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
			}
			SimplestApp object {
				to getFive(): Int {
					var b = 8
					val container = Container(3)
					b -= container.a
					return b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `converts values`() {
		val sourceCode = """
			A class {
				val b: Int
				converting init(b)
				operator +=(other: A) {
					b += other.b
				}
			}
			SimplestApp object {
				to getSeven(): Int {
					val a = A(5)
					a += 2
					return a.b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSeven")
		assertEquals(7, result)
	}
}
