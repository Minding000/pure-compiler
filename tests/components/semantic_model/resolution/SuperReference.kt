package components.semantic_model.resolution

import components.semantic_model.values.SuperReference
import logger.Severity
import logger.issues.resolution.*
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
		lintResult.assertIssueNotDetected<SuperReferenceOutsideOfTypeDefinition>()
		lintResult.assertIssueNotDetected<SuperReferenceOutsideOfAccess>()
		lintResult.assertIssueNotDetected<SuperInitializerCallOutsideOfInitializer>()
	}

	@Test
	fun `disallows super reference keyword outside of type definition`() {
		val sourceCode =
			"""
				super
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<SuperReferenceOutsideOfTypeDefinition>(
			"Super references are not allowed outside of type definitions.", Severity.ERROR)
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
		lintResult.assertIssueDetected<SuperReferenceOutsideOfAccess>(
			"Super references are not allowed outside of member and index accesses.", Severity.ERROR)
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
		lintResult.assertIssueDetected<SuperInitializerCallOutsideOfInitializer>(
			"The super initializer can only be called from initializers.", Severity.ERROR)
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
		lintResult.assertIssueNotDetected<SuperReferenceSpecifierNotInherited>()
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
		lintResult.assertIssueNotDetected<SuperReferenceSpecifierNotInherited>()
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
		lintResult.assertIssueDetected<SuperReferenceSpecifierNotInherited>("'Car' does not inherit from 'Machine'.",
			Severity.ERROR)
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
		lintResult.assertIssueDetected<SuperReferenceSpecifierNotInherited>("'Car' does not inherit from 'Car'.",
			Severity.ERROR)
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
		lintResult.assertIssueNotDetected<SuperMemberNotFound>()
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
		lintResult.assertIssueDetected<SuperMemberNotFound>(
			"The specified property does not exist on any super type of this type definition.", Severity.ERROR)
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
		lintResult.assertIssueNotDetected<SuperMemberNotFound>()
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
		lintResult.assertIssueDetected<SuperMemberNotFound>(
			"The specified initializer does not exist on any super type of this type definition.")
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
					overriding to travel(distance: Int) {
						super.travel(distance)
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<SuperMemberNotFound>()
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
		lintResult.assertIssueDetected<SuperMemberNotFound>(
			"The specified function does not exist on any super type of this type definition.")
	}

	@Test
	fun `allows accessing index operators that exist on a super type`() {
		val sourceCode =
			"""
				Int class
				Seat class
				Vehicle class {
					operator[seatIndex: Int]: Seat
				}
				Car class: Vehicle {
					overriding operator[seatIndex: Int]: Seat {
						return super[seatIndex]
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<SuperMemberNotFound>()
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
		lintResult.assertIssueDetected<SuperMemberNotFound>(
			"The specified index operator does not exist on any super type of this type definition.")
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
		lintResult.assertIssueNotDetected<SuperReferenceAmbiguity>()
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
		lintResult.assertIssueDetected<SuperReferenceAmbiguity>("""
			The super reference is ambiguous. Possible targets are:
			 - Bird
			 - Fish
		""".trimIndent(), Severity.ERROR)
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
		assertEquals("Vehicle", superReference?.providedType.toString())
	}
}
