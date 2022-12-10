package components.syntax_parser.element_generator

import components.syntax_parser.syntax_tree.access.IndexAccess
import components.syntax_parser.syntax_tree.access.InstanceAccess
import components.syntax_parser.syntax_tree.access.MemberAccess
import components.syntax_parser.syntax_tree.control_flow.FunctionCall
import components.syntax_parser.syntax_tree.control_flow.Try
import components.syntax_parser.syntax_tree.definitions.LambdaFunctionDefinition
import components.syntax_parser.syntax_tree.definitions.Operator
import components.syntax_parser.syntax_tree.definitions.TypeSpecification
import components.syntax_parser.syntax_tree.general.ForeignLanguageExpression
import components.syntax_parser.syntax_tree.general.TypeElement
import components.syntax_parser.syntax_tree.general.ValueElement
import components.syntax_parser.syntax_tree.literals.ForeignLanguageLiteral
import components.syntax_parser.syntax_tree.literals.Identifier
import components.syntax_parser.syntax_tree.literals.SelfReference
import components.syntax_parser.syntax_tree.operations.BinaryOperator
import components.syntax_parser.syntax_tree.operations.Cast
import components.syntax_parser.syntax_tree.operations.NullCheck
import components.syntax_parser.syntax_tree.operations.UnaryOperator
import components.tokenizer.Word
import components.tokenizer.WordAtom
import components.tokenizer.WordDescriptor
import components.tokenizer.WordType
import errors.internal.CompilerError
import errors.user.UnexpectedWordError
import source_structure.Position
import java.util.*
import java.util.regex.Pattern

class ExpressionParser(private val elementGenerator: ElementGenerator): Generator() {
	private val typeParameterListCheck: Pattern = Pattern.compile("[;()\\[\\]]")
	override var currentWord: Word?
		get() = elementGenerator.currentWord
		set(value) { elementGenerator.currentWord = value }
	override var nextWord: Word?
		get() = elementGenerator.nextWord
		set(value) { elementGenerator.nextWord = value }
	override var parseForeignLanguageLiteralNext: Boolean
		get() = elementGenerator.parseForeignLanguageLiteralNext
		set(value) { elementGenerator.parseForeignLanguageLiteralNext = value }

	private val statementParser
		get() = elementGenerator.statementParser
	private val typeParser
		get() = elementGenerator.typeParser
	private val literalParser
		get() = elementGenerator.literalParser

	override fun consume(type: WordDescriptor): Word {
		return elementGenerator.consume(type)
	}

	/**
	 * Expression:
	 *   <BinaryBooleanExpression>
	 */
	fun parseExpression(): ValueElement {
		return parseBinaryBooleanExpression()
	}

	/**
	 * BinaryBooleanExpression:
	 *   <Equality>
	 *   <Equality> & <Equality>
	 *   <Equality> | <Equality>
	 */
	private fun parseBinaryBooleanExpression(): ValueElement {
		var expression: ValueElement = parseEquality()
		while(WordType.BINARY_BOOLEAN_OPERATOR.includes(currentWord?.type)) {
			val operator = parseOperator(WordType.BINARY_BOOLEAN_OPERATOR)
			expression = BinaryOperator(expression, parseEquality(), operator)
		}
		return expression
	}

	/**
	 * Equality:
	 *   <Comparison>
	 *   <Comparison> >= <Comparison>
	 *   <Comparison> <= <Comparison>
	 *   <Comparison> > <Comparison>
	 *   <Comparison> < <Comparison>
	 */
	private fun parseEquality(): ValueElement {
		var expression: ValueElement = parseComparison()
		while(WordType.EQUALITY.includes(currentWord?.type)) {
			val operator = parseOperator(WordType.EQUALITY)
			expression = BinaryOperator(expression, parseComparison(), operator)
		}
		return expression
	}

