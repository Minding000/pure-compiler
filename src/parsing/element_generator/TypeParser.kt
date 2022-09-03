package parsing.element_generator

import parsing.ast.definitions.*
import parsing.ast.general.TypeElement
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
	 * Type:
	 *   [...]<UnionType>[?]
	 */
	fun parseType(optionalAllowed: Boolean = true): TypeElement {
		var hasDynamicQuantity = false
		if(currentWord?.type == WordAtom.SPREAD_GROUP) {
			consume(WordAtom.SPREAD_GROUP)
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
	 * 	 <ObjectType>
	 *   <ObjectType> & <ObjectType>
	 *   <ObjectType> | <ObjectType>
	 */
	fun parseUnionType(): TypeElement {
		var element = parseTypeAtom()
		while(WordType.UNION_OPERATOR.includes(currentWord?.type)) {
			val operator = consume(WordType.UNION_OPERATOR)
			element = UnionType(element, parseTypeAtom(), UnionType.Mode.bySymbol(operator.getValue()))
		}
		return element
	}

	/**
	 * TypeAtom:
	 *   <ObjectType>
	 *   <FunctionType>
	 */
	private fun parseTypeAtom(): TypeElement {
		if(currentWord?.type == WordAtom.PARENTHESES_OPEN || WordType.LAMBDA_FUNCTION.includes(currentWord?.type))
			return parseFunctionType()
		return parseObjectType()
	}

	/**
	 * ObjectType:
	 *   <TypeList><Identifier>
	 */
	private fun parseObjectType(): ObjectType {
		val typeList = parseOptionalTypeList()
		val identifier = parseIdentifier()
		return ObjectType(typeList, identifier)
	}

	/**
	 * FunctionType:
	 *   [<ParameterList>] => <Type>
	 *   [<ParameterList>] =>|
	 */
	private fun parseFunctionType(): FunctionType {
		val parameterList = if(WordType.LAMBDA_FUNCTION.includes(currentWord?.type))
			null
		else
			parseLambdaParameterList()
		val lambdaType = consume(WordType.LAMBDA_FUNCTION)
		val start = parameterList?.start ?: lambdaType.start
		var returnType: TypeElement? = null
		val end = if(lambdaType.type == WordAtom.ARROW_CAPPED) {
			lambdaType.end
		} else {
			returnType = parseType()
			returnType.end
		}
		return FunctionType(start, parameterList, returnType, end)
	}

	/**
	 * LambdaParameterList:
	 *   ([<TypeParameter>][, <TypeParameter>]...)
	 */
	private fun parseLambdaParameterList(): ParameterTypeList {
		val start = consume(WordAtom.PARENTHESES_OPEN).start
		val types = LinkedList<TypeElement>()
		types.add(parseType())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			types.add(parseType())
		}
		val end = consume(WordAtom.PARENTHESES_CLOSE).end
		return ParameterTypeList(start, types, end)
	}

	/**
	 * TypeList:
	 *   <<TypeParameter>[, <TypeParameter>]...>
	 */
	fun parseTypeList(): TypeList {
		val types = LinkedList<TypeElement>()
		val start = consume(WordType.GENERICS_START).start
		types.add(parseTypeParameter())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			types.add(parseTypeParameter())
		}
		val end = consume(WordType.GENERICS_END).end
		return TypeList(types, start, end)
	}

	/**
	 * TypeList:
	 *   [<<TypeParameter>[, <TypeParameter>]...>]
	 */
	fun parseOptionalTypeList(): TypeList? {
		if(WordType.GENERICS_START.includes(currentWord?.type))
			return parseTypeList()
		return null
	}

	/**
	 * TypeParameter:
	 *   <Type>[ <generics-modifier>]
	 */
	private fun parseTypeParameter(): TypeElement {
		val type = parseType()
		if(WordType.GENERICS_MODIFIER.includes(currentWord?.type)) {
			val genericsModifier = consume(WordType.GENERICS_MODIFIER)
			return TypeParameter(type, genericsModifier)
		}
		return type
	}
}