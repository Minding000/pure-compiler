package components.code_generation.llvm.models.declarations

import components.code_generation.llvm.models.values.Value
import components.semantic_model.declarations.LocalVariableDeclaration

class LocalValueDeclaration(override val model: LocalVariableDeclaration, value: Value?): ValueDeclaration(model, value)