	/**
	 * Comparison:
	 *   <Addition>
	 *   <Addition> == <Addition>
	 *   <Addition> != <Addition>
	 */
	private fun parseComparison(): ValueElement {
		var expression: ValueElement = parseAddition()
		while(WordType.COMPARISON.includes(currentWord?.type)) {
			val operator = parseOperator(WordType.COMPARISON)
			expression = BinaryOperator(expression, parseAddition(), operator)
		}
		return expression
	}

	/**
	 * Addition:
	 *   <Multiplication>
	 *   <Multiplication> + <Multiplication>
	 *   <Multiplication> - <Multiplication>
	 */
	private fun parseAddition(): ValueElement {
		var expression: ValueElement = parseMultiplication()
		while(WordType.ADDITION.includes(currentWord?.type)) {
			val operator = parseOperator(WordType.ADDITION)
			expression = BinaryOperator(expression, parseMultiplication(), operator)
		}
		return expression
	}

	/**
	 * Multiplication:
	 *   <NullCoalescence>
	 *   <NullCoalescence> * <NullCoalescence>
	 *   <NullCoalescence> / <NullCoalescence>
	 */
	private fun parseMultiplication(): ValueElement {
		var expression: ValueElement = parseNullCoalescence()
		while(WordType.MULTIPLICATION.includes(currentWord?.type)) {
			val operator = parseOperator(WordType.MULTIPLICATION)
			expression = BinaryOperator(expression, parseNullCoalescence(), operator)
		}
		return expression
	}

	/**
	 * NullCoalescence:
	 *   <Cast>
	 *   <Cast> ?? <Cast>
	 */
	private fun parseNullCoalescence(): ValueElement {
		var expression: ValueElement = parseCast()
		while(currentWord?.type == WordAtom.NULL_COALESCENCE) {
			val operator = parseOperator(WordAtom.NULL_COALESCENCE)
			expression = BinaryOperator(expression, parseCast(), operator)
		}
		return expression
	}

	/**
	 * Cast:
	 *   <Try>
	 *   <Try> as <Type>
	 *   <Try> as? <Type>
	 *   <Try> as! <Type>
	 *   <Try> is [<Identifier>: ]<Type>
	 *   <Try> !is [<Identifier>: ]<Type>
	 */
	private fun parseCast(): ValueElement {
		var expression: ValueElement = parseTry()
		if(WordType.CAST.includes(currentWord?.type)) {
			val operator = consume(WordType.CAST)
			var identifier: Identifier? = null
			if(nextWord?.type == WordAtom.COLON) {
				identifier = literalParser.parseIdentifier()
				consume(WordAtom.COLON)
			}
			val type = typeParser.parseType()
			expression = Cast(expression, operator.getValue(), identifier, type)
		}
		return expression
	}

	/**
	 * Try:
	 *   <UnaryOperator>
	 *   try? <UnaryOperator>
	 *   try! <UnaryOperator>
	 */
	fun parseTry(): ValueElement {
		if(WordType.TRY.includes(currentWord?.type)) {
			val operator = consume(WordType.TRY)
			return Try(parseUnaryOperator(), operator.type == WordAtom.OPTIONAL_TRY, operator.start)
		}
		return parseUnaryOperator()
	}

	/**
	 * UnaryOperator:
	 *   <Accessor>
	 *   !<Accessor>
	 *   +<Accessor>
	 *   -<Accessor>
	 *   ...<Accessor>
	 */
	private fun parseUnaryOperator(): ValueElement {
		if(WordType.UNARY_OPERATOR.includes(currentWord?.type)) {
			val operator = parseOperator(WordType.UNARY_OPERATOR)
			return UnaryOperator(parseAccessor(), operator)
		}
		return parseAccessor()
	}

	/**
	 * Accessor:
	 *   <Primary>
	 *   <Accessor><MemberAccess>
	 *   <Accessor><FunctionCall>
	 *   <Accessor><Index>
	 */
	private fun parseAccessor(): ValueElement {
		var expression = parsePrimary()
		while(WordType.ACCESSOR.includes(currentWord?.type)) {
			expression =  if(WordType.MEMBER_ACCESSOR.includes(currentWord?.type))
				parseMemberAccess(expression)
			else if(currentWord?.type == WordAtom.OPENING_PARENTHESIS)
				parseFunctionCall(expression)
			else if(currentWord?.type == WordAtom.OPENING_BRACKET)
				parseIndex(expression)
			else
				throw CompilerError("Failed to parse accessor: '${currentWord?.type}'")
		}
		return expression
	}

