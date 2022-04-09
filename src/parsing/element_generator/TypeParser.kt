package parsing.element_generator

import parsing.ast.definitions.*
import parsing.ast.general.Element
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
	 *   [...]<UnionType>[?]
	 */
	fun parseType(optionalAllowed: Boolean = true): Type {
		var hasDynamicQuantity = false
		if(currentWord?.type == WordAtom.TRIPLE_DOT) {
			consume(WordAtom.TRIPLE_DOT)
			hasDynamicQuantity = true
		}
		val baseType = parseUnionType()
		var isOptional = false
		if(optionalAllowed && currentWord?.type == WordAtom.QUESTION_MARK) {
			consume(WordAtom.QUESTION_MARK)
			isOptional = true
		}
		if(isOptional || hasDynamicQuantity)
			return QuantifiedType(baseType, hasDynamicQuantity, isOptional)
		return baseType
	}

	/**
	 * UnionType:
	 * 	 <SimpleType>
	 *   <SimpleType> & <SimpleType>
	 *   <SimpleType> | <SimpleType>
	 */
	fun parseUnionType(): Type {
		var element = parseTypeAtom()
		while(WordType.UNION_OPERATOR.includes(currentWord?.type)) {
			val operator = consume(WordType.UNION_OPERATOR)
			element = UnionType(element, parseTypeAtom(), UnionType.Mode.bySymbol(operator.getValue()))
		}
		return element
	}

	/**
	 * TypeAtom:
	 *   <SimpleType>
	 *   <LambdaFunction>
	 */
	private fun parseTypeAtom(): Type {
		if(currentWord?.type == WordAtom.PARENTHESES_OPEN || currentWord?.type == WordAtom.ARROW)
			return parseLambdaFunction()
		return parseSimpleType()
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
	 * LambdaFunction:
	 *   [<ParameterList>] => <Type>|;
	 */
	private fun parseLambdaFunction(): LambdaFunctionType {
		val parameterList = if(currentWord?.type == WordAtom.ARROW)
			null
		else
			parseLambdaParameterList()
		var start = consume(WordAtom.ARROW).start
		if(parameterList != null)
			start = parameterList.start
		var returnType: Type? = null
		val end = if(currentWord?.type == WordAtom.SEMICOLON) {
			consume(WordAtom.SEMICOLON).end
		} else {
			returnType = parseType()
			returnType.end
		}
		return LambdaFunctionType(start, parameterList, returnType, end)
	}

	/**
	 * LambdaParameterList:
	 *   ([<TypeParameter>][, <TypeParameter>]...)
	 */
	private fun parseLambdaParameterList(): LambdaParameterList {
		val start = consume(WordAtom.PARENTHESES_OPEN).start
		val types = LinkedList<Type>()
		types.add(parseType())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			types.add(parseType())
		}
		val end = consume(WordAtom.PARENTHESES_CLOSE).end
		return LambdaParameterList(start, types, end)
	}

	/**
	 * TypeList:
	 *   [<<TypeParameter>[, <TypeParameter>]...>]
	 */
	private fun parseTypeList(): TypeList? {
		if(WordType.GENERICS_START.includes(currentWord?.type)) {
			val types = LinkedList<Type>()
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
	 *   <Type>[ <generics-modifier>]
	 */
	private fun parseTypeParameter(): Type {
		val type = parseType()
		if(WordType.GENERICS_MODIFIER.includes(currentWord?.type)) {
			val genericsModifier = consume(WordType.GENERICS_MODIFIER)
			return TypeParameter(type, genericsModifier)
		}
		return type
	}
}