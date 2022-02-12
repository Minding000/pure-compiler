package parsing.element_generator

import parsing.ast.*
import parsing.ast.operations.*
import parsing.ast.definitions.*
import parsing.ast.literals.*
import parsing.tokenizer.*
import java.util.*

class TypeParser(private val elementGenerator: ElementGenerator): Generator() {
	override var currentWord: Word?
		get() = elementGenerator.currentWord
		set(value) { elementGenerator.currentWord = value }
	override var nextWord: Word?
		get() = elementGenerator.nextWord
		set(value) { elementGenerator.nextWord = value }
	override var parseForeignLanguageLiteralNext: Boolean
		get() = elementGenerator.parseForeignLanguageLiteralNext
		set(value) { elementGenerator.parseForeignLanguageLiteralNext = value }

	private val literalParser
		get() = elementGenerator.literalParser

	override fun consume(type: WordDescriptor): Word {
		return elementGenerator.consume(type)
	}

	private fun parseIdentifier(): Identifier {
		return literalParser.parseIdentifier()
	}

	/**
	 * OptionallyTypedIdentifier:
	 *   <Identifier>
	 *   <TypedIdentifier>
	 */
	fun parseOptionallyTypedIdentifier(): Element {
		return if(nextWord?.type == WordAtom.COLON)
			parseTypedIdentifier()
		else
			parseIdentifier()
	}

	/**
	 * TypedIdentifier:
	 *   <Identifier>: <Type>
	 */
	fun parseTypedIdentifier(): TypedIdentifier {
		val identifier = parseIdentifier()
		consume(WordAtom.COLON)
		val type = parseType()
		return TypedIdentifier(identifier, type)
	}

	/**
	 * Type:
	 *   [...]<UnionType>...[?]
	 */
	fun parseType(optionalAllowed: Boolean = true): Type {
		var hasDynamicQuantity = false
		if(currentWord?.type == WordAtom.TRIPLE_DOT) {
			consume(WordAtom.TRIPLE_DOT)
			hasDynamicQuantity = true
		}
		val typeList = parseTypeList()
		val baseType = parseUnionType()
		var isOptional = false
		if(optionalAllowed && currentWord?.type == WordAtom.QUESTION_MARK) {
			consume(WordAtom.QUESTION_MARK)
			isOptional = true
		}
		return Type(baseType, hasDynamicQuantity, isOptional, typeList)
	}

	/**
	 * UnionType:
	 * 	 <SimpleType>
	 *   <SimpleType> & <SimpleType>
	 *   <SimpleType> | <SimpleType>
	 */
	private fun parseUnionType(): Element {
		var element: Element = parseSimpleType()
		while(WordType.UNION_OPERATOR.includes(currentWord?.type)) {
			val operator = consume(WordType.UNION_OPERATOR)
			element = BinaryOperator(element, parseSimpleType(), operator.getValue())
		}
		return element
	}

	/**
	 * SimpleType:
	 *   <TypeList><Identifier>
	 */
	fun parseSimpleType(): SimpleType {
		val typeList = parseTypeList()
		val identifier = parseIdentifier()
		return SimpleType(typeList, identifier)
	}

	/**
	 * GenericModifier:
	 *   <generic-modifier>
	 */
	private fun parseGenericModifier(): GenericModifier {
		return GenericModifier(consume(WordType.GENERICS_MODIFIER))
	}

	/**
	 * TypeList:
	 *   [<<TypeParameter>[, <TypeParameter>]...>]
	 */
	private fun parseTypeList(): TypeList? {
		if(WordType.GENERICS_START.includes(currentWord?.type)) {
			val types = LinkedList<TypeParameter>()
			val start = consume(WordType.GENERICS_START).start
			types.add(parseTypeParameter())
			while(currentWord?.type == WordAtom.COMMA) {
				consume(WordAtom.COMMA)
				types.add(parseTypeParameter())
			}
			val end = consume(WordType.GENERICS_END).end
			return TypeList(types, start, end)
		}
		return null
	}

	/**
	 * TypeParameter:
	 *   <Type>[ <GenericModifier>]
	 */
	private fun parseTypeParameter(): TypeParameter {
		val type = parseType()
		var genericModifier: GenericModifier? = null
		if(WordType.GENERICS_MODIFIER.includes(currentWord?.type)) {
			genericModifier = parseGenericModifier()
		}
		return TypeParameter(type, genericModifier)
	}
}