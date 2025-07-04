package components.semantic_model.static_analysis

import org.junit.jupiter.api.Test
import util.TestUtil
import util.TestUtil.assertStringEquals
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
		assertStringEquals(report, tracker.getReportFor("a"))
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
		assertStringEquals(report, tracker.getReportFor("b"))
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
		assertStringEquals(reportForA, tracker.getReportFor("a"))
		assertStringEquals(reportForB, tracker.getReportFor("b"))
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
		assertStringEquals(report, tracker.getReportFor("a"))
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
			1: declaration & write -> 2 (Int, 0)
			2: hint -> 3 (Int, null)
			3: write -> 3, 8, end (Int, 1)
			8: read -> 3 (Int, 1)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `works with loops with over generator`() {
		val sourceCode = """
			var a = 0
			loop over Range(0, 5) as number {
				a += number
			}
			a
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 2 (Int, 0)
			2: hint -> 3, 5 (Int, null)
			3: read & mutation -> 3, 5 (Int, null)
			5: read -> end (Int, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `works with loops with pre while generator`() {
		val sourceCode = """
			Int class
			var result: Int? = Int()
			loop while result? {
				result
				result = null
			}
			result
		""".trimIndent()
		val report = """
			start -> 2
			2: declaration & write -> 3 (Int, Expression)
			3: hint -> 3 (Int?, null)
			3: read -> 3, 3 (Int?, null)
			3: hint -> 4 (Int, null)
			3: hint -> 7 (Null, null)
			4: read -> 5 (Int, null)
			5: write -> 3 (Null, null)
			7: read -> end (Null, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("result"))
	}

	@Test
	fun `works with loops with post while generator`() {
		val sourceCode = """
			Int class
			var result: Int? = Int()
			loop {
				result
				result = null
			} while result?
			result
		""".trimIndent()
		val report = """
			start -> 2
			2: declaration & write -> 3 (Int, Expression)
			3: hint -> 4 (Int?, null)
			4: read -> 5 (Int?, null)
			5: write -> 6 (Null, null)
			6: read -> 6, 6 (Null, null)
			6: hint -> 4 (Int, null)
			6: hint -> 7 (Null, null)
			7: read -> end (Null, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("result"))
	}

	@Test
	fun `works with loops with pre until generator`() {
		val sourceCode = """
			Int class
			var result: Int? = Int()
			loop until result? {
				result
				result = null
			}
			result
		""".trimIndent()
		val report = """
			start -> 2
			2: declaration & write -> 3 (Int, Expression)
			3: hint -> 3 (Int?, null)
			3: read -> 3, 3 (Int?, null)
			3: hint -> 7 (Int, null)
			3: hint -> 4 (Null, null)
			4: read -> 5 (Null, null)
			5: write -> 3 (Null, null)
			7: read -> end (Int, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("result"))
	}

	@Test
	fun `works with loops with post until generator`() {
		val sourceCode = """
			Int class
			var result: Int? = null
			loop {
				result
				result = Int()
			} until result?
			result
		""".trimIndent()
		val report = """
			start -> 2
			2: declaration & write -> 3 (Null, null)
			3: hint -> 4 (Int?, null)
			4: read -> 5 (Int?, null)
			5: write -> 6 (Int, Expression)
			6: read -> 6, 6 (Int, Expression)
			6: hint -> 7 (Int, Expression)
			6: hint -> 4 (Null, null)
			7: read -> end (Int, Expression)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("result"))
	}

	@Test
	fun `works with nested loops`() {
		val sourceCode = """
			var a = 0
			loop {
				a++
				loop over Range(0, 3) as modifier {
					a *= modifier
				}
				a
			}
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 2 (Int, 0)
			2: hint -> 3 (Int, null)
			3: read & mutation -> 4 (Int, null)
			4: hint -> 5, 7 (Int, null)
			5: read & mutation -> 5, 7 (Int, null)
			7: read -> 3 (Int, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("a"))
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
		assertStringEquals(report, tracker.childTrackers["DataProvider.getNumber(): Int"]?.getReportFor("a"))
	}

	@Test
	fun `works with raises`() {
		val sourceCode = """
			Processor object {
				to process() {
					val a: Int?
					if !a?
						raise yes
					a
				}
			}
		""".trimIndent()
		val report = """
			start -> 3
			3: declaration -> 4 (Int?, null)
			4: read -> 4, 4 (Int?, null)
			4: hint -> 6 (Int, null)
			4: hint -> end (Null, null)
			6: read -> end (Int, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.childTrackers["Processor.process()"]?.getReportFor("a"))
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
		assertStringEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `works with try block containing loops`() {
		val sourceCode = """
			var a = 10
			{
				loop over Range(0, 3) as modifier {
					a *= modifier
				}
			} always {
				a
			}
			a = 2
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 3, 7e (Int, 10)
			3: hint -> 4, 7, 7e (Int, null)
			4: read & mutation -> 4, 7, 7e (Int, null)
			7: read -> 9 (Int, null)
			7e: read -> continues raise (Int, null)
			9: write -> end (Int, 2)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `works with handle block containing loops`() {
		val sourceCode = """
			var a = 10
			{
				a
			} handle Error {
				loop over Range(0, 3) as modifier {
					a *= modifier
				}
			}
			a = 2
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 3, 5 (Int, 10)
			3: read -> 5, 9 (Int, 10)
			5: hint -> 6, 9 (Int, null)
			6: read & mutation -> 6, 9 (Int, null)
			9: write -> end (Int, 2)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("a"))
	}

	//TODO minor improvement: 6e isn't marked with 'continues raise'
	@Test
	fun `works with always block containing loops`() {
		val sourceCode = """
			var a = 10
			{
				a
			} always {
				loop over Range(0, 3) as modifier {
					a *= modifier
				}
			}
			a = 2
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 3, 5e (Int, 10)
			3: read -> 5, 5e (Int, 10)
			5: hint -> 6, 9 (Int, null)
			6: read & mutation -> 6, 9 (Int, null)
			5e: hint -> 6e (Int, null)
			6e: read & mutation -> 6e (Int, null)
			9: write -> end (Int, 2)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("a"))
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
		assertStringEquals(report, tracker.getReportFor("d"))
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
		assertStringEquals(report, tracker.getReportFor("d"))
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
		assertStringEquals(report, tracker.getReportFor("c"))
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
		assertStringEquals(report, tracker.getReportFor("value"))
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
		assertStringEquals(report, tracker.getReportFor("e"))
	}

	@Test
	fun `works with member accesses`() {
		val sourceCode = """
			val a: Point
			if a.b
				c = 3
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration -> 2 (Point, null)
			2: read -> end (Point, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("a"))
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
		assertStringEquals(report, tracker.childTrackers["A.init()"]?.getReportFor("a"))
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
		assertStringEquals(report, tracker.childTrackers["A.init()"]?.getReportFor("a"))
	}

	@Test
	fun `branches on boolean and`() {
		val sourceCode = """
			var a: Bool
			a and a
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration -> 2 (Bool, null)
			2: read -> 2, end (Bool, null)
			2: hint -> 2 (Bool, yes)
			2: read -> end (Bool, yes)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `branches on boolean or`() {
		val sourceCode = """
			var a: Bool
			a or a
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration -> 2 (Bool, null)
			2: read -> 2, end (Bool, null)
			2: hint -> 2 (Bool, no)
			2: read -> end (Bool, no)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `applies hints in boolean expressions`() {
		val sourceCode = """
			var a: Int?
			a is Int or a
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
		assertStringEquals(report, tracker.getReportFor("a"))
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
		assertStringEquals(report, tracker.getReportFor("a"))
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
		assertStringEquals(report, tracker.getReportFor("a"))
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
		assertStringEquals(report, tracker.getReportFor("a"))
	}

	//TODO add warning on expected division by zero
	@Test
	fun `ignores division by zero`() {
		val sourceCode = """
			var a = 1
			a /= 0
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 2 (Int, 1)
			2: read & mutation -> end (Int, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("a"))
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
		assertStringEquals(report, tracker.getReportFor("a"))
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
		assertStringEquals(report, tracker.getReportFor("a"))
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
		assertStringEquals(report, tracker.getReportFor("a"))
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
		assertStringEquals(report, tracker.getReportFor("a"))
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
		assertStringEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `changes type on has-value check`() {
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
			2: hint -> 3 (Int, null)
			2: hint -> 5 (Null, null)
			3: read -> end (Int, null)
			5: read -> end (Null, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("a"))
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
		assertStringEquals(report, tracker.getReportFor("a"))
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
		assertStringEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `applies hints from and expressions`() {
		val sourceCode = """
			val a: Int?
			if a is Int and a == 2 {
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
		assertStringEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `applies hints from or expressions`() {
		val sourceCode = """
			val a: Int?
			if a is Int or a == 2 {
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
		assertStringEquals(report, tracker.getReportFor("a"))
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
		assertStringEquals(report, tracker.getReportFor("a"))
	}

	@Test
	fun `can compare identity without value`() {
		val sourceCode = """
			var a = random()
			val b = a
			val c = a === b
		""".trimIndent()
		val report = """
			start -> 3
			3: declaration & write -> end (Bool, yes)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("c"))
	}

	@Test
	fun `invalidates identity on jump`() {
		val sourceCode = """
			var a: Int
			var b: Int
			loop {
				a = random()
				val c = a == b
				b = a
			}
		""".trimIndent()
		val report = """
			start -> 5
			5: declaration & write -> end (null, null)
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertStringEquals(report, tracker.getReportFor("c"))
	}
}
