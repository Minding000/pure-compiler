package compiler

import compiler.instructions.Copy
import compiler.instructions.Instruction
import compiler.instructions.Prt
import compiler.value_analysis.*
import java.util.*
import kotlin.collections.LinkedHashSet

class InstructionOptimizer(private val instructionList: MutableList<Instruction>) {
	private val complexInstructions = LinkedList<Instruction>()
	private val dynamicValues = LinkedHashSet<DynamicValue>()

	fun optimize() {
		analyseInstructions()
		val previousInstructionCount = instructionList.size
		val dynamicValueCount = dynamicValues.size
		removeRedundantWrites()
		resolveCopies()
		computeStaticExpressions()
		val registerCount = assignRegisters()
		println("Instruction count: $previousInstructionCount -> ${instructionList.size}")
		println("Register count: $dynamicValueCount -> $registerCount")
	}

	private fun analyseInstructions() {
		for(instruction in instructionList) {
			if(isInstructionComplex(instruction))
				complexInstructions.add(instruction)
			for(writtenDynamicValue in instruction.getWrittenDynamicValues())
				dynamicValues.add(writtenDynamicValue)
			for(readDynamicValue in instruction.getReadDynamicValues())
				readDynamicValue.usages.add(instruction)
		}
	}

	private fun computeStaticExpressions() {
		var previousInstructionCount: Int
		do {
			previousInstructionCount = instructionList.size
			val iterator = instructionList.iterator()
			while(iterator.hasNext()) {
				val instruction = iterator.next()
				val result = instruction.getStaticValue()
				if(result != null) {
					iterator.remove()
					for(dynamicValue in instruction.getWrittenDynamicValues()) {
						for(readInstruction in dynamicValue.usages)
							readInstruction.replace(dynamicValue, result)
						dynamicValues.remove(dynamicValue)
					}
				}
			}
			// Repeat this process, since computed values may enable other compiler.instructions to be statically computed
		} while(previousInstructionCount > instructionList.size)
	}

	private fun removeRedundantWrites() {
		var previousInstructionCount: Int
		do {
			previousInstructionCount = instructionList.size
			val iterator = dynamicValues.iterator()
			while(iterator.hasNext()) {
				val dynamicValue = iterator.next()
				// Remove single writes
				if(dynamicValue.usages.isEmpty()) {
					instructionList.remove(dynamicValue.getWriteInstruction())
					iterator.remove()
					continue
				}
			}
			// Repeat this process, since removed writes may make other writes redundant
		} while(previousInstructionCount > instructionList.size)
	}

	/**
	 * Removes all Copy compiler.instructions
	 */
	private fun resolveCopies() {
		// ValueScopes that can be collapsed:
		// Case 1:
		// - Write (simple aka. ASSIGN)
		// - Read (any) (multiple)
		//   - ACTION: use source register in reads
		//     - REQUIREMENT: source register value does not change in between
		//   - ACTION: use source register in reads and move to write
		//     - REQUIREMENT: other read context does not change (values, print order)
		//       - NOTE: sometimes moving to write may not work, but moving it forwards a little does work
		/* Example:
		* # setup:
		* r0 = 1
		* r2 = 2
		* r1 = r0           # redundant copy
		* 	print(r2, 2)    # block
		*     r2 = r0 + 2   # intermediate solution
		*     print(r0)     # intermediate solution
		* 	r0 = r0 + 3     # block
		* r2 = r1 + 2       # original location
		* print(r1)         # original location
		*/
		// or:
		// Case 2:
		// - Write (any)
		// - Read (simple aka. ASSIGN)
		//   - ACTION: use source expression in read
		//     - REQUIREMENT: source expression values do not change in between
		//   - ACTION: change write instruction to target assignment target
		//     - REQUIREMENT: target register not used in between
		/* Example:
		* r1 = 1
		* r2 = 2
		* r3 = 3
		* r0 = r2 + r3
		* 	r2 = r2 + 4
		* 	print(r1)
		* r1 = r0
		* print(r1)
		*/
		// or:
		// Case 3:
		// - Modify
		// - Read (simple aka. ASSIGN)
		//   - ACTION: use source expression in read
		//     - REQUIREMENT: source expression values do not change in between (only other value - this register is already guaranteed to not change)
		//   - ACTION: change modify instruction to write instruction (targeting assignment target)
		//     - REQUIREMENT: target register not used in between
		/* Example:
		* r0 = 0
		* r1 = 1
		* r2 = 2
		* r0 = r0 + r2
		*   r2 = r2 + 2
		* 	print(r1)
		* r1 = r0
		* print(r1)
		*/

		/*
		* Algorithm:
		* - create resulting instruction
		* - move forward from write considering write requirements
		* - move backwards from read considering read requirements
		* - if sections overlap, move resulting instruction to beginning of overlap
		* - requirements:
		*   - written value cannot occur.
		*   - read value can only be read
		*   - for external compiler.instructions: external instruction order cannot change
		* */

		val iterator = dynamicValues.iterator()
		while(iterator.hasNext()) {
			val dynamicValue = iterator.next()
			val writeInstruction = dynamicValue.getWriteInstruction()
			if(writeInstruction is Copy) {
				mergeInstructions(writeInstruction, dynamicValue.usages)
				// Remove dynamic value
				iterator.remove()
			}
		}
	}

	/**
	 * Checks whether the given compiler.instructions can be merged
	 * @return Whether the merge was successful and the value scope should be deleted
	 */
	private fun mergeInstructions(copyInstruction: Copy, readInstructions: List<Instruction>) {
		val valueSource = copyInstruction.valueSource
		// Remove write instruction
		instructionList.remove(copyInstruction)
		if(valueSource is DynamicValue)
			valueSource.usages.remove(copyInstruction)
		for(readInstruction in readInstructions) {
			// Modify read instruction
			readInstruction.replace(copyInstruction.targetDynamicValue, valueSource)
			if(valueSource is DynamicValue) {
				// Add value reference
				valueSource.usages.add(readInstruction)
			}
		}
	}

	/**
	 * Checks whether the given instruction changes the control flow
	 * Examples: function call, print, jump //TODO store and load fall into this as well in their own respect
	 */
	private fun isInstructionComplex(instruction: Instruction): Boolean {
		return instruction is Prt
	}

	/**
	 * Returns the index of the given instruction in the instruction list (subject to change)
	 */
	private fun getInstructionIndex(instruction: Instruction): Int {
		return instructionList.indexOf(instruction)
	}

	/**
	 * Compresses value linter.scopes into registers
	 */
	private fun assignRegisters(): Int {
		if(dynamicValues.isEmpty())
			return 0
		val remainingDynamicValues = LinkedList(dynamicValues)
		var currentIndex = 0
		var currentRegister = Register(0)
		while(!remainingDynamicValues.isEmpty()) {
			var closestStart = instructionList.size
			var closestDynamicValue: DynamicValue? = null
			for(dynamicValue in remainingDynamicValues) {
				val start = getInstructionIndex(dynamicValue.getWriteInstruction())
				if(start in currentIndex until closestStart) {
					closestStart = start
					closestDynamicValue = dynamicValue
				}
			}
			if(closestDynamicValue == null) {
				currentRegister = Register(currentRegister.index + 1)
				currentIndex = 0
				continue
			}
			remainingDynamicValues.remove(closestDynamicValue)
			closestDynamicValue.register = currentRegister
			currentIndex = getInstructionIndex(closestDynamicValue.usages.last)
		}
		return currentRegister.index + 1
	}
}