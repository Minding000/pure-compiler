package parsing.tokenizer

import source_structure.Position
import source_structure.Section

class Word(start: Position, end: Position, val type: WordAtom): Section(start, end) {

	fun matches(type: WordDescriptor): Boolean {
		return type.includes(this.type)
	}
}