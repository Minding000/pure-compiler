package util

import components.semantic_model.general.SemanticModel
import components.semantic_model.types.OrUnionType
import components.semantic_model.types.Type

fun List<Type>.combine(context: SemanticModel): Type {
	return OrUnionType(context.source, context.scope, this).simplified()
}
