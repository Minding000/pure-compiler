package components.semantic_model.values

import components.semantic_model.context.VariableTracker
import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier

class LocalVariableDeclaration(source: SyntaxTreeNode, scope: MutableScope, name: String, type: Type? = null, value: Value? = null,
							   isConstant: Boolean = true, isMutable: Boolean = false):
	ValueDeclaration(source, scope, name, type, value, isConstant, isMutable) {

	constructor(source: Identifier, scope: MutableScope, type: Type? = null): this(source, scope, source.getValue(), type)

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		tracker.declare(this, type is StaticType)
	}
}
