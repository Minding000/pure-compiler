package components.code_generation.llvm.models.declarations

import components.code_generation.llvm.models.values.Value
import components.semantic_model.declarations.GlobalValueDeclaration

class GlobalValueDeclaration(override val model: GlobalValueDeclaration, value: Value?): ValueDeclaration(model, value)
