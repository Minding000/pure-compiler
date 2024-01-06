package components.code_generation.operators

import components.code_generation.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class InstanceAccess {

	@Test
	fun `compiles instance accesses on object types`() {
		val sourceCode = """
			Train class {
				instances DEFAULT(12)
				val id: Int
				init(id)
			}
			SimplestApp object {
				to getTwelve(): Int {
					val defaultTrain: Train = .DEFAULT
					return defaultTrain.id
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTwelve")
		assertEquals(12, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles instance accesses on generic types`() {
		val sourceCode = """
			referencing Pure
			abstract Vehicle class {
				abstract instances DEFAULT
				var id = 0
			}
			Train class: Vehicle & Identifiable {
				overriding instances DEFAULT(2)
				init(id)
			}
			Station class {
				containing V: Vehicle
				to getId(): Int {
					val defaultTrain: V = .DEFAULT
					return defaultTrain.id
				}
			}
			SimplestApp object {
				to getTwo(): Int {
					val station = <Train>Station()
					return station.getId()
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTwo", true)
		assertEquals(2, Llvm.castToSignedInteger(result))
	}
}
