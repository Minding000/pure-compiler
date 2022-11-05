package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.Element
import java.util.*

abstract class Unit(open val source: Element) {
	val units = LinkedList<Unit>()
	open val isInterruptingExecution = false

	fun addUnits(vararg newUnits: Unit?) {
		for(newUnit in newUnits) {
			if(newUnit != null)
				this.units.add(newUnit)
		}
	}

	fun addUnits(vararg newUnits: Collection<Unit>) {
		for(unitCollection in newUnits)
			units.addAll(unitCollection)
	}

	fun addUnitsOptional(vararg newUnits: Collection<Unit?>) {
		for(unitCollection in newUnits) {
			for(newUnit in unitCollection)
				addUnits(newUnit)
		}
	}

	fun removeUnit(unit: Unit) {
		units.remove(unit)
	}

	open fun linkTypes(linter: Linter, scope: Scope) {
		for(unit in units)
			unit.linkTypes(linter, scope)
	}

	open fun resolveGenerics(linter: Linter) {
		for(unit in units)
			unit.resolveGenerics(linter)
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
		val unitIterator = units.iterator()
		for(unit in unitIterator)
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
