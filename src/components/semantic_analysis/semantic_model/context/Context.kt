package components.semantic_analysis.semantic_model.context

import components.semantic_analysis.semantic_model.control_flow.LoopStatement
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import logger.Issue
import logger.Logger
import java.util.*

class Context {
	val logger = Logger("compiler")
	val declarationStack = DeclarationStack(logger)
	val surroundingLoops = LinkedList<LoopStatement>()

	fun addIssue(issue: Issue) = logger.add(issue)

	fun registerWrite(variableDeclaration: Value) {
		if(variableDeclaration !is VariableValue)
			return
		val declaration = variableDeclaration.definition ?: return
		for(surroundingLoop in surroundingLoops) {
			surroundingLoop.mutatedVariables.add(declaration)
		}
	}
}
