package components.semantic_model.declarations

import components.code_generation.llvm.models.declarations.GlobalValueDeclaration
import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode

class GlobalValueDeclaration(source: SyntaxTreeNode, scope: MutableScope, name: String, type: Type? = null, value: Value? = null):
	ValueDeclaration(source, scope, name, type, value, true) {

	override fun requiresFileRunner(): Boolean {
		return providedType !is StaticType
	}

	override fun toUnit(): GlobalValueDeclaration {
		val unit = GlobalValueDeclaration(this, value?.toUnit())
		this.unit = unit
		return unit
	}
}
