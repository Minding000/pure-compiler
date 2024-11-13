package components.code_generation.llvm.models.declarations

import components.code_generation.llvm.models.general.Unit
import components.semantic_model.declarations.Enum

class Enum(override val model: Enum, members: List<Unit>, staticValueDeclaration: ValueDeclaration):
	TypeDeclaration(model, members, staticValueDeclaration)
