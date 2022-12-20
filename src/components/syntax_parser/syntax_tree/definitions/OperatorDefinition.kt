package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.OperatorSection
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.StatementSection
import components.syntax_parser.syntax_tree.general.TypeElement
import components.tokenizer.WordAtom
import errors.internal.CompilerError
import messages.Message
import components.semantic_analysis.semantic_model.values.Operator.Kind as OperatorKind

class OperatorDefinition(private val operator: Operator, private val parameterList: ParameterList?,
						 private val body: StatementSection?, private var returnType: TypeElement?):
	Element(operator.start, body?.end ?: returnType?.end ?: parameterList?.end ?: operator.end) {
	lateinit var parent: OperatorSection

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.ABSTRACT, WordAtom.MUTATING, WordAtom.NATIVE, WordAtom.OVERRIDING)
	}

	override fun concretize(linter: Linter, scope: MutableScope): FunctionImplementation {
		parent.validate(linter, ALLOWED_MODIFIER_TYPES)
		val surroundingTypeDefinition = scope.getSurroundingDefinition()
			?: throw CompilerError("Operator expected surrounding type definition.")
		val isAbstract = parent.containsModifier(WordAtom.ABSTRACT)
		val isMutating = parent.containsModifier(WordAtom.MUTATING)
		val isNative = parent.containsModifier(WordAtom.NATIVE)
		val isOverriding = parent.containsModifier(WordAtom.OVERRIDING)
		val operatorScope = BlockScope(scope)
		if(parameterList?.containsGenericParameterList == true) {
			if(operator is IndexOperator) {
				linter.addMessage(parameterList, "Generic parameters for the index operator " +
					"are received in the index parameter list instead.", Message.Type.WARNING)
			} else {
				linter.addMessage(parameterList,
					"Operators (except for the index operator) can not be generic.", Message.Type.WARNING)
			}
		}
		var parameters = parameterList?.concretizeParameters(linter, operatorScope) ?: listOf()
		val body = body?.concretize(linter, operatorScope)
		val returnType = returnType?.concretize(linter, operatorScope)
		val kind = if(operator is IndexOperator) {
			if(parameters.isEmpty())
				OperatorKind.BRACKETS_GET
			else
				OperatorKind.BRACKETS_SET
		} else operator.getKind()
		val genericParameters = (operator as? IndexOperator)?.concretizeGenerics(linter, operatorScope) ?: listOf()
		if(operator is IndexOperator)
			parameters = operator.concretizeIndices(linter, operatorScope) + parameters
		val implementation = FunctionImplementation(this, surroundingTypeDefinition, operatorScope,
			genericParameters, parameters, body, returnType, isAbstract, isMutating, isNative, isOverriding)
		scope.declareOperator(linter, kind, implementation)
		return implementation
	}

	override fun toString(): String {
		val string = StringBuilder()
		string.append("OperatorDefinition [ ")
		string.append(operator)
		if(parameterList != null)
			string.append(" ")
				.append(parameterList)
		string.append(": ")
			.append(returnType ?: "void")
			.append(" ] { ")
			.append(body)
			.append(" }")
		return string.toString()
	}
}
