package linting.semantic_model.values

import parsing.syntax_tree.definitions.Instance as InstanceSyntaxTree

open class Instance(override val source: InstanceSyntaxTree, value: VariableValue):
	VariableValueDeclaration(source, value.name, null, null, true)