package components.parsing.syntax_tree.definitions

import components.linting.Linter
import components.linting.semantic_model.definitions.FunctionImplementation as SemanticFunctionImplementationModel
import components.linting.semantic_model.general.Unit
import components.linting.semantic_model.scopes.BlockScope
import components.linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.definitions.sections.FunctionSection
import components.parsing.syntax_tree.general.Element
import components.parsing.syntax_tree.general.StatementSection
import components.parsing.syntax_tree.literals.Identifier
import components.parsing.syntax_tree.general.TypeElement
import components.tokenizer.WordAtom
import java.lang.StringBuilder

class FunctionDefinition(private val identifier: Identifier, private val parameterList: ParameterList,
						 private val body: StatementSection?, private var returnType: TypeElement?):
	Element(identifier.start, body?.end ?: returnType?.end ?: parameterList.end) {
	lateinit var parent: FunctionSection

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE, WordAtom.OVERRIDING, WordAtom.MUTATING)
	}

	override fun concretize(linter: Linter, scope: MutableScope, units: MutableList<Unit>) {
		concretize(linter, scope)
	}

	override fun concretize(linter: Linter, scope: MutableScope): SemanticFunctionImplementationModel {
		parent.validate(linter, ALLOWED_MODIFIER_TYPES)
		val isNative = parent.containsModifier(WordAtom.NATIVE)
		val isOverriding = parent.containsModifier(WordAtom.OVERRIDING)
		val isMutating = parent.containsModifier(WordAtom.MUTATING)
		val functionScope = BlockScope(scope)
		val genericParameters = parameterList.concretizeGenerics(linter, functionScope) ?: listOf()
		val parameters = parameterList.concretizeParameters(linter, functionScope)
		val returnType = returnType?.concretize(linter, scope)
		val implementation = SemanticFunctionImplementationModel(this, functionScope, genericParameters,
			parameters, body?.concretize(linter, functionScope), returnType, isNative, isOverriding, isMutating)
		scope.declareFunction(linter, identifier.getValue(), implementation)
		return implementation
	}

	override fun toString(): String {
		return StringBuilder()
			.append("Function [ ")
			.append(identifier)
			.append(" ")
			.append(parameterList)
			.append(": ")
			.append(returnType ?: "void")
			.append(" ] { ")
			.append(body ?: "")
			.append(" }")
			.toString()
	}
}
