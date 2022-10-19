package parsing.element_generator

import errors.internal.CompilerError
import errors.user.UnexpectedWordError
import parsing.syntax_tree.access.IndexAccess
import parsing.syntax_tree.access.InstanceAccess
import parsing.syntax_tree.access.MemberAccess
import parsing.syntax_tree.control_flow.FunctionCall
import parsing.syntax_tree.control_flow.Try
import parsing.syntax_tree.definitions.LambdaFunctionDefinition
import parsing.syntax_tree.definitions.TypeSpecification
import parsing.syntax_tree.general.ForeignLanguageExpression
import parsing.syntax_tree.general.ValueElement
import parsing.syntax_tree.literals.ForeignLanguageLiteral
import parsing.syntax_tree.literals.Identifier
import parsing.syntax_tree.operations.BinaryOperator
import parsing.syntax_tree.operations.Cast
import parsing.syntax_tree.operations.NullCheck
import parsing.syntax_tree.operations.UnaryOperator
import parsing.tokenizer.Word
import parsing.tokenizer.WordAtom
import parsing.tokenizer.WordDescriptor
import parsing.tokenizer.WordType
import source_structure.Position
import java.util.*

class ExpressionParser(private val elementGenerator: ElementGenerator): Generator() {
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
			val operator = consume(WordType.BINARY_BOOLEAN_OPERATOR)
			expression = BinaryOperator(expression, parseEquality(), operator.getValue())
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
			val operator = consume(WordType.EQUALITY)
			expression = BinaryOperator(expression, parseComparison(), operator.getValue())
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
			val operator = consume(WordType.COMPARISON)
			expression = BinaryOperator(expression, parseAddition(), operator.getValue())
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
			val operator = consume(WordType.ADDITION)
			expression = BinaryOperator(expression, parseMultiplication(), operator.getValue())
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
			val operator = consume(WordType.MULTIPLICATION)
			expression = BinaryOperator(expression, parseNullCoalescence(), operator.getValue())
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
			val operator = consume(WordAtom.NULL_COALESCENCE)
			expression = BinaryOperator(expression, parseCast(), operator.getValue())
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
			return Try(parseUnaryOperator(), operator.type == WordAtom.TRY_OPTIONAL, operator.start)
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
			val operator = consume(WordType.UNARY_OPERATOR)
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
	 *   <Accessor>$<TypeList>
	 */
	private fun parseAccessor(): ValueElement {
		var expression = parsePrimary()
		while(WordType.ACCESSOR.includes(currentWord?.type)) {
			expression =  if(WordType.MEMBER_ACCESSOR.includes(currentWord?.type))
				parseMemberAccess(expression)
			else if(currentWord?.type == WordAtom.PARENTHESES_OPEN)
				parseFunctionCall(expression)
			else if(currentWord?.type == WordAtom.BRACKETS_OPEN)
				parseIndex(expression)
			else if(currentWord?.type == WordAtom.TYPE_SPECIFICATION)
				parseTypeSpecification(expression)
			else
				throw CompilerError("Failed to parse accessor: '${currentWord?.type}'")
		}
		return expression
	}

	/**
	 * TypeSpecification:
	 *   <Accessor>$<TypeList>
	 */
	private fun parseTypeSpecification(expression: ValueElement): ValueElement {
		consume(WordAtom.TYPE_SPECIFICATION)
		val typeList = typeParser.parseTypeList()
		return TypeSpecification(expression.start, typeList.end, expression, typeList)
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
		consume(WordAtom.BRACKETS_OPEN)
		var genericParameters: List<ValueElement>? = null
		var indices = LinkedList<ValueElement>()
		if(currentWord?.type != WordAtom.BRACKETS_CLOSE) {
			if(currentWord?.type != WordAtom.SEMICOLON) {
				indices.add(parseExpression())
				while(currentWord?.type == WordAtom.COMMA) {
					consume(WordAtom.COMMA)
					indices.add(parseExpression())
				}
			}
			if(currentWord?.type == WordAtom.SEMICOLON) {
				consume(WordAtom.SEMICOLON)
				genericParameters = indices
				indices = LinkedList()
				if(currentWord?.type != WordAtom.BRACKETS_CLOSE) {
					indices.add(parseExpression())
					while(currentWord?.type == WordAtom.COMMA) {
						consume(WordAtom.COMMA)
						indices.add(parseExpression())
					}
				}
			}
		}
		val end = consume(WordAtom.BRACKETS_CLOSE).end
		return IndexAccess(expression, genericParameters, indices, end)
	}

	/**
	 * FunctionCall:
	 *   <Accessor>([[<Type>[, <Type>]...];][<Expression>[, <Expression>]...])
	 */
	private fun parseFunctionCall(expression: ValueElement): ValueElement {
		consume(WordAtom.PARENTHESES_OPEN)
		var genericParameters: List<ValueElement>? = null
		var parameters = LinkedList<ValueElement>()
		if(currentWord?.type != WordAtom.PARENTHESES_CLOSE) {
			if(currentWord?.type != WordAtom.SEMICOLON) {
				parameters.add(parseExpression())
				while(currentWord?.type == WordAtom.COMMA) {
					consume(WordAtom.COMMA)
					parameters.add(parseExpression())
				}
			}
			if(currentWord?.type == WordAtom.SEMICOLON) {
				consume(WordAtom.SEMICOLON)
				genericParameters = parameters
				parameters = LinkedList()
				if(currentWord?.type != WordAtom.PARENTHESES_CLOSE) {
					parameters.add(parseExpression())
					while(currentWord?.type == WordAtom.COMMA) {
						consume(WordAtom.COMMA)
						parameters.add(parseExpression())
					}
				}
			}
		}
		val end = consume(WordAtom.PARENTHESES_CLOSE).end
		return FunctionCall(expression, genericParameters, parameters, end)
	}

	/**
	 * Primary:
	 *   <Atom>
	 *   (<Expression>)
	 *   <LambdaFunction>
	 */
	private fun parsePrimary(): ValueElement {
		if(currentWord?.type == WordAtom.PARENTHESES_OPEN) {
			val start = consume(WordAtom.PARENTHESES_OPEN).start
			val isEmptyParameterListPresent = currentWord?.type == WordAtom.PARENTHESES_CLOSE
					&& nextWord?.type == WordAtom.ARROW
			val isParameterModifierPresent = WordType.MODIFIER.includes(currentWord?.type)
			val isParameterPresent = currentWord?.type == WordAtom.IDENTIFIER
				&& nextWord?.type in listOf(WordAtom.COMMA, WordAtom.SEMICOLON, WordAtom.COLON)
			if(isEmptyParameterListPresent || isParameterModifierPresent || isParameterPresent)
				return parseLambdaFunctionDefinition(start)
			val expression = parseExpression()
			consume(WordAtom.PARENTHESES_CLOSE)
			return expression
		}
		return parseAtom()
	}

	/**
	 * LambdaFunction:
	 *   (<ParameterList>)[: <Type>] => <Statement>
	 */
	private fun parseLambdaFunctionDefinition(start: Position): LambdaFunctionDefinition {
		val parameterList = statementParser.parseParameterList(WordAtom.PARENTHESES_OPEN, WordAtom.PARENTHESES_CLOSE,
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
}
