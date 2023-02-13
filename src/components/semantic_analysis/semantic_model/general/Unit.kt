package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.Element
import java.util.*

abstract class Unit(open val source: Element) {
	open var parent: Unit? = null
	val units = LinkedList<Unit>()
	open val isInterruptingExecution = false

	fun addUnits(vararg newUnits: Unit?) {
		for(newUnit in newUnits) {
			if(newUnit != null) {
				newUnit.parent = this
				this.units.add(newUnit)
			}
		}
	}

	fun addUnits(vararg newUnits: Collection<Unit?>) {
		for(unitCollection in newUnits) {
			for(newUnit in unitCollection)
				addUnits(newUnit)
		}
	}

	fun removeUnit(unit: Unit) {
		units.remove(unit)
		unit.parent = null
	}

	fun contains(other: Unit): Boolean {
		return source.start < other.source.start && other.source.end < source.end
	}

	fun isBefore(other: Unit): Boolean {
		return source.end < other.source.start
	}

	fun isAfter(other: Unit): Boolean {
		return other.source.end < source.start
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

	open fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		for(unit in units)
			unit.analyseDataFlow(linter, tracker)
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

	fun <T: Unit>find(predicate: (T) -> Boolean, `class`: Class<T>): T? {
		@Suppress("UNCHECKED_CAST") // Cast will always work, because Class<T> == T
		if(`class`.isAssignableFrom(this.javaClass) && predicate(this as T))
			return this
		for(unit in units)
			return unit.find(predicate, `class`) ?: continue
		return null
	}

//	abstract fun compile(context: BuildContext): Pointer?
}
