package util

import components.syntax_parser.element_generator.ElementGenerator
import components.syntax_parser.syntax_tree.general.Program

class ParseResult(val elementGenerator: ElementGenerator, val program: Program): LogResult(elementGenerator.logger)
