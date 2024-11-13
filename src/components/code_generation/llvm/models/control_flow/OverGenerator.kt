package components.code_generation.llvm.models.control_flow

import components.code_generation.llvm.models.declarations.LocalValueDeclaration
import components.code_generation.llvm.models.general.Unit
import components.code_generation.llvm.models.values.Value
import components.semantic_model.control_flow.OverGenerator

class OverGenerator(override val model: OverGenerator, val iterable: Value, val iteratorVariableDeclaration: LocalValueDeclaration?,
					val variableDeclarations: List<LocalValueDeclaration>):
	Unit(model, listOfNotNull(iterable, iteratorVariableDeclaration, *variableDeclarations.toTypedArray()))
