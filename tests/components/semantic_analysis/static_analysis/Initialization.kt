package components.semantic_analysis.static_analysis

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Initialization {

	@Test
	fun `allows use of initialized local variables`() {
		val sourceCode =
			"""
				val x = 5
				x
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "hasn't been initialized")
	}

	@Test
	fun `disallows use of uninitialized local variables`() {
		val sourceCode =
			"""
				Int class
				val x: Int
				x
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Local variable 'x' hasn't been initialized yet")
	}

	@Test
	fun `allows assignments to uninitialized constant local variables`() {
		val sourceCode =
			"""
				Int class
				val x: Int
				x = 4
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "cannot be reassigned, because it is constant")
	}

	@Test
	fun `disallows assignments to possibly initialized constant local variables`() {
		val sourceCode =
			"""
				Int class
				val x: Int
				if yes
					x = 2
				x = 4
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "cannot be reassigned, because it is constant")
	}

	@Test
	fun `allows assignments to initialized local variables`() {
		val sourceCode =
			"""
				Int class
				var x = 2
				x = 4
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "cannot be reassigned, because it is constant")
	}

	@Test
	fun `disallows assignments to initialized constant local variables`() {
		val sourceCode =
			"""
				val x = 2
				x = 4
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'x' cannot be reassigned, because it is constant")
	}

	@Test
	fun `disallows assignments to parameter variables`() {
		val sourceCode =
			"""
				Microphone class {
					to setFilterThreshold(volumeInDecibels: Int) {
						volumeInDecibels = 2
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'volumeInDecibels' cannot be reassigned, because it is constant")
	}

	@Test
	fun `disallows initialization of constant properties outside of initializer`() {
		val sourceCode =
			"""
				Int class
				Human class {
					val numberOfArms: Int

					to talk() {
						numberOfArms = 3
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'numberOfArms' cannot be reassigned, because it is constant")
	}

	@Test
	fun `allows initialization of uninitialized constant properties inside of initializer`() {
		val sourceCode =
			"""
				Int class
				Human class {
					val numberOfArms: Int

					init {
						numberOfArms = 3
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "cannot be reassigned, because it is constant")
	}

	@Test
	fun `disallows initialization of initialized constant properties inside of initializer`() {
		val sourceCode =
			"""
				Int class
				Human class {
					val numberOfArms = 2

					init {
						numberOfArms = 3
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'numberOfArms' cannot be reassigned, because it is constant")
	}

	@Test
	fun `allows for properties to be initialized outside of initializer`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms = 2
					init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "The following properties have not been initialized")
	}

	@Test
	fun `allows for properties to be initialized inside of initializer`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms: Int
					init {
						numberOfArms = 2
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "The following properties have not been initialized")
	}

	@Test
	fun `allows for properties to be initialized by property parameters`() {
		val sourceCode =
			"""
				Human class {
					val age: Int
					init(age)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "The following properties have not been initialized")
	}

	@Test
	fun `ignores static properties`() {
		val sourceCode =
			"""
				Human class {
					Arm class
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "The following properties have not been initialized")
	}

	@Test
	fun `disallows initializers that don't always initialize all properties`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms: Int
					init {
						if yes
							numberOfArms = 2
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, """
			The following properties have not been initialized by this initializer:
			 - numberOfArms: Int
		""".trimIndent())
	}

	@Test
	fun `disallows uninitialized properties when no explicit initializer exists`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms: Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, """
			The following properties have not been initialized by this initializer:
			 - numberOfArms: Int
		""".trimIndent())
	}

	@Test
	fun `allows for initialized properties to be used`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms: Int
					init {
						numberOfArms = 2
						numberOfArms
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "hasn't been initialized yet")
	}

	@Test
	fun `disallows for uninitialized properties to be used`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms: Int
					init {
						numberOfArms
						numberOfArms = 2
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Property 'numberOfArms' hasn't been initialized yet")
	}

	@Test
	fun `recognizes when parameters initialize properties`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms: Int
					init(numberOfArms) {
						numberOfArms
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "hasn't been initialized yet")
	}

	@Test
	fun `recognizes when functions initialize properties`() {
		val sourceCode =
			"""
				Human class {
					var age: Int
					init {
						setNewborn()
						age
					}
					to setNewborn() {
						age = 0
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "hasn't been initialized yet")
	}

	@Test
	fun `allows for functions that rely on an initialized property to be called`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms: Int
					init {
						numberOfArms = 2
						printNumberOfArms()
					}
					to printNumberOfArms() {
						numberOfArms
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "relies on the following uninitialized properties")
	}

	@Test
	fun `allows for functions that rely on an uninitialized property to be called outside of initializers`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms: Int
					to getNumberOfArms(): Int {
						return numberOfArms
					}
					to printNumberOfArms() {
						getNumberOfArms()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "relies on the following uninitialized properties")
	}

	@Test
	fun `disallows for functions that rely on an uninitialized property to be called`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms: Int
					init {
						printNumberOfArms()
						numberOfArms = 2
					}
					to printNumberOfArms() {
						numberOfArms
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, """
			The function 'printNumberOfArms()' relies on the following uninitialized properties:
			 - numberOfArms: Int
		""".trimIndent())
	}

	//TODO also consider which properties of the super type get initialized by the super initializer
	// - implement super keyword
	// - super function calls
	// - super operator calls
	// - super initializer calls
	//   - required
	//   - no duplicate calls allowed
}