	/**
	 * MemberAccess:
	 *   <Accessor>[?].[<TypeList>]<Identifier>
	 */
	private fun parseMemberAccess(rootExpression: ValueElement): ValueElement {
		val accessor = consume(WordType.MEMBER_ACCESSOR)
		val typeList = typeParser.parseOptionalTypeList()
		var expression: ValueElement = MemberAccess(rootExpression, literalParser.parseIdentifier(),
			accessor.type == WordAtom.OPTIONAL_ACCESSOR)
		if(typeList != null)
			expression = TypeSpecification(typeList.start, expression.end, expression, typeList)
		return expression
	}

	/**
	 * Index:
	 *   <Accessor>[[[<Type>[, <Type>]...];][<Expression>[, <Expression>]...]]
	 */
	private fun parseIndex(expression: ValueElement): ValueElement {
		return parseCall(expression, WordAtom.OPENING_BRACKET, WordAtom.CLOSING_BRACKET)
	}

	/**
	 * FunctionCall:
	 *   <Accessor>([[<Type>[, <Type>]...];][<Expression>[, <Expression>]...])
	 */
	private fun parseFunctionCall(expression: ValueElement): ValueElement {
		return parseCall(expression, WordAtom.OPENING_PARENTHESIS, WordAtom.CLOSING_PARENTHESIS)
	}

	/**
	 * This is a helper for:
	 * <FunctionCall>
	 * <Index>
	 */
	private fun parseCall(expression: ValueElement, startWord: WordAtom, endWord: WordAtom): ValueElement {
		val parameterListStart = consume(startWord).end
		var typeParameters: List<TypeElement>? = null
		val nextSpecialCharacter = elementGenerator.wordGenerator.scanForCharacters(parameterListStart,
			typeParameterListCheck)
		if(nextSpecialCharacter == ';') {
			typeParameters = LinkedList()
			typeParameters.add(typeParser.parseType())
			while(currentWord?.type == WordAtom.COMMA) {
				consume(WordAtom.COMMA)
				typeParameters.add(typeParser.parseType())
			}
			consume(WordAtom.SEMICOLON)
		}
		val valueParameters = LinkedList<ValueElement>()
		if(currentWord?.type != endWord) {
			valueParameters.add(parseExpression())
			while(currentWord?.type == WordAtom.COMMA) {
				consume(WordAtom.COMMA)
				valueParameters.add(parseExpression())
			}
		}
		val end = consume(endWord).end
		return if(startWord == WordAtom.OPENING_PARENTHESIS)
			FunctionCall(expression, typeParameters, valueParameters, end)
		else
			IndexAccess(expression, typeParameters, valueParameters, end)
	}

	/**
	 * Primary:
	 *   <Atom>
	 *   (<Expression>)
	 *   <LambdaFunction>
	 */
	private fun parsePrimary(): ValueElement {
		if(currentWord?.type == WordAtom.OPENING_PARENTHESIS) {
			val start = consume(WordAtom.OPENING_PARENTHESIS).start
			val isEmptyParameterListPresent = currentWord?.type == WordAtom.CLOSING_PARENTHESIS
					&& nextWord?.type == WordAtom.ARROW
			val isParameterModifierPresent = WordType.MODIFIER.includes(currentWord?.type)
			val isParameterPresent = currentWord?.type == WordAtom.IDENTIFIER
				&& nextWord?.type in listOf(WordAtom.COMMA, WordAtom.SEMICOLON, WordAtom.COLON)
			if(isEmptyParameterListPresent || isParameterModifierPresent || isParameterPresent)
				return parseLambdaFunctionDefinition(start)
			val expression = parseExpression()
			consume(WordAtom.CLOSING_PARENTHESIS)
			return expression
		}
		return parseAtom()
	}

