package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.literals.Type
import linter.scopes.BlockScope
import parsing.ast.definitions.OperatorDefinition as ASTOperatorDefinition

class IndexOperatorDefinition(source: ASTOperatorDefinition, scope: BlockScope,
							  val indices: List<Parameter>, parameters: List<Parameter>, body: Unit?,
							  returnType: Type?):
	OperatorDefinition(source, "[]", scope, parameters, body, returnType) {

	init {
		units.addAll(indices)
	}

	override fun toString(): String {
		return "[${indices.joinToString { index -> index.type.toString() }}]($variation)"
	}
}