package components.code_generation.llvm.context

import errors.internal.CompilerError

class IdentityMap<Identifier> {
	private var nextId = 1
	val ids = HashMap<Identifier, Int>()

	companion object {
		const val NULL_ID = 0
	}

	fun register(identifier: Identifier): Int {
		return ids.getOrPut(identifier) { nextId++ }
	}

	fun getId(identifier: Identifier): Int {
		return ids[identifier] ?: throw CompilerError("Requested ID for unknown identifier '$identifier'.")
	}
}
