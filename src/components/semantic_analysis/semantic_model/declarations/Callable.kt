package components.semantic_analysis.semantic_model.declarations

import java.util.*

interface Callable {
	val propertiesRequiredToBeInitialized: LinkedList<PropertyDeclaration>
	val propertiesBeingInitialized: LinkedList<PropertyDeclaration>
}
