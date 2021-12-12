package parsing.tokenizer

import source_structure.Position
import source_structure.Section

class Word(start: Position, end: Position, val type: WordType): Section(start, end)