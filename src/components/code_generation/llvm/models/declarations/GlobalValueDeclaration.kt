package components.code_generation.llvm.models.declarations

import components.code_generation.llvm.models.values.Value
import components.semantic_model.declarations.GlobalValueDeclaration
import components.semantic_model.types.StaticType

class GlobalValueDeclaration(override val model: GlobalValueDeclaration, value: Value?): ValueDeclaration(model, value) {

	override fun requiresFileRunner(): Boolean {
		return model.providedType !is StaticType
	}
}
