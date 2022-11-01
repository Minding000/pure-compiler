package components.syntax_parser.syntax_tree.definitions

import components.linting.Linter
import components.linting.semantic_model.definitions.IndexOperatorDefinition as SemanticIndexOperatorDefinitionModel
import components.linting.semantic_model.definitions.OperatorDefinition as SemanticOperatorDefinitionModel
import components.linting.semantic_model.scopes.BlockScope
import components.linting.semantic_model.scopes.MutableScope
import messages.Message
import components.syntax_parser.syntax_tree.definitions.sections.OperatorSection
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.StatementSection
import components.syntax_parser.syntax_tree.general.TypeElement
import components.tokenizer.WordAtom

class OperatorDefinition(private val operator: Operator, private val parameterList: ParameterList?,
						 private val body: StatementSection?, private var returnType: TypeElement?):
	Element(operator.start, body?.end ?: returnType?.end ?: parameterList?.end ?: operator.end) {
	lateinit var parent: OperatorSection

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE, WordAtom.OVERRIDING)
	}

	override fun concretize(linter: Linter, scope: MutableScope): SemanticOperatorDefinitionModel {
		parent.validate(linter, ALLOWED_MODIFIER_TYPES)
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
		val parameters = parameterList?.concretizeParameters(linter, operatorScope) ?: listOf()
		val operatorDefinition = if(operator is IndexOperator) {
			val genericParameters = operator.concretizeGenerics(linter, operatorScope) ?: listOf()
			val indices = operator.concretizeIndices(linter, operatorScope)
			SemanticIndexOperatorDefinitionModel(this, operatorScope, genericParameters, indices, parameters,
				body?.concretize(linter, operatorScope), returnType?.concretize(linter, operatorScope), isNative,
				isOverriding)
		} else {
			SemanticOperatorDefinitionModel(this, operator.getValue(), operatorScope, parameters,
				body?.concretize(linter, operatorScope), returnType?.concretize(linter, operatorScope), isNative,
				isOverriding)
		}
		scope.declareOperator(linter, operatorDefinition)
		return operatorDefinition
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
