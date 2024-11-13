package components.code_generation.llvm.models.declarations

import components.semantic_model.declarations.GenericTypeDeclaration

class GenericTypeDeclaration(override val model: GenericTypeDeclaration, staticValueDeclaration: ValueDeclaration):
	TypeDeclaration(model, emptyList(), staticValueDeclaration)
