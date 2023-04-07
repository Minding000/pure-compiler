package util

import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.types.OrUnionType
import components.semantic_analysis.semantic_model.types.Type

fun List<Type>.combine(context: Unit): Type {
	return OrUnionType(context.source, context.scope, this).simplified()
}
