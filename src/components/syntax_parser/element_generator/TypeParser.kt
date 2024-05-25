package components.syntax_parser.element_generator

import components.syntax_parser.syntax_tree.definitions.FunctionType
import components.syntax_parser.syntax_tree.definitions.ParameterTypeList
import components.syntax_parser.syntax_tree.definitions.TypeParameter
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.*
import components.tokenizer.Word
import components.tokenizer.WordAtom
import components.tokenizer.WordDescriptor
import components.tokenizer.WordType
import source_structure.Position
import java.util.*

class TypeParser(private val syntaxTreeGenerator: SyntaxTreeGenerator): Generator() {
	override val currentWord: Word?
		get() = syntaxTreeGenerator.currentWord
	override val nextWord: Word?
		get() = syntaxTreeGenerator.nextWord
	override var parseForeignLanguageLiteralNext: Boolean
		get() = syntaxTreeGenerator.parseForeignLanguageLiteralNext
		set(value) {
			syntaxTreeGenerator.parseForeignLanguageLiteralNext = value
		}

	private val literalParser
		get() = syntaxTreeGenerator.literalParser

	override fun getCurrentPosition(): Position = syntaxTreeGenerator.getCurrentPosition()

	override fun consume(type: WordDescriptor): Word {
		return syntaxTreeGenerator.consume(type)
	}

	private fun parseIdentifier(): Identifier {
		return literalParser.parseIdentifier()
	}

	/**
	 * Type:
	 *   [...]<UnionType>[?]
	 */
	fun parseType(optionalAllowed: Boolean = true): TypeSyntaxTreeNode {
		var hasDynamicQuantity = false
		if(currentWord?.type == WordAtom.SPREAD) {
			consume(WordAtom.SPREAD)
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
	fun parseUnionType(): TypeSyntaxTreeNode {
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
	private fun parseTypeAtom(): TypeSyntaxTreeNode {
		if(currentWord?.type == WordAtom.OPENING_PARENTHESIS || WordType.LAMBDA_FUNCTION.includes(currentWord?.type))
			return parseFunctionType()
		return parseObjectType()
	}

	/**
	 * ObjectType:
	 *   [specific ][<TypeList>]<Identifier>
	 *   [specific ]<ObjectType>.[<TypeList>]<Identifier>
	 */
	fun parseObjectType(allowTypeList: Boolean = true): ObjectType {
		val isSpecific = if(currentWord?.type == WordAtom.SPECIFIC) {
			consume(WordAtom.SPECIFIC)
			true
		} else false
		var objectType: ObjectType? = null
		while(true) {
			val typeList = if(allowTypeList)
				parseOptionalTypeList()
			else null
			val identifier = parseIdentifier()
			objectType = ObjectType(isSpecific, objectType, typeList, identifier)
			if(currentWord?.type != WordAtom.DOT)
				return objectType
			consume(WordAtom.DOT)
		}
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
		var returnType: TypeSyntaxTreeNode? = null
		val end = if(lambdaType.type == WordAtom.CAPPED_ARROW) {
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
		val start = consume(WordAtom.OPENING_PARENTHESIS).start
		val types = LinkedList<TypeSyntaxTreeNode>()
		types.add(parseType())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			types.add(parseType())
		}
		val end = consume(WordAtom.CLOSING_PARENTHESIS).end
		return ParameterTypeList(start, types, end)
	}

	/**
	 * TypeList:
	 *   <<TypeParameter>[, <TypeParameter>]...>
	 */
	fun parseTypeList(): TypeList {
		val types = LinkedList<TypeSyntaxTreeNode>()
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
	private fun parseTypeParameter(): TypeSyntaxTreeNode {
		val type = parseType()
		if(WordType.GENERICS_MODIFIER.includes(currentWord?.type)) {
			val genericsModifier = consume(WordType.GENERICS_MODIFIER)
			return TypeParameter(type, genericsModifier)
		}
		return type
	}
}
