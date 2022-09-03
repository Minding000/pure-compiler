package linter.elements.general

import linter.Linter
import linter.scopes.MutableScope
import linter.scopes.Scope
import java.util.*

abstract class Unit {
	val units = LinkedList<Unit>()

	open fun linkTypes(linter: Linter, scope: Scope) {
		for(unit in units)
			unit.linkTypes(linter, scope)
	}

	open fun linkPropertyParameters(linter: Linter, scope: MutableScope) {
		for(unit in units)
			unit.linkPropertyParameters(linter, scope)
	}

	open fun linkValues(linter: Linter, scope: Scope) {
		for(unit in units)
			unit.linkValues(linter, scope)
	}

	open fun validate(linter: Linter) {
		for(unit in units)
			unit.validate(linter)
	}

	inline fun <reified T: Unit>find(noinline predicate: (T) -> Boolean): T? {
		if(this is T && predicate(this))
			return this
		for(unit in units)
			return unit.find(predicate, T::class.java) ?: continue
		return null
	}

	fun <T: Unit>find(predicate: (T) -> Boolean, clazz: Class<T>): T? {
		@Suppress("UNCHECKED_CAST") // Cast will always work, because Class<T> == T
		if(clazz.isAssignableFrom(this.javaClass) && predicate(this as T))
			return this
		for(unit in units)
			return unit.find(predicate, clazz) ?: continue
		return null
	}

//	abstract fun compile(context: BuildContext): Pointer?
}