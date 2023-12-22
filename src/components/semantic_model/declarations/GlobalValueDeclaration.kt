package components.semantic_model.declarations

import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.Type
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode

class GlobalValueDeclaration(source: SyntaxTreeNode, scope: MutableScope, name: String, type: Type? = null, value: Value? = null):
	ValueDeclaration(source, scope, name, type, value, true)
