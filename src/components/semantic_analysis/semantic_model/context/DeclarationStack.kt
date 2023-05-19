package components.semantic_analysis.semantic_model.context

import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import components.semantic_analysis.semantic_model.definitions.TypeAlias
import logger.Logger
import logger.issues.definition.CircularTypeAlias
import logger.issues.initialization.CircularAssignment
import java.util.*

class DeclarationStack(private val logger: Logger) {
	private val typeAliases = LinkedList<TypeAlias>()
	private val propertyDeclarations = LinkedList<PropertyDeclaration>()

	fun push(typeAlias: TypeAlias): Boolean {
		if(typeAliases.contains(typeAlias)) {
			var isPartOfCircularAssignment = false
			for(existingTypeAlias in typeAliases) {
				if(!isPartOfCircularAssignment) {
					if(existingTypeAlias == typeAlias)
						isPartOfCircularAssignment = true
					else
						continue
				}
				logger.add(CircularTypeAlias(existingTypeAlias))
			}
			return false
		}
		typeAliases.add(typeAlias)
		return true
	}

	fun pop(typeAlias: TypeAlias) {
		typeAliases.remove(typeAlias)
	}

	fun push(propertyDeclaration: PropertyDeclaration): Boolean {
		if(propertyDeclarations.contains(propertyDeclaration)) {
			var isPartOfCircularAssignment = false
			for(existingPropertyDeclaration in propertyDeclarations) {
				if(!isPartOfCircularAssignment) {
					if(existingPropertyDeclaration == propertyDeclaration)
						isPartOfCircularAssignment = true
					else
						continue
				}
				logger.add(CircularAssignment(existingPropertyDeclaration))
			}
			return false
		}
		propertyDeclarations.add(propertyDeclaration)
		return true
	}

	fun pop(propertyDeclaration: PropertyDeclaration) {
		propertyDeclarations.remove(propertyDeclaration)
	}
}
