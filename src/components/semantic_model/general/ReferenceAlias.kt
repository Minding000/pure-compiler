package components.semantic_model.general

import components.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.ReferenceAlias as ReferenceAliasSyntaxTree

class ReferenceAlias(override val source: ReferenceAliasSyntaxTree, scope: Scope, val originalName: String, val localName: String):
	SemanticModel(source, scope)
