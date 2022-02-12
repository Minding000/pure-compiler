package parsing.element_generator

import parsing.ast.*
import parsing.ast.operations.*
import errors.user.UnexpectedWordError
import parsing.ast.access.Index
import parsing.ast.access.InstanceAccess
import parsing.ast.access.MemberAccess
import parsing.ast.control_flow.*
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
	fun parseExpression(): Element {
		return parseBinaryBooleanExpression()
	}

	/**
	 * BinaryBooleanExpression:
	 *   <Equality>
	 *   <Equality> & <Equality>
	 *   <Equality> | <Equality>
	 */
	private fun parseBinaryBooleanExpression(): Element {
		var expression: Element = parseEquality()
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
	private fun parseEquality(): Element {
		var expression: Element = parseComparison()
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
	private fun parseComparison(): Element {
		var expression: Element = parseAddition()
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
	private fun parseAddition(): Element {
		var expression: Element = parseMultiplication()
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
	private fun parseMultiplication(): Element {
		var expression: Element = parseNullCoalescence()
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
	private fun parseNullCoalescence(): Element {
		var expression: Element = parseCast()
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
	 *   <Try> is <TypedIdentifier>
	 *   <Try> !is <TypedIdentifier>
	 */
	private fun parseCast(): Element {
		var expression: Element = parseTry()
		if(WordType.CAST.includes(currentWord?.type)) {
			val operator = consume(WordType.CAST)
			val type = if(nextWord?.type == WordAtom.COLON)
				typeParser.parseTypedIdentifier()
			else
				typeParser.parseType()
			expression = Cast(expression, operator.getValue(), type)
		}
		return expression
	}

	/**
	 * Try:
	 *   <UnaryOperator>
	 *   try? <UnaryOperator>
	 *   try! <UnaryOperator>
	 */
	private fun parseTry(): Element {
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
	private fun parseUnaryOperator(): Element {
		if(WordType.UNARY_OPERATOR.includes(currentWord?.type)) {
			val operator = consume(WordType.UNARY_OPERATOR)
			return UnaryOperator(parseReferenceChain(), operator)
		}
		return parseReferenceChain()
	}

	/**
	 * ReferenceChain:
	 *   <FunctionCall>
	 *   <FunctionCall>[[?].<FunctionCall>]
	 */
	fun parseReferenceChain(): Element {
		var expression = parseFunctionCall()
		while(WordType.MEMBER_ACCESSOR.includes(currentWord?.type)) {
			val accessor = consume(WordType.MEMBER_ACCESSOR)
			expression = MemberAccess(expression, parseFunctionCall(), accessor.type == WordAtom.OPTIONAL_ACCESSOR)
		}
		return expression
	}

	/**
	 * FunctionCall:
	 *   <Index>
	 *   <Index>([<Expression>[, <Expression>]...])
	 */
	private fun parseFunctionCall(): Element {
		var expression = parseIndex()
		if(currentWord?.type == WordAtom.PARENTHESES_OPEN) {
			consume(WordAtom.PARENTHESES_OPEN)
			val parameters = LinkedList<Element>()
			if(currentWord?.type != WordAtom.PARENTHESES_CLOSE) {
				parameters.add(parseExpression())
				while(currentWord?.type == WordAtom.COMMA) {
					consume(WordAtom.COMMA)
					parameters.add(parseExpression())
				}
			}
			val end = consume(WordAtom.PARENTHESES_CLOSE).end
			expression = FunctionCall(expression, parameters, end)
		}
		return expression
	}

	/**
	 * Index:
	 *   <Primary>
	 *   <Primary>[<Expression>[, <Expression>]...]
	 */
	fun parseIndex(): Element {
		var expression = parsePrimary()
		if(currentWord?.type == WordAtom.BRACKETS_OPEN) {
			consume(WordAtom.BRACKETS_OPEN)
			val indices = LinkedList<Element>()
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
	 * Primary:
	 *   <Atom>
	 *   (<Expression>)
	 */
	private fun parsePrimary(): Element {
		if(currentWord?.type == WordAtom.PARENTHESES_OPEN) {
			consume(WordAtom.PARENTHESES_OPEN)
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
	 *   <SimpleType>
	 *   <ForeignLanguageExpression>
	 */
	private fun parseAtom(): Element {
		val word = getCurrentWord("atom")
		return when(word.type) {
			WordAtom.NULL_LITERAL -> literalParser.parseNullLiteral()
			WordAtom.BOOLEAN_LITERAL -> literalParser.parseBooleanLiteral()
			WordAtom.NUMBER_LITERAL -> literalParser.parseNumberLiteral()
			WordAtom.STRING_LITERAL -> literalParser.parseStringLiteral()
			WordAtom.IDENTIFIER -> {
				when(nextWord?.type) {
					WordAtom.DOUBLE_COLON -> parseForeignLanguageExpression()
					WordAtom.QUESTION_MARK -> parseNullCheck()
					else -> literalParser.parseIdentifier()
				}
			}
			WordAtom.DOT -> parseInstanceAccess()
			else -> {
				if(WordType.GENERICS_START.includes(word.type))
					typeParser.parseSimpleType()
				else
					throw UnexpectedWordError(word, "atom")
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
		consume(WordAtom.DOUBLE_COLON)
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