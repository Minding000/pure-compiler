package elements.identifier

import code.InstructionGenerator
import errors.user.ResolveError
import scopes.Scope
import value_analysis.DynamicValue
import value_analysis.ValueSource
import word_generation.Word

class VariableReference(scope: Scope, word: Word): IdentifierReference<VariableIdentifier>(scope, word), Variable {
	var context: IdentifierReference<VariableIdentifier>? = null

	override fun resolve() {
		val ctx = context
		if(ctx == null) {
			target = scope.getVariableIdentifierRecursive(this)
		} else {
			target = ctx.requireType().getClass().subScope.getIdentifierOrNull(this.getValue()) as? VariableIdentifier ?: throw ResolveError(this)
		}
	}

	override fun getNewDynamicValue(instructionGenerator: InstructionGenerator): DynamicValue {
		return requireTarget().getNewDynamicValue(instructionGenerator)
	}

	override fun generateInstructions(generator: InstructionGenerator): ValueSource {
		return requireTarget().generateInstructions(generator)
	}

	override fun toString(): String {
		return "VariableReference { ${getValue()} }"
	}
}