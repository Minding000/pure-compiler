package code

import analysis.Occurrence
import analysis.OccurrenceType
import analysis.ValueScope
import errors.CompilerError
import instructions.*
import objects.Instruction
import objects.Register
import objects.Value
import util.jumpTo
import java.util.*
import kotlin.collections.HashMap

class InstructionOptimizer(private val instructionList: MutableList<Instruction>) {
	private val complexInstructions = LinkedList<Instruction>()
	private val valueScopeList = LinkedList<ValueScope>()
	//TODO registers don't really matter that much.
	// So, this can probably be simplified by using value scopes as values and assigning those registers in the end
	private val valueScopes = HashMap<Register, LinkedList<ValueScope>>()
	private val instructions = HashMap<Register, LinkedList<Instruction>>()

	fun optimize() {
		generateValueScopes()
		val previousInstructionCount = instructionList.size
		val previousRegisterCount = instructions.size
		removeUselessWrites()
		removeUselessAssignments()
		reduceRegisterCount()
		println("Instruction count: $previousInstructionCount -> ${instructionList.size}")
		println("Register count: $previousRegisterCount -> ${instructions.size}")
	}

	private fun generateValueScopes() {
		for(instruction in instructionList) {
			if(isInstructionComplex(instruction))
				complexInstructions.add(instruction)
			for(register in instruction.getRegisters())
				instructions.getOrPut(register) { LinkedList() }.add(instruction)
			when(instruction) {
				is BinaryInstruction -> {
					when (instruction.outputRegister) {
						instruction.leftValueSource -> {
							var valueScope = getCurrentValueScope(instruction.outputRegister)
							valueScope.occurrences.add(Occurrence(instruction, instruction.outputRegister, OccurrenceType.MODIFY))
							val rightValueSource = instruction.rightValueSource
							if(rightValueSource is Register) {
								valueScope = getCurrentValueScope(rightValueSource)
								valueScope.occurrences.add(Occurrence(instruction, rightValueSource, OccurrenceType.READ))
							}
						}
						instruction.rightValueSource -> {
							var valueScope = getCurrentValueScope(instruction.outputRegister)
							valueScope.occurrences.add(Occurrence(instruction, instruction.outputRegister, OccurrenceType.MODIFY))
							val leftValueSource = instruction.leftValueSource
							if(leftValueSource is Register) {
								valueScope = getCurrentValueScope(leftValueSource)
								valueScope.occurrences.add(Occurrence(instruction, leftValueSource, OccurrenceType.READ))
							}
						}
						else -> {
							addValueScope(ValueScope(instruction.outputRegister, instruction))
							var valueScope: ValueScope
							val leftValueSource = instruction.leftValueSource
							if(leftValueSource is Register) {
								valueScope = getCurrentValueScope(leftValueSource)
								valueScope.occurrences.add(Occurrence(instruction, leftValueSource, OccurrenceType.READ))
							}
							val rightValueSource = instruction.rightValueSource
							if(rightValueSource is Register) {
								valueScope = getCurrentValueScope(rightValueSource)
								valueScope.occurrences.add(Occurrence(instruction, rightValueSource, OccurrenceType.READ))
							}
						}
					}
				}
				is Copy -> {
					addValueScope(ValueScope(instruction.targetRegister, instruction))
					val valueScope = getCurrentValueScope(instruction.sourceRegister)
					valueScope.occurrences.add(Occurrence(instruction, instruction.sourceRegister, OccurrenceType.READ))
				}
				is Init -> {
					addValueScope(ValueScope(instruction.targetRegister, instruction))
				}
				is Prt -> {
					for(valueSource in instruction.valueSources) {
						if(valueSource is Register) {
							val valueScope = getCurrentValueScope(valueSource)
							valueScope.occurrences.add(Occurrence(instruction, valueSource, OccurrenceType.READ))
						}
					}
				}
			}
		}
	}

	private fun addValueScope(valueScope: ValueScope) {
		valueScopeList.add(valueScope)
		valueScopes.getOrPut(valueScope.register) { LinkedList() }.add(valueScope)
	}

	private fun getCurrentValueScope(register: Register): ValueScope {
		return valueScopes[register]?.lastOrNull() ?: throw CompilerError("Register usage before initialization (Register #${register.index}).")
	}

