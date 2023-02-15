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
			1: declaration & write -> end
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReport("a"))
	}

	@Test
	fun `works with multiple statements`() {
		val sourceCode = """
			var b = 0
			b
		""".trimIndent()
		val report = """
			start -> 1
			1: declaration & write -> 2
			2: read -> end
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReport("b"))
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
			3: declaration -> end
		""".trimIndent()
		val reportForB = """
			start -> 2
			2: declaration & write -> 4
			4: read -> end
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(reportForA, tracker.getReport("a"))
		assertEquals(reportForB, tracker.getReport("b"))
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
			1: declaration & write -> 3, 5
			3: read -> 7
			5: write -> 7
			7: read -> end
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReport("a"))
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
			1: declaration & write -> 3
			3: write -> 3, 8, end
			8: read -> 3
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReport("a"))
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
			1: declaration & write -> 3, 5
			3: read & mutation -> 3, 5
			5: read -> end
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReport("a"))
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
			4: declaration & write -> 7, end
			7: read -> end
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.childTrackers["getNumber"]?.getReport("a"))
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
			1: declaration & write -> 3, 6, 8e
			3: write -> 4, 6, 8e
			4: read -> 6, 8, 8e
			6: write -> 8e
			8: read -> 10
			8e: read -> continues raise
			10: write -> end
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReport("a"))
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
			1: declaration & write -> 3, end
			3: read -> end
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReport("c"))
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
			2: declaration & write -> end
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReport("value"))
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
			3: declaration & write -> end
		""".trimIndent()
		val tracker = TestUtil.analyseDataFlow(sourceCode)
		assertEquals(report, tracker.getReport("e"))
	}
}
