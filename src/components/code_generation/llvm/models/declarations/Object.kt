package components.code_generation.llvm.models.declarations

import components.code_generation.llvm.models.general.Unit
import components.semantic_model.declarations.Object

class Object(override val model: Object, members: List<Unit>): TypeDeclaration(model, members)
