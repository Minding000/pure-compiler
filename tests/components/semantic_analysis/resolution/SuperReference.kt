package components.semantic_analysis.resolution

import components.semantic_analysis.semantic_model.values.SuperReference
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class SuperReference {

	@Test
	fun `allows super initializer reference inside of initializer`() {
		val sourceCode =
			"""
				Vehicle class
				Car class: Vehicle {
					init {
						super.init()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"Super references are not allowed outside of type definitions")
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"Super references are not allowed outside of member and index accesses")
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "super initializer can only be called from initializers")
	}

	@Test
	fun `disallows super reference keyword outside of type definition`() {
		val sourceCode =
			"""
				super
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Super references are not allowed outside of type definitions")
	}

	@Test
	fun `disallows super reference keyword outside of member or index accesses`() {
		val sourceCode =
			"""
				Car class {
					to getCar(): Car {
						return super
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Super references are not allowed outside of member and index accesses")
	}

	@Test
	fun `disallows super initializer reference outside of initializer`() {
		val sourceCode =
			"""
				Car class {
					to getCar(): Car {
						super.init()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "The super initializer can only be called from initializers")
	}

	@Test
	fun `allows specifying super type that the type definition inherits from directly`() {
		val sourceCode =
			"""
				Vehicle class
				Car class: Vehicle {
					init {
						super<Vehicle>
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "does not inherit from")
	}

	@Test
	fun `allows specifying super type that the type definition inherits from indirectly`() {
		val sourceCode =
			"""
				Machine class
				Vehicle class: Machine
				Car class: Vehicle {
					init {
						super<Machine>
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "does not inherit from")
	}

	@Test
	fun `disallows specifying super type that the type definition doesn't inherit from`() {
		val sourceCode =
			"""
				Machine class
				Car class {
					init {
						super<Machine>
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'Car' does not inherit from 'Machine'")
	}

	@Test
	fun `disallows specifying the type definition itself`() {
		val sourceCode =
			"""
				Car class {
					init {
						super<Car>
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'Car' does not inherit from 'Car'")
	}

	@Test
	fun `allows accessing properties that exist on a super type`() {
		val sourceCode =
			"""
				Vehicle class {
					val capacity = 50
				}
				Car class: Vehicle {
					to getNumberOfSeats(): Int {
						return super.capacity
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"The specified member does not exist on any super type of this type definition")
	}

	@Test
	fun `disallows accessing properties that don't exist on a super type`() {
		val sourceCode =
			"""
				Vehicle class
				Car class: Vehicle {
					to getNumberOfSeats(): Int {
						return super.capacity
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"The specified member does not exist on any super type of this type definition")
	}

	@Test
	fun `allows accessing initializers that exist on a super type`() {
		val sourceCode =
			"""
				Int class
				Vehicle class {
					init(capacity: Int)
				}
				Car class: Vehicle {
					init(numberOfSeats: Int) {
						super.init(numberOfSeats)
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"The specified member does not exist on any super type of this type definition")
	}

	@Test
	fun `disallows accessing initializers that don't exist on a super type`() {
		val sourceCode =
			"""
				Int class
				Vehicle class
				Car class: Vehicle {
					init(numberOfSeats: Int) {
						super.init(numberOfSeats)
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"The specified member does not exist on any super type of this type definition")
	}

	@Test
	fun `allows accessing functions that exist on a super type`() {
		val sourceCode =
			"""
				Int class
				Vehicle class {
					to travel(distance: Int)
				}
				Car class: Vehicle {
					to travel(distance: Int) {
						super.travel(distance)
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"The specified member does not exist on any super type of this type definition")
	}

	@Test
	fun `disallows accessing functions that don't exist on a super type`() {
		val sourceCode =
			"""
				Int class
				Vehicle class {
					to travel()
				}
				Car class: Vehicle {
					to travel(distance: Int) {
						super.travel(distance)
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"The specified member does not exist on any super type of this type definition")
	}

	@Test
	fun `allows accessing index operators that exist on a super type`() {
		val sourceCode =
			"""
				Int class
				Vehicle class {
					operator[seatIndex: Int]
				}
				Car class: Vehicle {
					operator[seatIndex: Int] {
						return super[seatIndex]
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"The specified member does not exist on any super type of this type definition")
	}

	@Test
	fun `disallows accessing index operators that don't exist on a super type`() {
		val sourceCode =
			"""
				Int class
				Vehicle class
				Car class: Vehicle {
					operator[seatIndex: Int] {
						return super[seatIndex]
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"The specified member does not exist on any super type of this type definition")
	}

	@Test
	fun `allows unambiguous access`() {
		val sourceCode =
			"""
				Bird class {
					to glide()
				}
				Fish class
				FlyingFish class: Bird & Fish {
					to move() {
						super.glide()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "The super reference is ambiguous")
	}

	@Test
	fun `disallows ambiguous access`() {
		val sourceCode =
			"""
				Bird class {
					to glide()
				}
				Fish class {
					to glide()
				}
				FlyingFish class: Bird & Fish {
					to move() {
						super.glide()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,"""
			The super reference is ambiguous. Possible targets are:
			 - Bird
			 - Fish
		""".trimIndent())
	}

	@Test
	fun `resolves to specific super type`() {
		val sourceCode =
			"""
				Vehicle class {
					val capacity = 50
				}
				Car class: Vehicle {
					to getNumberOfSeats(): Int {
						return super.capacity
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val superReference = lintResult.find<SuperReference>()
		assertEquals("Vehicle", superReference?.type.toString())
	}
}
