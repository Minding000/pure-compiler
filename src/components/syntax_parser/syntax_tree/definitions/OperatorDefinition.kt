package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.OperatorSection
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.StatementSection
import components.syntax_parser.syntax_tree.general.TypeElement
import components.tokenizer.WordAtom
import logger.issues.definition.GenericOperator
import logger.issues.definition.TypeParametersOutsideOfIndexParameterList
import components.semantic_analysis.semantic_model.values.Operator.Kind as OperatorKind

class OperatorDefinition(private val operator: Operator, private val parameterList: ParameterList?, private val body: StatementSection?,
						 private var returnType: TypeElement?):
	Element(operator.start, body?.end ?: returnType?.end ?: parameterList?.end ?: operator.end) {
	lateinit var parent: OperatorSection

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.ABSTRACT, WordAtom.MUTATING, WordAtom.NATIVE, WordAtom.OVERRIDING)
	}

	override fun concretize(scope: MutableScope): FunctionImplementation {
		parent.validate(ALLOWED_MODIFIER_TYPES)
		val isAbstract = parent.containsModifier(WordAtom.ABSTRACT)
		val isMutating = parent.containsModifier(WordAtom.MUTATING)
		val isNative = parent.containsModifier(WordAtom.NATIVE)
		val isOverriding = parent.containsModifier(WordAtom.OVERRIDING)
		val operatorScope = BlockScope(scope)
		if(parameterList?.containsGenericParameterList == true) {
			if(operator is IndexOperator) {
				context.addIssue(TypeParametersOutsideOfIndexParameterList(parameterList))
			} else {
				context.addIssue(GenericOperator(parameterList))
			}
		}
		var parameters = parameterList?.concretizeParameters(operatorScope) ?: listOf()
		val body = body?.concretize(operatorScope)
		val returnType = returnType?.concretize(operatorScope)
		val genericParameters = (operator as? IndexOperator)?.concretizeGenerics(operatorScope) ?: listOf()
		if(operator is IndexOperator)
			parameters = operator.concretizeIndices(operatorScope) + parameters
		return FunctionImplementation(this, operatorScope, genericParameters, parameters, body, returnType, isAbstract,
			isMutating, isNative, isOverriding)
	}

	fun getKind(): OperatorKind {
		return if(operator is IndexOperator) {
			if(parameterList?.containsParameters == true)
				OperatorKind.BRACKETS_SET
			else
				OperatorKind.BRACKETS_GET
		} else operator.getKind()
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
