package components.code_generation.llvm.models.declarations

import components.code_generation.llvm.models.values.Value
import components.semantic_model.declarations.PropertyDeclaration

class PropertyDeclaration(override val model: PropertyDeclaration, value: Value?): ValueDeclaration(model, value)
