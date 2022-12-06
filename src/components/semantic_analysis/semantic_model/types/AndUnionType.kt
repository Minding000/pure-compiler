package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.semantic_model.definitions.OperatorDefinition
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.values.MemberDeclaration
import components.syntax_parser.syntax_tree.general.Element
import java.util.*

class AndUnionType(override val source: Element, val types: List<Type>): Type(source) {

	init {
		addUnits(types)
		for(type in types)
			type.scope.subscribe(this)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): AndUnionType {
		val specificTypes = LinkedList<Type>()
		for(type in types)
			specificTypes.add(type.withTypeSubstitutions(typeSubstitutions))
		return AndUnionType(source, specificTypes)
	}

	override fun onNewType(type: TypeDefinition) {
		this.scope.addType(type)
	}

	override fun onNewValue(value: MemberDeclaration) {
		this.scope.addValue(value)
	}

	override fun onNewOperator(operator: OperatorDefinition) {
		this.scope.addOperator(operator)
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		val sourceType = resolveTypeAlias(unresolvedSourceType)
		for(type in types)
			if(!type.accepts(sourceType))
				return false
		return true
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = resolveTypeAlias(unresolvedTargetType)
		for(type in types)
			if(type.isAssignableTo(targetType))
				return true
		return false
	}

	override fun equals(other: Any?): Boolean {
		if(other !is AndUnionType)
			return false
		if(types.size != other.types.size)
			return false
		for(type in types)
			if(!other.types.contains(type))
				return false
		return true
	}

	override fun hashCode(): Int {
		return types.hashCode()
	}

	override fun toString(): String {
		return types.joinToString(" & ") //TODO use parentheses when the output would be ambiguous
	}
}