	private fun removeUselessWrites() {
		// Example:
		// - Write	- invalid: no usages
		// - Write	- valid
		// - Modify	- valid
		// - Modify	- valid
		// - Read	- valid
		// - Read	- valid
		// - Modify	- invalid: no usages
		// - Modify - invalid: no usages
		// - Write	- invalid: no usages
		var previousInstructionCount: Int
		do {
			previousInstructionCount = instructionList.size
			val iterator = valueScopeList.descendingIterator()
			while(iterator.hasNext()) {
				val valueScope = iterator.next()
				// Remove single writes
				if(valueScope.occurrences.isEmpty()) {
					instructionList.remove(valueScope.writeInstruction)
					iterator.remove()
					for(register in valueScope.writeInstruction.getRegisters()) {
						instructions[register]?.remove(valueScope.writeInstruction)
						valueScopes[register]?.remove(valueScope)
					}
					continue
				}
				while(valueScope.occurrences.last.type != OccurrenceType.READ) {
					// Remove unnecessary modifications
					val instruction = valueScope.occurrences.pop().instruction
					instructionList.remove(instruction)
					for(register in instruction.getRegisters())
						instructions[register]?.remove(instruction)
					// Remove single writes
					if(valueScope.occurrences.isEmpty()) {
						instructionList.remove(valueScope.writeInstruction)
						iterator.remove()
						for(register in valueScope.writeInstruction.getRegisters()) {
							instructions[register]?.remove(valueScope.writeInstruction)
							valueScopes[register]?.remove(valueScope)
						}
						break
					}
				}
			}
			// Repeat this process, since removed instructions may have been targets of other instructions
		} while(previousInstructionCount > instructionList.size)
	}

	private fun removeUselessAssignments() {
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
		*   - for external instructions: external instruction order cannot change
		* */

		val iterator = valueScopeList.listIterator()
		while(iterator.hasNext()) {
			val valueScope = iterator.next()
			if(valueScope.occurrences.size > 0) {
				val writeInstruction = valueScope.writeInstruction
				// Case 1:
				if(writeInstruction is WriteInstruction) {
					val readInstructions = LinkedList<Instruction>()
					var onlyReadOccurrences = true
					for(occurrence in valueScope.occurrences) {
						if(occurrence.type != OccurrenceType.READ) {
							onlyReadOccurrences = false
							break
						}
						readInstructions.add(occurrence.instruction)
					}
					if(onlyReadOccurrences && mergeInstructionsIfPossible(writeInstruction, readInstructions)) {
						// Remove value scope
						iterator.remove()
						valueScopes[valueScope.register]?.remove(valueScope)
					}
				}
			}
		}
	}

	/**
	 * Checks whether the given instructions can be merged
	 * @return Whether the merge was successful and the value scope should be deleted
	 */
	private fun mergeInstructionsIfPossible(writeInstruction: WriteInstruction, readInstructions: LinkedList<Instruction>): Boolean {
		if(writeInstruction is Init && !writeInstruction.value.isInlineable)
			return false
		// Check if possible
		val writeIndex = getInstructionIndex(writeInstruction)
		val distanceMap = HashMap<Instruction, Int>()
		for(readInstruction in readInstructions) {
			val readIndex = getInstructionIndex(readInstruction)
			val distance = readIndex - writeIndex - 1
			var deltaForwards = 0
			if(distance > 0) {
				deltaForwards = if(writeInstruction is Copy)
					getCopyHaste(writeInstruction, writeIndex, distance)
				else
					getComplexHaste(writeInstruction, writeIndex, distance)
				val deltaBackwards = getReadLaziness(readInstruction, readIndex,distance)
				if(deltaForwards + deltaBackwards < distance)
					return false
			}
			distanceMap[readInstruction] = deltaForwards
		}
		// Remove write instruction
		instructionList.remove(writeInstruction)
		for(register in writeInstruction.getRegisters())
			instructions[register]?.remove(writeInstruction)
		val valueSource = writeInstruction.getValueSource()
		val sourceScope = if(valueSource is Register) getValueScope(valueSource, writeIndex - 1) else null
		for(_readInstruction in readInstructions) {
			var readInstruction = _readInstruction
			val newIndex = writeIndex + distanceMap[readInstruction]!! // Indices created using same loop
			// Remove read
			val previousReadIndex = getInstructionIndex(readInstruction)
			instructionList.remove(readInstruction)
			for(register in readInstruction.getRegisters())
				instructions[register]?.remove(readInstruction)
			// Modify read instruction
			if(readInstruction is Copy && valueSource is Value) {
				readInstruction = Init(readInstruction.targetRegister, valueSource)
				val targetScope = getValueScope(readInstruction.targetRegister, previousReadIndex)
				targetScope.writeInstruction = readInstruction
			} else {
				readInstruction.replace(writeInstruction.targetRegister, valueSource)
			}
			sourceScope?.occurrences?.add(Occurrence(readInstruction, sourceScope.register, OccurrenceType.READ))
			// Reinsert read instruction at new position
			instructionList.add(newIndex, readInstruction)
			for(register in readInstruction.getRegisters())
				instructions[register]?.add(readInstruction)
			if(valueSource is Register) {
				// Insert instruction in source register value scope
				val valueScope = getValueScope(valueSource, writeIndex)
				val iterator = valueScope.occurrences.listIterator()
				while(iterator.hasNext()) {
					val occurrence = iterator.next()
					if(getInstructionIndex(occurrence.instruction) >= newIndex) {
						iterator.previous()
						val isModification = readInstruction.writesTo(valueSource)
						iterator.add(Occurrence(readInstruction, valueSource, if (isModification) OccurrenceType.MODIFY else OccurrenceType.READ))
						break
					}
				}
			}
		}
		return true
	}

