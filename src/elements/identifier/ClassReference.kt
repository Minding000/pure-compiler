package elements.identifier

import code.InstructionGenerator
import elements.definitions.ClassDefinition
import scopes.Scope
import types.Type
import value_analysis.ValueSource
import word_generation.Word

class ClassReference(scope: Scope, word: Word): IdentifierReference<ClassIdentifier>(scope, word), Type {
	override val name: String
		get() = getValue()

	override fun resolve() {
		target = scope.getClassIdentifierRecursive(this)
	}

	override fun getClass(): ClassDefinition {
		return requireTarget().definition
	}

	override fun generateInstructions(generator: InstructionGenerator): ValueSource {
		throw UnsupportedOperationException("Classes do not generate instructions.")
	}

	override fun toString(): String {
		return "ClassReference { ${getValue()} }"
	}
}