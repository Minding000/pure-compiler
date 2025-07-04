package components.semantic_model.static_analysis

import logger.Severity
import logger.issues.initialization.ConstantReassignment
import logger.issues.initialization.NotInitialized
import logger.issues.initialization.ReliesOnUninitializedProperties
import logger.issues.initialization.UninitializedProperties
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil

internal class InitializationState {

	//TODO implement: this is a niche feature that requires an extra "initializing [function]" keyword to ensure the functions are only called from initializers (and only once)
	@Disabled
	@Test
	fun `allow assignment of constants in functions`() {
		val sourceCode =
			"""
				SantaApp object {
					val remainingHos
					init {
						loadHos()
					}
					to loadHos() {
						remainingHos = 5
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ConstantReassignment>()
	}

	@Disabled
	@Test
	fun `disallow initialization functions to be called outside of the constructor`() {
		val sourceCode =
			"""
				SantaApp object {
					val remainingHos
					init {
						loadHos()
					}
					to start() {
						loadHos()
					}
					to loadHos() {
						remainingHos = 5
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ConstantReassignment>()
	}

	@Test
	fun `does not mistake value usage for property initialization`() {
		val sourceCode =
			"""
				 Printer object {
					val speed = 20
					to print() {
						checkSpeed()
					}
					to checkSpeed() {
						loop {
							speed
							if yes
								break
						}
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ConstantReassignment>()
	}

	@Test
	fun `allows use of initialized local variables`() {
		val sourceCode =
			"""
				val x = 5
				x
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotInitialized>()
	}

	@Test
	fun `allows use of initialized local variables after loop`() {
		val sourceCode =
			"""
			var a = 0
			loop over Range(0, 5) as number {
				a += number
			}
			a
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotInitialized>()
	}

	@Test
	fun `allows use of static local variables`() {
		val sourceCode =
			"""
				Int class
				Int()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotInitialized>()
	}

	@Test
	fun `allows use of global values`() {
		val sourceCode =
			"""
				Int
				Int object
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotInitialized>()
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
		lintResult.assertIssueDetected<NotInitialized>("Local variable 'x' hasn't been initialized yet.", Severity.ERROR)
	}

	@Test
	fun `allows declaration of constants in loops`() {
		val sourceCode =
			"""
				loop {
					val a = 5
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ConstantReassignment>()
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
		lintResult.assertIssueNotDetected<ConstantReassignment>()
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
		lintResult.assertIssueDetected<ConstantReassignment>("'x' cannot be reassigned, because it is constant.",
			Severity.ERROR)
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
		lintResult.assertIssueNotDetected<ConstantReassignment>()
	}

	@Test
	fun `disallows assignments to initialized constant local variables`() {
		val sourceCode =
			"""
				val x = 2
				x = 4
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ConstantReassignment>("'x' cannot be reassigned, because it is constant.")
	}

	@Test
	fun `disallows assignments to constant properties`() {
		val sourceCode =
			"""
				Glasses class {
					val frameId: Int
				}
				val glasses = Glasses()
				glasses.frameId = 4
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ConstantReassignment>("'frameId' cannot be reassigned, because it is constant.")
	}

	@Test
	fun `disallows assignments to functions`() {
		val sourceCode =
			"""
				Glasses class {
					to clean()
				}
				val glasses = Glasses()
				glasses.clean = 4
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ConstantReassignment>("'clean' cannot be reassigned, because it is constant.")
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
		lintResult.assertIssueDetected<ConstantReassignment>("'volumeInDecibels' cannot be reassigned, because it is constant.")
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
		lintResult.assertIssueDetected<ConstantReassignment>("'numberOfArms' cannot be reassigned, because it is constant.")
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
		lintResult.assertIssueNotDetected<ConstantReassignment>()
	}

	@Test
	fun `disallows initialization of initialized constant properties inside of initializer`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms = 2
					init {
						numberOfArms = 3
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ConstantReassignment>("'numberOfArms' cannot be reassigned, because it is constant.")
	}

	@Test
	fun `disallows initialization of initialized constant properties inside of initializer using self references`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms = 2
					init {
						this.numberOfArms = 3
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ConstantReassignment>("'numberOfArms' cannot be reassigned, because it is constant.")
	}

	@Test
	fun `disallows initialization of initialized constant properties inside of initializer using property parameters`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms = 2
					init(numberOfArms)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ConstantReassignment>("'numberOfArms' cannot be reassigned, because it is constant.")
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
		lintResult.assertIssueNotDetected<UninitializedProperties>()
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
		lintResult.assertIssueNotDetected<UninitializedProperties>()
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
		lintResult.assertIssueNotDetected<UninitializedProperties>()
	}

	@Test
	fun `allows for properties to be initialized by native initializer`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms: Int
					native init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<UninitializedProperties>()
	}

	@Test
	fun `ignores static properties`() {
		val sourceCode =
			"""
				Human class {
					Arm class
					init {
						createArm()
					}
					to createArm(): Arm {
						return Arm()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<UninitializedProperties>()
		lintResult.assertIssueNotDetected<ReliesOnUninitializedProperties>()
	}

	@Test
	fun `ignores abstract properties`() {
		val sourceCode =
			"""
				abstract Device class {
					abstract val inputVoltage: Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<UninitializedProperties>()
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
		lintResult.assertIssueDetected<UninitializedProperties>("""
			The following properties have not been initialized by this initializer:
			 - numberOfArms: Int
		""".trimIndent(), Severity.ERROR)
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
		lintResult.assertIssueDetected<UninitializedProperties>("""
			The following properties have not been initialized by this initializer:
			 - numberOfArms: Int
		""".trimIndent())
	}

	@Test
	fun `disallows initializers that don't initialize all super properties`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms: Int
					init {
						numberOfArms = 2
					}
				}
				Magician class: Human {
					init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<UninitializedProperties>("""
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
		lintResult.assertIssueNotDetected<NotInitialized>()
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
		lintResult.assertIssueDetected<NotInitialized>("Property 'numberOfArms' hasn't been initialized yet.")
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
		lintResult.assertIssueNotDetected<NotInitialized>()
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
		lintResult.assertIssueNotDetected<NotInitialized>()
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
		lintResult.assertIssueNotDetected<ReliesOnUninitializedProperties>()
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
		lintResult.assertIssueNotDetected<ReliesOnUninitializedProperties>()
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
		lintResult.assertIssueDetected<ReliesOnUninitializedProperties>("""
			The callable 'printNumberOfArms()' relies on the following uninitialized properties:
			 - numberOfArms: Int
		""".trimIndent(), Severity.ERROR)
	}

	@Test
	fun `allows for initialization through super initializers`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms: Int
					init {
						numberOfArms = 2
					}
				}
				Magician class: Human {
					init {
						super.init()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ConstantReassignment>()
		lintResult.assertIssueNotDetected<UninitializedProperties>()
	}

	@Test
	fun `disallows duplicate super initializer calls`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms: Int
					init {
						numberOfArms = 2
					}
				}
				Magician class: Human {
					init {
						super.init()
						super.init()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ConstantReassignment>("'numberOfArms' cannot be reassigned, because it is constant.")
	}

	@Test
	fun `allows for initialization through another initializer`() {
		val sourceCode =
			"""
				Human class {
					val numberOfArms: Int
					init(numberOfArms)
					init {
						init(2)
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ConstantReassignment>()
		lintResult.assertIssueNotDetected<UninitializedProperties>()
	}

	@Test
	fun `allows inner classes and objects`() {
		val sourceCode =
			"""
				Map class {
					Faction enum
					PlayerSpawn object
					Details class
					bound EnemySpawn class
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotInitialized>()
		lintResult.assertIssueNotDetected<UninitializedProperties>()
	}

	@Test
	fun `discerns initializers of current and parent class`() {
		val sourceCode =
			"""
				Int class
				Map class {
					val size: Int
					init(size)
					bound EnemySpawn class {
						init {
							size
						}
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotInitialized>()
	}

	@Test
	fun `doesn't require computed properties to be initialized`() {
		val sourceCode =
			"""
				Int class
				Map class {
					computed size: Int gets Int()
					init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<UninitializedProperties>()
	}

	@Test
	fun `allows calling secondary initializer when computed property is present`() {
		val sourceCode =
			"""
				Int class
				Map class {
					computed size: Int gets Int()
					init
					init(a: Int) {
						init()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ConstantReassignment>()
	}

	@Test
	fun `allows calling secondary initializer to initialize a property`() {
		val sourceCode =
			"""
				Int class
				Map class {
					val size: Int
					init {
						size = Int()
						size
					}
					init(a: Int) {
						init()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<UninitializedProperties>()
		lintResult.assertIssueNotDetected<ReliesOnUninitializedProperties>()
	}
}
