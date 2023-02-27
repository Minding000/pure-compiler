package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.ReferenceAlias as ReferenceAliasSyntaxTree

class ReferenceAlias(override val source: ReferenceAliasSyntaxTree, scope: Scope, val originalTypeName: String,
					 val localTypeName: String): Unit(source, scope)