	/**
	 * LambdaFunction:
	 *   (<ParameterList>)[: <Type>] => <Statement>
	 */
	private fun parseLambdaFunctionDefinition(start: Position): LambdaFunctionDefinition {
		val parameterList = statementParser.parseParameterList(
			WordAtom.OPENING_PARENTHESIS, WordAtom.CLOSING_PARENTHESIS,
			start)
		val returnType = if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			typeParser.parseType()
		} else null
		consume(WordAtom.ARROW)
		val body = statementParser.parseStatementSection()
		return LambdaFunctionDefinition(start, parameterList, body, returnType)
	}

	/**
	 * Atom:
	 *   <NullLiteral>
	 *   <BooleanLiteral>
	 *   <NumberLiteral>
	 *   <StringLiteral>
	 *   <SelfReference>
	 *   <Identifier>
	 *   <TypeSpecification>
	 *   <ForeignLanguageExpression>
	 */
	private fun parseAtom(): ValueElement {
		val word = getCurrentWord("atom")
		return when(word.type) {
			WordAtom.NULL_LITERAL -> literalParser.parseNullLiteral()
			WordAtom.BOOLEAN_LITERAL -> literalParser.parseBooleanLiteral()
			WordAtom.NUMBER_LITERAL -> literalParser.parseNumberLiteral()
			WordAtom.STRING_LITERAL -> literalParser.parseStringLiteral()
			WordAtom.SELF_REFERENCE -> parseSelfReference()
			WordAtom.IDENTIFIER -> {
				when(nextWord?.type) {
					WordAtom.FOREIGN_EXPRESSION -> parseForeignLanguageExpression()
					WordAtom.QUESTION_MARK -> parseNullCheck()
					else -> literalParser.parseIdentifier()
				}
			}
			WordAtom.DOT -> parseInstanceAccess()
			else -> {
				if(WordType.GENERICS_START.includes(word.type)) {
					val typeList = typeParser.parseTypeList()
					val identifier = literalParser.parseIdentifier()
					TypeSpecification(typeList.start, identifier.end, identifier, typeList)
				} else {
					throw UnexpectedWordError(word, "atom")
				}
			}
		}
	}

	/**
	 * SelfReference:
	 *   <self-reference>
	 */
	private fun parseSelfReference(): SelfReference {
		return SelfReference(consume(WordAtom.SELF_REFERENCE))
	}

	/**
	 * ForeignLanguageExpression:
	 *   <Identifier>::<ForeignLanguageLiteral>
	 */
	private fun parseForeignLanguageExpression(): ForeignLanguageExpression {
		parseForeignLanguageLiteralNext = true
		val identifier = literalParser.parseIdentifier()
		consume(WordAtom.FOREIGN_EXPRESSION)
		val foreignLanguage = parseForeignLanguageLiteral()
		return ForeignLanguageExpression(identifier, foreignLanguage)
	}

	/**
	 * ForeignLanguageLiteral:
	 *   <foreign-language>
	 */
	private fun parseForeignLanguageLiteral(): ForeignLanguageLiteral {
		return ForeignLanguageLiteral(consume(WordAtom.FOREIGN_LANGUAGE))
	}

	/**
	 * NullCheck:
	 *   <Identifier>?
	 */
	private fun parseNullCheck(): NullCheck {
		val identifier = literalParser.parseIdentifier()
		consume(WordAtom.QUESTION_MARK)
		return NullCheck(identifier)
	}

	/**
	 * InstanceAccess:
	 *   .<Identifier>
	 */
	private fun parseInstanceAccess(): InstanceAccess {
		val start = consume(WordAtom.DOT).start
		val identifier = literalParser.parseIdentifier()
		return InstanceAccess(start, identifier)
	}

	/**
	 * Operator:
	 *   <operator>
	 */
	fun parseOperator(descriptor: WordDescriptor): Operator {
		return Operator(consume(descriptor))
	}
}