	/**
	 * Checks whether the given instruction changes the control flow
	 * Examples: function call, print, jump
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
	 * Returns the active value scope of the given register at the given index
	 */
	private fun getValueScope(register: Register, index: Int): ValueScope {
		var activeScope: ValueScope? = null
		for(valueScope in valueScopes[register] ?: throw CompilerError("Register without value scope: r${register.index}")) {
			val start = getInstructionIndex(valueScope.writeInstruction)
			if(start > index)
				break
			activeScope = valueScope
		}
		if(activeScope == null)
			throw CompilerError("Register without value scope: r${register.index}")
		return activeScope
	}

	/**
	 * Gets by which distance the given copy instruction can be moved forwards without breaking the code
	 * @param copyInstruction The copy instruction to be moved
	 * @param maxDistance The maximum distance to check for
	 */
	private fun getCopyHaste(copyInstruction: Copy, index: Int, maxDistance: Int): Int {
		val register = copyInstruction.sourceRegister
		val registerInstructions = instructions[register] ?: throw CompilerError("Register without instructions: r${register.index}")
		val iterator = registerInstructions.iterator()
		iterator.jumpTo(copyInstruction)
		while(iterator.hasNext()) {
			val instruction = iterator.next()
			if(instruction.writesTo(register)) {
				val start = getInstructionIndex(instruction)
				if(start > index)
					return (start - index - 1).coerceAtMost(maxDistance)
			}
		}
		return maxDistance
	}

	private fun getComplexHaste(complexInstruction: Instruction, index: Int, maxDistance: Int): Int {
		//TODO "complex" should be split into:
		// - write output
		// - memory access
		// - unknown (function call)
		return maxDistance
	}

	private fun getReadLaziness(readInstruction: Instruction, index: Int, maxDistance: Int): Int {
		var minDistance = maxDistance
		if(isInstructionComplex(readInstruction)) {
			val complexIndex = complexInstructions.indexOf(readInstruction)
			if(complexIndex > 0) {
				val previousComplexInstruction = complexInstructions[complexIndex - 1]
				val distance = index - getInstructionIndex(previousComplexInstruction) - 1
				if(distance < minDistance)
					minDistance = distance
			}
		}
		for(register in readInstruction.getWrittenRegisters()) {
			val registerInstructions = instructions[register] ?: throw CompilerError("Register without instructions: r${register.index}")
			val iterator = registerInstructions.descendingIterator()
			iterator.jumpTo(readInstruction)
			if(iterator.hasNext()) {
				val distance = index - getInstructionIndex(iterator.next()) - 1
				if(distance < minDistance)
					minDistance = distance
			}
		}
		for(register in readInstruction.getReadRegisters()) {
			val registerInstructions = instructions[register] ?: throw CompilerError("Register without instructions: r${register.index}")
			val iterator = registerInstructions.descendingIterator()
			iterator.jumpTo(readInstruction)
			while(iterator.hasNext()) {
				val instruction = iterator.next()
				if(instruction.writesTo(register)) {
					val distance = index - getInstructionIndex(instruction) - 1
					if(distance < minDistance)
						minDistance = distance
				}
			}
		}
		return minDistance
	}

	/**
	 * Compresses value scopes into registers
	 */
	private fun reduceRegisterCount() {
		val remainingValueScopes = LinkedList(valueScopeList)
		instructions.clear()
		valueScopes.clear()
		var currentIndex = 0
		var currentRegister = Register(0)
		while(!remainingValueScopes.isEmpty()) {
			var closestStart = instructionList.size
			var closestValueScope: ValueScope? = null
			for(valueScope in remainingValueScopes) {
				val start = getInstructionIndex(valueScope.writeInstruction)
				if(start in currentIndex until closestStart) {
					closestStart = start
					closestValueScope = valueScope
				}
			}
			if(closestValueScope == null) {
				currentRegister = Register(currentRegister.index + 1)
				currentIndex = 0
				continue
			}
			remainingValueScopes.remove(closestValueScope)
			closestValueScope.writeInstruction.replace(closestValueScope.register, currentRegister)
			for(occurrence in closestValueScope.occurrences)
				occurrence.instruction.replace(closestValueScope.register, currentRegister)
			closestValueScope.register = currentRegister
			currentIndex = getInstructionIndex(closestValueScope.occurrences.last.instruction)

			valueScopes.getOrPut(currentRegister) { LinkedList() }.add(closestValueScope)
			instructions.getOrPut(currentRegister) { LinkedList() }.add(closestValueScope.writeInstruction)
			for(occurrence in closestValueScope.occurrences)
				instructions.getOrPut(currentRegister) { LinkedList() }.add(occurrence.instruction)
		}
	}
}