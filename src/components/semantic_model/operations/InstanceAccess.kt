package components.semantic_model.operations

import components.semantic_model.context.VariableTracker
import components.semantic_model.scopes.Scope
import components.semantic_model.types.OptionalType
import components.semantic_model.types.Type
import components.semantic_model.values.VariableValue
import errors.internal.CompilerError
import components.syntax_parser.syntax_tree.access.InstanceAccess as InstanceAccessSyntaxTree

class InstanceAccess(override val source: InstanceAccessSyntaxTree, scope: Scope, name: String): VariableValue(source, scope, name) {

	override fun determineTypes() {
		// Do nothing.
		// Type is always inferred.
	}

	override fun setInferredType(inferredType: Type?) {
		super.setInferredType(inferredType)
		val type = type ?: return
		val (declaration) = type.interfaceScope.getValueDeclaration(this)
		if(declaration == null)
			throw CompilerError(source, "Inferred type doesn't contain instance value.")
		declaration.usages.add(this)
		this.declaration = declaration
	}

	override fun computeValue(tracker: VariableTracker) {
		super.computeValue(tracker)
		staticValue = declaration?.value
	}

	override fun isAssignableTo(targetType: Type?): Boolean {
		if(targetType is OptionalType)
			return isAssignableTo(targetType.baseType)
		return targetType?.interfaceScope?.hasInstance(name) ?: false
	}
}
