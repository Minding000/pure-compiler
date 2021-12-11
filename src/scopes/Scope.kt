package scopes

import elements.identifier.*
import errors.user.ResolveError
import java.util.*

open class Scope(open val parentScope: Scope?) {
	val childScopes = LinkedList<Scope>()
	private val identifiers = HashMap<String, Identifier>()

	fun getVariableIdentifierRecursive(reference: IdentifierReference<VariableIdentifier>): VariableIdentifier {
		return getIdentifierOrNull(reference.getValue()) as? VariableIdentifier ?: throw ResolveError(reference)
	}

	fun getClassIdentifierRecursive(reference: IdentifierReference<ClassIdentifier>): ClassIdentifier {
		return getIdentifierOrNull(reference.getValue()) as? ClassIdentifier ?: throw ResolveError(reference)
	}

	fun getIdentifierRecursive(reference: IdentifierReference<Identifier>): Identifier {
		return getIdentifierOrNull(reference.getValue()) ?: throw ResolveError(reference)
	}

	private fun getIdentifierOrNullRecursive(name: String): Identifier? {
		var identifier = getIdentifierOrNull(name)
		if(identifier == null)
			identifier = parentScope?.getIdentifierOrNullRecursive(name)
		return identifier
	}

	fun getIdentifierOrNull(name: String): Identifier? {
		return identifiers[name]
	}

	open fun declareIdentifier(identifier: Identifier) {
		val previousDeclaration = identifiers[identifier.name]
		if(previousDeclaration != null)
			throw ResolveError("Cannot redeclare identifier '${identifier.name}' at ${identifier.serializeDeclarationPosition()} previously declared at ${previousDeclaration.serializeDeclarationPosition()}.")
		identifiers[identifier.name] = identifier
	}
}