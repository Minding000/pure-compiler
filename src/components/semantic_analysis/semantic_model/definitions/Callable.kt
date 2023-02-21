package components.semantic_analysis.semantic_model.definitions

import java.util.*

interface Callable {
	val propertiesRequiredToBeInitialized: LinkedList<PropertyDeclaration>
	val propertiesBeingInitialized: LinkedList<PropertyDeclaration>
}
