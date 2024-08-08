package components.semantic_model.declarations

import java.util.*

interface Callable {
	var hasDataFlowBeenAnalysed: Boolean
	val propertiesRequiredToBeInitialized: LinkedList<PropertyDeclaration>
	val propertiesBeingInitialized: LinkedList<PropertyDeclaration>

	fun analyseDataFlow()
	// see: https://youtrack.jetbrains.com/issue/KT-31420
	@Suppress("INAPPLICABLE_JVM_NAME")
	@JvmName("getPropertiesRequiredToBeInitialized_function")
	fun getPropertiesRequiredToBeInitialized(): LinkedList<PropertyDeclaration> {
		if(!hasDataFlowBeenAnalysed)
			analyseDataFlow()
		return propertiesRequiredToBeInitialized
	}
	// see: https://youtrack.jetbrains.com/issue/KT-31420
	@Suppress("INAPPLICABLE_JVM_NAME")
	@JvmName("getPropertiesBeingInitialized_function")
	fun getPropertiesBeingInitialized(): LinkedList<PropertyDeclaration> {
		if(!hasDataFlowBeenAnalysed)
			analyseDataFlow()
		return propertiesBeingInitialized
	}
}
