package scopes

class GlobalScope(parentScope: GlobalScope?): Scope(parentScope) {
	//TODO split Identifiers into IdentifierDefinition and IdentifierReference hold array of loose identifier references.
	//TODO member identifiers should include class (Bob.walk)
}