package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.FunctionSection
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.StatementSection
import components.syntax_parser.syntax_tree.general.TypeElement
import components.syntax_parser.syntax_tree.literals.Identifier
import components.tokenizer.WordAtom
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation as SemanticFunctionImplementationModel

class FunctionDefinition(private val identifier: Identifier, private val parameterList: ParameterList,
						 private val body: StatementSection?, private var returnType: TypeElement?):
	Element(identifier.start, body?.end ?: returnType?.end ?: parameterList.end) {
	lateinit var parent: FunctionSection

	companion object {
		val ALLOWED_MODIFIERS = listOf(WordAtom.ABSTRACT, WordAtom.MUTATING, WordAtom.NATIVE, WordAtom.OVERRIDING)
	}

	override fun concretize(scope: MutableScope): SemanticFunctionImplementationModel {
		parent.validate(ALLOWED_MODIFIERS)
		val isAbstract = parent.containsModifier(WordAtom.ABSTRACT)
		val isMutating = parent.containsModifier(WordAtom.MUTATING)
		val isNative = parent.containsModifier(WordAtom.NATIVE)
		val isOverriding = parent.containsModifier(WordAtom.OVERRIDING)
		val functionScope = BlockScope(scope)
		val genericParameters = parameterList.concretizeGenerics(functionScope) ?: listOf()
		val parameters = parameterList.concretizeParameters(functionScope)
		val returnType = returnType?.concretize(functionScope)
		return SemanticFunctionImplementationModel(this, functionScope, genericParameters, parameters,
			body?.concretize(functionScope), returnType, isAbstract, isMutating, isNative, isOverriding)
	}

	fun getName(): String = identifier.getValue()

	override fun toString(): String {
		val stringRepresentation = StringBuilder()
			.append("Function [ ")
			.append(identifier)
			.append(" ")
			.append(parameterList)
			.append(": ")
			.append(returnType ?: "void")
			.append(" ]")
		if(body != null)
			stringRepresentation
				.append(" { ")
				.append(body)
				.append(" }")
		return stringRepresentation.toString()
	}
}
