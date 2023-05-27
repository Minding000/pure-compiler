package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.OptionalType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.VariableValue
import errors.internal.CompilerError
import components.syntax_parser.syntax_tree.access.InstanceAccess as InstanceAccessSyntaxTree

class InstanceAccess(override val source: InstanceAccessSyntaxTree, scope: Scope, name: String): VariableValue(source, scope, name) {

	override fun isAssignableTo(targetType: Type?): Boolean {
		if(targetType is OptionalType)
			return isAssignableTo(targetType.baseType)
		return targetType?.interfaceScope?.hasInstance(name) ?: false
	}

	override fun setInferredType(inferredType: Type?) {
		super.setInferredType(inferredType)
		type?.let { type ->
			val definition = type.interfaceScope.resolveValue(this)
				?: throw CompilerError(source, "Inferred type doesn't contain instance value.")
			definition.usages.add(this)
			this.definition = definition
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		staticValue = definition?.value
	}
}
