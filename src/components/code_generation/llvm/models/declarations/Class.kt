package components.code_generation.llvm.models.declarations

import components.code_generation.llvm.models.general.Unit
import components.semantic_model.declarations.Class

class Class(override val model: Class, members: List<Unit>, staticValueDeclaration: ValueDeclaration):
	TypeDeclaration(model, members, staticValueDeclaration)
