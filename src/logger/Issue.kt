package logger

import source_structure.Section

abstract class Issue(val severity: Severity, val source: Section? = null) {
	open val isInternal = false
	abstract val text: String
	abstract val description: String
	open val example: String? = null
	open val suggestion: String? = null
	open val suggestedAction: Action? = null

	override fun toString(): String {
		var stringRepresentation = ""
		if(source != null)
			stringRepresentation += "${source.getStartString()}: "
		stringRepresentation += text
		return stringRepresentation
	}
}
