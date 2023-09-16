package logger.issues.resolution

import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ConversionAmbiguity(source: SyntaxTreeNode, sourceType: Type, targetType: Type, possibleConversions: List<InitializerDefinition>):
	Issue(Severity.ERROR, source) {
	override val text = "Conversion from '$sourceType' to '$targetType' needs to be explicit," +
		" because there are multiple possible conversions:" + possibleConversions.joinToString("") { "\n - ${it.parentTypeDeclaration.name}" }
	override val description = "The conversion is possible but ambiguous, because there are multiple possible conversions."
	override val suggestion = "Make the conversion explicit by calling the converting initializer of the target type."
}
