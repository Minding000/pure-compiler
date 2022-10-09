package linting.semantic_model.values

import parsing.syntax_tree.definitions.Instance as InstanceSyntaxTree

open class Instance(override val source: InstanceSyntaxTree, name: String):
	VariableValueDeclaration(source, name)
