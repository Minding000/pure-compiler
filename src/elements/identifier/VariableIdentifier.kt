package elements.identifier

import code.InstructionGenerator
import errors.internal.CompilerError
import value_analysis.DynamicValue
import elements.ValueElement
import scopes.Scope
import word_generation.Word

class VariableIdentifier(override val parentScope: Scope, word: Word): ValueElement(word.start, word.end), Identifier, Variable {
	override val name = getValue()

	override fun serializeDeclarationPosition(): String {
		return getRegionString()
	}

	override fun generateInstructions(generator: InstructionGenerator): DynamicValue {
		return generator.linkedDynamicValues[this] ?: throw CompilerError("Accessing uninitialized identifier '${getValue()}'.")
	}

	override fun getNewDynamicValue(instructionGenerator: InstructionGenerator): DynamicValue {
		val dynamicValue = DynamicValue()
		instructionGenerator.linkedDynamicValues[this] = dynamicValue
		return dynamicValue
	}

	override fun toString(): String {
		return "VariableIdentifier { ${getValue()} }"
	}
}