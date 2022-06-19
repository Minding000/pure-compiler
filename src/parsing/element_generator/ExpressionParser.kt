package parsing.element_generator

import parsing.ast.operations.*
import errors.user.UnexpectedWordError
import parsing.ast.access.Index
import parsing.ast.access.InstanceAccess
import parsing.ast.access.MemberAccess
import parsing.ast.control_flow.*
import parsing.ast.definitions.LambdaFunctionDefinition
import parsing.ast.definitions.Parameter
import parsing.ast.definitions.ParameterList
import parsing.ast.general.*
import parsing.ast.literals.*
import parsing.tokenizer.*
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
	 *   <ReferenceChain>
	 *   !<ReferenceChain>
	 *   +<ReferenceChain>
	 *   -<ReferenceChain>
	 *   ...<ReferenceChain>
	 */
	private fun parseUnaryOperator(): ValueElement {
		if(WordType.UNARY_OPERATOR.includes(currentWord?.type)) {
			val operator = consume(WordType.UNARY_OPERATOR)
			return UnaryOperator(parseReferenceChain(), operator)
		}
		return parseReferenceChain()
	}

	/**
	 * ReferenceChain:
	 *   <Index>
	 *   <Index>[[?].<Index>]
	 */
	private fun parseReferenceChain(): ValueElement {
		var expression = parseIndex()
		while(WordType.MEMBER_ACCESSOR.includes(currentWord?.type)) {
			val accessor = consume(WordType.MEMBER_ACCESSOR)
			expression = MemberAccess(expression, parseIndex(), accessor.type == WordAtom.OPTIONAL_ACCESSOR)
		}
		return expression
	}

	/**
	 * Index:
	 *   <FunctionCall>
	 *   <FunctionCall>[<Expression>[, <Expression>]...]
	 */
	private fun parseIndex(): ValueElement {
		var expression = parseFunctionCall()
		if(currentWord?.type == WordAtom.BRACKETS_OPEN) {
			consume(WordAtom.BRACKETS_OPEN)
			val indices = LinkedList<ValueElement>()
			indices.add(parseExpression())
			while(currentWord?.type == WordAtom.COMMA) {
				consume(WordAtom.COMMA)
				indices.add(parseExpression())
			}
			val end = consume(WordAtom.BRACKETS_CLOSE).end
			expression = Index(expression, indices, end)
		}
		return expression
	}

	/**
	 * FunctionCall:
	 *   <Primary>
	 *   [<TypeList>]<Primary>([<Expression>[, <Expression>]...])
	 */
	private fun parseFunctionCall(): ValueElement {
		val typeList = typeParser.parseTypeList()
		//TODO allow for anonymous function calls
		var expression = parsePrimary()
		if(typeList != null || currentWord?.type == WordAtom.PARENTHESES_OPEN) {
			consume(WordAtom.PARENTHESES_OPEN)
			val parameters = LinkedList<ValueElement>()
			if(currentWord?.type != WordAtom.PARENTHESES_CLOSE) {
				parameters.add(parseExpression())
				while(currentWord?.type == WordAtom.COMMA) {
					consume(WordAtom.COMMA)
					parameters.add(parseExpression())
				}
			}
			val end = consume(WordAtom.PARENTHESES_CLOSE).end
			expression = FunctionCall(typeList, expression, parameters, end)
		}
		return expression
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
			val isEmptyParameterListVisible = currentWord?.type == WordAtom.PARENTHESES_CLOSE && nextWord?.type == WordAtom.ARROW
			val isParameterVisible = currentWord?.type == WordAtom.IDENTIFIER && (nextWord?.type == WordAtom.COMMA || nextWord?.type == WordAtom.COLON)
			val isParameterModifierVisible = WordType.MODIFIER.includes(currentWord?.type)
			if(isEmptyParameterListVisible || isParameterVisible || isParameterModifierVisible) {
				val parameters = LinkedList<Parameter>()
				while(currentWord?.type != WordAtom.PARENTHESES_CLOSE) {
					parameters.add(statementParser.parseParameter())
					if(currentWord?.type != WordAtom.COMMA)
						break
					consume(WordAtom.COMMA)
				}
				val end = consume(WordAtom.PARENTHESES_CLOSE).end
				consume(WordAtom.ARROW)
				val parameterList = ParameterList(start, end, parameters)
				val body = statementParser.parseStatementSection()
				return LambdaFunctionDefinition(start, parameterList, body)
			}
			val expression = parseExpression()
			consume(WordAtom.PARENTHESES_CLOSE)
			return expression
		}
		return parseAtom()
	}

	/**
	 * Atom:
	 *   <NullLiteral>
	 *   <BooleanLiteral>
	 *   <NumberLiteral>
	 *   <StringLiteral>
	 *   <Identifier>
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
			else -> throw UnexpectedWordError(word, "atom")
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
		consume(WordAtom.DOT)
		val identifier = literalParser.parseIdentifier()
		return InstanceAccess(identifier)
	}
}