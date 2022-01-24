package parsing.ast.definitions

import parsing.ast.Element
import util.indent

class ComputedProperty(val identifier: TypedIdentifier, val getExpression: Element?, val setExpression: Element?): Element(identifier.start, setExpression?.end ?: getExpression?.end ?: identifier.end) {

	override fun toString(): String {
		var body = identifier.toString()
		if(getExpression != null)
			body += "\nget $getExpression"
		if(setExpression != null)
			body += "\nset $setExpression"
		return "ComputedProperty {${"\n$body".indent()}\n}"
	}
}