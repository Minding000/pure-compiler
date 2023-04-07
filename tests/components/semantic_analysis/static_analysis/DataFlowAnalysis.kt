package components.semantic_analysis.static_analysis

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class DataFlowAnalysis {

	@Test
	fun `works without variables`() {
		val tracker = TestUtil.analyseDataFlow("")
		assertTrue(tracker.variables.isEmpty())
	}

	@Test
	fun `works with one statement`() {
		val sourceCode = """
			val a = 0
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> end (Int, 0)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `works with multiple statements`() {
		val sourceCode = """
			var b = 0
			b
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 2 (Int, 0)
			2: read -> end (Int, 0)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("b"))
	}

	@Test
	fun `works with multiple variables`() {
		val sourceCode = """
			Int class
			var b = 0
			val a: Int
			b
		""".trimIndent()
		val reportForA = """
			start -> 3
			3: declaration -> end (Int, null)
		""".trimIndent()
		val reportForB = """
			start -> 2
			2: declaration & write -> 4 (Int, 0)
			4: read -> end (Int, 0)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(reportForA, tracker.getReportFor("a"))
		assertEquals(reportForB, tracker.getReportFor("b"))
	}

	@Test
	fun `works with if statements`() {
		val sourceCode = """
			var a = 0
			if yes {
				a
			} else {
				a = 1
			}
			a
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 3, 5 (Int, 0)
			3: read -> 7 (Int, 0)
			5: write -> 7 (Int, 1)
			7: read -> end (Int, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `works with loops without generator`() {
		val sourceCode = """
			var a = 0
			loop {
				a = 1
				if yes
					break
				else if no
					next
				a
			}
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 3 (Int, 0)
			3: write -> 3, 8, end (Int, 1)
			8: read -> 3 (Int, 1)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `works with loops with generator`() {
		val sourceCode = """
			var a = 0
			loop over Range(0, 5) as number {
				a += number
			}
			a
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 3, 5 (Int, 0)
			3: read & mutation -> 3, 5 (Int, null)
			5: read -> end (Int, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `works with returns`() {
		val sourceCode = """
			Int class
			DataProvider object {
				to getNumber(): Int {
					var a = 0
					if yes
						return 0
					return a
				}
			}
		""".trimIndent()
		val report = """
			start -> 4
			4: declaration & write -> 7, end (Int, 0)
			7: read -> end (Int, 0)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.childTrackers["DataProvider.getNumber(): Int"]?.getReportFor("a"))
	}

	@Test
	fun `works with handle blocks`() {
		val sourceCode = """
			var a = 10
			{
				a = 0
				a
			} handle Error {
				a = 1
			} always {
				a
			}
			a = 2
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 3, 6, 8e (Int, 10)
			3: write -> 4, 6, 8e (Int, 0)
			4: read -> 6, 8, 8e (Int, 0)
			6: write -> 8, 8e (Int, 1)
			8: read -> 10 (Int, null)
			8e: read -> continues raise (Int, null)
			10: write -> end (Int, 2)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `works with switch statements with else branches`() {
		val sourceCode = """
			var d = -1
			switch d {
				0: d
				d: d
				else: d
			}
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 2 (Int, -1)
			2: read -> 3, 4 (Int, -1)
			3: hint -> 3 (Int, 0)
			3: read -> end (Int, 0)
			4: read -> 4, 5 (Int, -1)
			4: hint -> 4 (Int, -1)
			4: read -> end (Int, -1)
			5: read -> end (Int, -1)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("d"))
	}

	@Test
	fun `works with switch statements without else branches`() {
		val sourceCode = """
			var d = -1
			switch d {
				0: d
				d: d
			}
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 2 (Int, -1)
			2: read -> 3, 4 (Int, -1)
			3: hint -> 3 (Int, 0)
			3: read -> end (Int, 0)
			4: read -> 4, end (Int, -1)
			4: hint -> 4 (Int, -1)
			4: read -> end (Int, -1)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("d"))
	}

	@Test
	fun `works when last usage is optional`() {
		val sourceCode = """
			var c = 0
			if yes
				c
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 3, end (Int, 0)
			3: read -> end (Int, 0)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("c"))
	}

	@Test
	fun `over generator initializes declared variables`() {
		val sourceCode = """
			var a: ...Int
			loop over a as value
				break
		""".trimIndent()
		val report = """
			start -> 2
			2: declaration & write -> end (Int, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("value"))
	}

	@Test
	fun `handle blocks initialize error variable`() {
		val sourceCode = """
			val a: Int
			{
			} handle e: Error {
			}
		""".trimIndent()
		val report = """
			start -> 3
			3: declaration & write -> end (Error, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("e"))
	}

	@Test
	fun `works with member accesses`() {
		val sourceCode = """
			val a: Int
			a.b = 2
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration -> 2 (Int, null)
			2: read -> end (Int, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `works with member accesses containing self references`() {
		val sourceCode = """
			A class {
				val a: Int
				init {
					this.a
				}
			}
		""".trimIndent()
		val report = """
			start -> 2
			2: declaration -> 4 (Int, null)
			4: read -> end (Int, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.childTrackers["A.init()"]?.getReportFor("a"))
	}

	@Test
	fun `works with member accesses containing self references being written to`() {
		val sourceCode = """
			A class {
				val a: Int
				init {
					this.a = 3
				}
			}
		""".trimIndent()
		val report = """
			start -> 2
			2: declaration -> 4 (Int, null)
			4: write -> end (Int, 3)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.childTrackers["A.init()"]?.getReportFor("a"))
	}

	@Test
	fun `branches on boolean and`() {
		val sourceCode = """
			var a: Bool
			a & a
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration -> 2 (Bool, null)
			2: read -> 2, end (Bool, null)
			2: read -> end (Bool, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	//TODO add warning on expected division by zero

	@Test
	fun `branches on boolean or`() { //TODO add value hint when variable is used in boolean expression
		val sourceCode = """
			var a: Bool
			a | a
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration -> 2 (Bool, null)
			2: read -> 2, end (Bool, null)
			2: read -> end (Bool, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `applies hints in boolean expressions`() { //TODO consider this syntax ambiguity
		val sourceCode = """
			var a: Int?
			(a is Int) | a
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration -> 2 (Int?, null)
			2: read -> 2, 2 (Int?, null)
			2: hint -> end (Int, null)
			2: hint -> 2 (Null, null)
			2: read -> end (Null, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `changes type and value on write`() {
		val sourceCode = """
			var a = 1
			a = 2.1
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 2 (Int, 1)
			2: write -> end (Float, 2.1)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `retains type and changes value on unary modification`() {
		val sourceCode = """
			var a = 1
			a++
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 2 (Int, 1)
			2: read & mutation -> end (Int, 2)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `retains type and changes value on binary modification`() {
		val sourceCode = """
			var a = 1
			a += 2
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 2 (Int, 1)
			2: read & mutation -> end (Int, 3)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `calculates value from unary operators`() {
		val sourceCode = """
			var a = !yes
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> end (Bool, no)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `calculates value from binary operators`() {
		val sourceCode = """
			var a = 1 + 1
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> end (Int, 2)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `retains type and value on ordinary read`() {
		val sourceCode = """
			var a = 1
			a
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 2 (Int, 1)
			2: read -> end (Int, 1)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `changes value on comparison`() {
		val sourceCode = """
			var a = 1
			if a == 2 {
				a
			} else {
				a
			}
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 2 (Int, 1)
			2: read -> 2, 5 (Int, 1)
			2: hint -> 3 (Int, 2)
			3: read -> end (Int, 2)
			5: read -> end (Int, 1)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `changes value on negated comparison`() {
		val sourceCode = """
			var a = 1
			if a != 2 {
				a
			} else {
				a
			}
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 2 (Int, 1)
			2: read -> 2, 3 (Int, 1)
			2: hint -> 5 (Int, 2)
			3: read -> end (Int, 1)
			5: read -> end (Int, 2)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `changes type on null check`() {
		val sourceCode = """
			val a: Int?
			if a? {
				a
			} else {
				a
			}
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration -> 2 (Int?, null)
			2: read -> 2, 2 (Int?, null)
			2: hint -> 3 (Null, null)
			2: hint -> 5 (Int, null)
			3: read -> end (Null, null)
			5: read -> end (Int, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `changes type on type check`() {
		val sourceCode = """
			val a: Int?
			if a is Int {
				a
			} else {
				a
			}
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration -> 2 (Int?, null)
			2: read -> 2, 2 (Int?, null)
			2: hint -> 3 (Int, null)
			2: hint -> 5 (Null, null)
			3: read -> end (Int, null)
			5: read -> end (Null, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `changes type on negated type check`() {
		val sourceCode = """
			val a: Int?
			if a is! Int {
				a
			} else {
				a
			}
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration -> 2 (Int?, null)
			2: read -> 2, 2 (Int?, null)
			2: hint -> 5 (Int, null)
			2: hint -> 3 (Null, null)
			3: read -> end (Null, null)
			5: read -> end (Int, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `applies hints from and expressions`() {
		val sourceCode = """
			val a: Int?
			if (a is Int) & a == 2 {
				a
			} else {
				a
			}
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration -> 2 (Int?, null)
			2: read -> 2, 2 (Int?, null)
			2: hint -> 2 (Int, null)
			2: hint -> 5 (Null, null)
			2: read -> 2, 5 (Int, null)
			2: hint -> 3 (Int, 2)
			3: read -> end (Int, 2)
			5: read -> end (Int?, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `applies hints from or expressions`() {
		val sourceCode = """
			val a: Int?
			if (a is Int) | a == 2 {
				a
			} else {
				a
			}
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration -> 2 (Int?, null)
			2: read -> 2, 2 (Int?, null)
			2: hint -> 3 (Int, null)
			2: hint -> 2 (Null, null)
			2: read -> 2, 5 (Null, null)
			2: hint -> 3 (Int, 2)
			3: read -> end (Int, null)
			5: read -> end (Null, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `applies hints from not expressions`() {
		val sourceCode = """
			val a: Int?
			if !(a is Int) {
				a
			} else {
				a
			}
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration -> 2 (Int?, null)
			2: read -> 2, 2 (Int?, null)
			2: hint -> 5 (Int, null)
			2: hint -> 3 (Null, null)
			3: read -> end (Null, null)
			5: read -> end (Int, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReportFor("a"))
	}
}
