package linter.elements.general

import compiler.targets.llvm.BuildContext
import errors.internal.CompilerError
import linter.Linter
import linter.elements.literals.Type
import linter.scopes.Scope
import org.bytedeco.javacpp.Pointer
import org.bytedeco.llvm.LLVM.LLVMBuilderRef
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import java.util.*

abstract class Unit(var type: Type? = null) {
	val units = LinkedList<Unit>()

	open fun linkTypes(linter: Linter, scope: Scope) {
		for(unit in units)
			unit.linkTypes(linter, scope)
	}

	open fun linkReferences(linter: Linter, scope: Scope) {
		for(unit in units)
			unit.linkReferences(linter, scope)
	}

	open fun validate(linter: Linter) {
		for(unit in units)
			unit.validate(linter)
	}

	fun resolveType(): Type {
		return type ?: throw CompilerError("Unit '${this.javaClass.name}' doesn't have a type.")
	}

//	abstract fun compile(context: BuildContext): Pointer?
}