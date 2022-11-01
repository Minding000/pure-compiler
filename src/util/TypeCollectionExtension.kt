package util

import components.linting.semantic_model.types.AndUnionType
import components.linting.semantic_model.types.ObjectType
import components.linting.semantic_model.types.OrUnionType
import components.linting.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element
import java.util.*

fun Collection<Type>.getCommonType(source: Element): Type? {
	//TODO make the following examples tests
	//TODO also test inference when parameter is union

	// JungleCat         	<-- select
	// Cat               	<-- compare -> mismatch -> super class -> match -> continue with Cat & PresentInJungle
	//  - Monkey         	<-- compare -> mismatch -> super class -> match -> continue with Animal & TreeClimbing
	//  - Horse | Desk   	<-- compare -> mismatch -> super class -> match -> continue with Animal | Desk

	//  - Bacteria       	<-- compare -> mismatch -> try each union part -> continue with LivingThing
	//  - TreeClimbingRobot <-- compare -> mismatch -> try each union part -> continue with TreeClimbing
	var commonType: Type? = null
	for(type in this) {
		while(commonType?.accepts(type) != true) {
			when(commonType) {
				null -> commonType = type
				is AndUnionType -> {
					val applicableAndUnionTypes = LinkedList<Type>()
					for(andUnionType in commonType.types) {
						if(andUnionType.accepts(type))
							applicableAndUnionTypes.add(andUnionType)
					}
					if(applicableAndUnionTypes.isEmpty())
						return null
					commonType = if(applicableAndUnionTypes.size == 1)
						applicableAndUnionTypes.firstOrNull()
					else
						AndUnionType(source, applicableAndUnionTypes)
				}
				is OrUnionType -> {
					return null
				}
				is ObjectType -> {
					commonType = commonType.definition?.superType
				}
			}
		}
	}
	return commonType
}
