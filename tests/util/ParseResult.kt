package util

import components.syntax_parser.element_generator.SyntaxTreeGenerator
import components.syntax_parser.syntax_tree.general.Program

class ParseResult(val syntaxTreeGenerator: SyntaxTreeGenerator, val program: Program): LogResult(syntaxTreeGenerator.project.context.logger)
