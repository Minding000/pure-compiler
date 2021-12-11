package source_structure

interface IdentifierSource {
	fun getValue(): String
	fun getStartString(): String
	fun getRegionString(): String
}