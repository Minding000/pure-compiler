package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.definitions.FunctionImplementation
import linting.semantic_model.general.Unit
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.definitions.sections.FunctionSection
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.general.StatementSection
import parsing.syntax_tree.literals.Identifier
import parsing.syntax_tree.general.TypeElement
import parsing.tokenizer.WordAtom
import java.lang.StringBuilder

class FunctionDefinition(private val identifier: Identifier, private val genericsList: GenericsList?,
						 private val parameterList: ParameterList, private val body: StatementSection?,
						 private var returnType: TypeElement?):
	Element(identifier.start, body?.end ?: returnType?.end ?: parameterList.end) {
	lateinit var parent: FunctionSection

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE, WordAtom.OVERRIDE)
	}

	override fun concretize(linter: Linter, scope: MutableScope, units: MutableList<Unit>) {
		concretize(linter, scope)
	}

	override fun concretize(linter: Linter, scope: MutableScope): FunctionImplementation {
		parent.validate(linter, ALLOWED_MODIFIER_TYPES)
		val isNative = parent.containsModifier(WordAtom.NATIVE)
		val isOverriding = parent.containsModifier(WordAtom.OVERRIDE)
		val functionScope = BlockScope(scope)
		val genericParameters = genericsList?.concretizeGenerics(linter, scope) ?: listOf()
		val parameters = parameterList.concretizeParameters(linter, functionScope)
		val returnType = returnType?.concretize(linter, scope)
		val implementation = FunctionImplementation(this, functionScope, genericParameters, parameters,
			body?.concretize(linter, functionScope), returnType, isNative, isOverriding)
		scope.declareFunction(linter, identifier.getValue(), implementation)
		return implementation
	}

	override fun toString(): String {
		val string = StringBuilder()
		string.append("Function [ ").append(identifier)
		if(genericsList != null)
			string.append(" ")
				.append(genericsList)
		string.append(" ")
			.append(parameterList)
			.append(": ")
			.append(returnType ?: "void")
			.append(" ] { ")
			.append(body ?: "")
			.append(" }")
		return string.toString()
	}
}