package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.declarations.FunctionImplementation
import components.semantic_model.scopes.BlockScope
import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.OperatorSection
import components.syntax_parser.syntax_tree.general.StatementSection
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.tokenizer.WordAtom
import logger.issues.declaration.GenericOperator
import logger.issues.declaration.TypeParametersOutsideOfIndexParameterList
import components.semantic_model.values.Operator.Kind as OperatorKind

class OperatorDefinition(private val operator: Operator, private val parameterList: ParameterList?, private val body: StatementSection?,
						 private var returnType: TypeSyntaxTreeNode?):
	SyntaxTreeNode(operator.start, body?.end ?: returnType?.end ?: parameterList?.end ?: operator.end) {
	lateinit var parent: OperatorSection

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.ABSTRACT, WordAtom.MUTATING, WordAtom.NATIVE, WordAtom.OVERRIDING)
	}

	override fun toSemanticModel(scope: MutableScope): FunctionImplementation {
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
		var parameters = parameterList?.getSemanticParameterModels(operatorScope) ?: emptyList()
		val body = body?.toSemanticModel(operatorScope)
		val returnType = returnType?.toSemanticModel(operatorScope)
		val localTypeParameters = (operator as? IndexOperator)?.getSemanticGenericParameterModels(operatorScope) ?: emptyList()
		if(operator is IndexOperator)
			parameters = operator.getSemanticIndexParameterModels(operatorScope) + parameters
		return FunctionImplementation(this, operatorScope, localTypeParameters, parameters, body, returnType, isAbstract,
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
