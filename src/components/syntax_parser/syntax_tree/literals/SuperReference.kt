package components.syntax_parser.syntax_tree.literals

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.tokenizer.Word
import source_structure.Position
import components.semantic_model.values.SuperReference as SemanticSuperReferenceModel

class SuperReference(word: Word, private val specifier: ObjectType?, end: Position): ValueSyntaxTreeNode(word.start, end) {

	override fun toSemanticModel(scope: MutableScope): SemanticSuperReferenceModel {
		return SemanticSuperReferenceModel(this, scope, specifier?.toSemanticObjectTypeModel(scope))
	}

	override fun toString(): String {
		var stringRepresentation = "Super"
		if(specifier != null)
			stringRepresentation += " [ $specifier ]"
		return stringRepresentation
	}
}
