package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.control_flow.LoopStatement as LoopStatementSyntaxTree

class LoopStatement(override val source: LoopStatementSyntaxTree, val scope: BlockScope, val generator: Unit?,
					val body: ErrorHandlingContext): Unit(source) {

	init {
		addUnits(generator, body)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		//TODO implement infinite loop check
//		if(generator is WhileGenerator) {
//			val condition = generator.condition.staticValue
//			if(condition is BooleanLiteral && condition.value) {
//				var mightGetInterrupted = false
//				for(statement in body.mainBlock.statements) {
//					//TODO this is not reliable as some statement might contain a interrupting statement, but not be interrupting for certain itself
//					if(statement.isInterruptingExecution) {
//						mightGetInterrupted = true
//						break
//					}
//				}
//				if(!mightGetInterrupted) {
//					handleBlockCheck@for(handleBlock in body.handleBlocks) {
//						for(statement in handleBlock.block.statements) {
//							if(statement.isInterruptingExecution) {
//								mightGetInterrupted = true
//								break@handleBlockCheck
//							}
//						}
//					}
//				}
//				if(!mightGetInterrupted) {
//					if(body.alwaysBlock != null) {
//						for(statement in body.alwaysBlock.statements) {
//							if(statement.isInterruptingExecution) {
//								mightGetInterrupted = true
//								break
//							}
//						}
//					}
//				}
//				if(!mightGetInterrupted)
//					isInterruptingExecution = true
//			}
//		}
	}
}
