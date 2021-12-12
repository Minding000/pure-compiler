package parsing

import parsing.tokenizer.Word
import parsing.tokenizer.WordGenerator
import parsing.tokenizer.WordType
import parsing.ast.*
import parsing.ast.control_flow.FunctionCall
import parsing.ast.control_flow.IfStatement
import parsing.ast.definitions.ClassDefinition
import parsing.ast.operations.*
import errors.user.UnexpectedWordError
import parsing.ast.definitions.FunctionDefinition
import errors.user.UnexpectedEndOfFileError
import parsing.ast.access.ReferenceChain
import parsing.ast.control_flow.ReturnStatement
import source_structure.Project
import parsing.ast.definitions.TypedIdentifier
import parsing.ast.definitions.VariableDeclaration
import parsing.ast.literals.*
import java.util.*

class ElementGenerator(project: Project) {
	private val wordGenerator: WordGenerator
	private var currentWord: Word?
	private var nextWord: Word?

	init {
		wordGenerator = WordGenerator(project)
		currentWord = wordGenerator.getNextWord()
		nextWord = wordGenerator.getNextWord()
	}

	private fun consume(type: WordType): Word {
		val consumedWord = getCurrentWord(type)
		if(consumedWord.type != type)
			throw UnexpectedWordError(consumedWord, type)
		currentWord = nextWord
		nextWord = wordGenerator.getNextWord()
		return consumedWord
	}

	private fun consumeLineBreaks() {
		while(currentWord?.type == WordType.LINE_BREAK)
			consume(WordType.LINE_BREAK)
	}

	private fun getCurrentWord(expectation: String): Word {
		return currentWord ?: throw UnexpectedEndOfFileError(expectation)
	}

	private fun getCurrentWord(expectation: WordType): Word {
		return getCurrentWord(expectation.toString())
	}

	private fun getCurrentWord(expectation: List<WordType>): Word {
		return getCurrentWord(expectation.toString())
	}

	/**
	 * Program:
	 *   <empty>
	 *   <Program>\n
	 *   <Statement>
	 *   <Program>\n<Statement>
	 */
	fun parseProgram(): Program {
		val statements = LinkedList<Element>()
		while(currentWord != null) {
			consumeLineBreaks()
			if(currentWord == null)
				break
			statements.add(parseStatement())
		}
		return Program(statements)
	}

	/**
	 * Statement:
	 *   <Print>
	 *   <IfStatement>
	 *   <ReturnStatement>
	 *   <VariableDeclaration>
	 *   <ClassDefinition>
	 *   <Assignment>
	 *   <UnaryModification>
	 *   <BinaryModification>
	 *   <Expression>
	 */
	private fun parseStatement(): Element {
		if(currentWord?.type == WordType.ECHO)
			return parsePrint()
		if(currentWord?.type == WordType.IF)
			return parseIfStatement()
		if(currentWord?.type == WordType.RETURN)
			return parseReturnStatement()
		if(currentWord?.type == WordType.VAR)
			return parseVariableDeclaration()
		if(currentWord?.type == WordType.CLASS)
			return parseClassDefinition()
		if(nextWord?.type == WordType.ASSIGNMENT)
			return parseAssignment()
		if(nextWord?.type == WordType.UNARY_MODIFICATION)
			return parseUnaryModification()
		if(nextWord?.type == WordType.BINARY_MODIFICATION)
			return parseBinaryModification()
		return parseExpression()
	}

	/**
	 * IfStatement:
	 *   if <Expression>
	 *       <Statement>
	 *   [else
	 *   	 <Statement>]
	 */
	private fun parseIfStatement(): IfStatement {
		val start = consume(WordType.IF).start
		val condition = parseExpression()
		consumeLineBreaks()
		val trueBranch = parseStatement()
		consumeLineBreaks()
		var falseBranch: Element? = null
		if(currentWord?.type == WordType.ELSE) {
			consume(WordType.ELSE)
			consumeLineBreaks()
			falseBranch = parseStatement()
		}
		return IfStatement(condition, trueBranch, falseBranch, start, falseBranch?.end ?: trueBranch.end)
	}

	/**
	 * ReturnStatement:
	 *   return <Expression>
	 */
	private fun parseReturnStatement(): ReturnStatement {
		val word = consume(WordType.RETURN)
		var value: Element? = null
		if(currentWord?.type != WordType.LINE_BREAK)
			value = parseExpression()
		return ReturnStatement(value, word.start, value?.end ?: word.end)
	}

	/**
	 * Print:
	 *   echo <Expression>[,<Expression>]...
	 */
	private fun parsePrint(): Print {
		val start = consume(WordType.ECHO).start
		val elements = LinkedList<Element>()
		elements.add(parseExpression())
		while(currentWord?.type == WordType.COMMA) {
			consume(WordType.COMMA)
			elements.add(parseExpression())
		}
		return Print(start, elements.last.end, elements)
	}

	/**
	 * ClassDefinition:
	 *   class <Identifier> { [<MemberDeclaration>\n]... }
	 */
	private fun parseClassDefinition(): ClassDefinition {
		val start = consume(WordType.CLASS).start
		val identifier = parseIdentifier()
		consume(WordType.BRACES_OPEN)
		val members = LinkedList<Element>()
		while(currentWord?.type != WordType.BRACES_CLOSE) {
			consumeLineBreaks()
			if(currentWord?.type == WordType.BRACES_CLOSE)
				break
			members.add(parseMemberDeclaration())
		}
		val end = consume(WordType.BRACES_CLOSE).end
		return ClassDefinition(start, end, identifier, members)
	}

	/**
	 * MemberDeclaration:
	 *   <PropertyDeclaration>
	 *   <FunctionDefinition>
	 */
	private fun parseMemberDeclaration(): Element {
		if(currentWord?.type == WordType.VAR)
			return parsePropertyDeclaration()
		if(currentWord?.type == WordType.FUN)
			return parseFunctionDefinition()
		val requiredWordTypeList = listOf(WordType.VAR, WordType.FUN)
		throw UnexpectedWordError(getCurrentWord(requiredWordTypeList), requiredWordTypeList)
	}

	/**
	 * PropertyDeclaration:
	 *   <VariableDeclaration>
	 */
	private fun parsePropertyDeclaration(): VariableDeclaration {
		return parseVariableDeclaration()
	}

	/**
	 * FunctionDeclaration:
	 *   fun <Identifier>(<Parameter>[,<Parameter>])[: <Type>] { [<Statement>\n]... }
	 */
	private fun parseFunctionDefinition(): Element {
		val start = consume(WordType.FUN).start
		val identifier = parseIdentifier()
		consume(WordType.PARENTHESES_OPEN)
		val parameters = LinkedList<TypedIdentifier>()
		parameters.add(parseParameter())
		while(currentWord?.type == WordType.COMMA) {
			consume(WordType.COMMA)
			parameters.add(parseParameter())
		}
		consume(WordType.PARENTHESES_CLOSE)
		var returnType: Identifier? = null
		if(currentWord?.type == WordType.COLON) {
			consume(WordType.COLON)
			returnType = parseType()
		}
		consume(WordType.BRACES_OPEN)
		val statements = LinkedList<Element>()
		while(currentWord?.type != WordType.BRACES_CLOSE) {
			consumeLineBreaks()
			if(currentWord?.type == WordType.BRACES_CLOSE)
				break
			statements.add(parseStatement())
		}
		val end = consume(WordType.BRACES_CLOSE).end
		return FunctionDefinition(start, end, identifier, parameters, statements, returnType)
	}

	/**
	 * Parameter:
	 *   <TypedIdentifier>
	 */
	private fun parseParameter(): TypedIdentifier {
		return parseTypedIdentifier()
	}

	/**
	 * VariableDeclaration:
	 *   var <VariableDeclarationPart>[,<VariableDeclarationPart>]...
	 */
	private fun parseVariableDeclaration(): VariableDeclaration {
		val start = consume(WordType.VAR).start
		val declarationParts = LinkedList<Element>()
		declarationParts.add(parseVariableDeclarationPart())
		while(currentWord?.type == WordType.COMMA) {
			consume(WordType.COMMA)
			declarationParts.add(parseVariableDeclarationPart())
		}
		return VariableDeclaration(start, declarationParts.last.end, declarationParts)
	}

	/**
	 * VariableDeclarationPart:
	 *   <TypedIdentifier>
	 *   <Assignment:declare>
	 */
	private fun parseVariableDeclarationPart(): Element {
		if(nextWord?.type == WordType.ASSIGNMENT)
			return parseAssignment(true)
		return parseTypedIdentifier()
	}

	/**
	 * Assignment:
	 *   <Identifier> = <Expression>
	 * Assignment[declare]:
	 *   <Identifier|TypedIdentifier> = <Expression>
	 */
	private fun parseAssignment(declare: Boolean = false): Assignment {
		val identifier = if(declare) {
			if(nextWord?.type == WordType.COLON) {
				parseTypedIdentifier()
			} else {
				parseIdentifier()
			}
		} else {
			parseReferenceChain()
		}
		consume(WordType.ASSIGNMENT)
		val expression = parseExpression()
		return Assignment(identifier, expression)
	}

	/**
	 * UnaryModification:
	 *   <Identifier>++
	 *   <Identifier>--
	 */
	private fun parseUnaryModification(): UnaryModification {
		val identifier = parseReferenceChain()
		val operator = consume(WordType.UNARY_MODIFICATION)
		return UnaryModification(identifier, operator)
	}

	/**
	 * BinaryModification:
	 *   <Identifier> += <Expression>
	 *   <Identifier> -= <Expression>
	 *   <Identifier> *= <Expression>
	 *   <Identifier> /= <Expression>
	 *   <Identifier> ^= <Expression>
	 */
	private fun parseBinaryModification(): BinaryModification {
		val identifier = parseReferenceChain()
		val operator = consume(WordType.BINARY_MODIFICATION)
		val expression = parseExpression()
		return BinaryModification(identifier, expression, operator.getValue())
	}

	/**
	 * Expression:
	 *   <BinaryBooleanExpression>
	 */
	private fun parseExpression(): Element {
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
		while(currentWord?.type == WordType.BINARY_BOOLEAN_OPERATOR) {
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
		while(currentWord?.type == WordType.EQUALITY) {
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
		while(currentWord?.type == WordType.COMPARISON) {
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
		while(currentWord?.type == WordType.ADDITION) {
			val operator = consume(WordType.ADDITION)
			expression = BinaryOperator(expression, parseMultiplication(), operator.getValue())
		}
		return expression
	}

	/**
	 * Multiplication:
	 *   <Exponentiation>
	 *   <Exponentiation> * <Exponentiation>
	 *   <Exponentiation> / <Exponentiation>
	 */
	private fun parseMultiplication(): Element {
		var expression: Element = parseExponentiation()
		while(currentWord?.type == WordType.MULTIPLICATION) {
			val operator = consume(WordType.MULTIPLICATION)
			expression = BinaryOperator(expression, parseExponentiation(), operator.getValue())
		}
		return expression
	}

	/**
	 * Exponentiation:
	 *   <UnaryOperator>
	 *   <UnaryOperator> ^ <UnaryOperator>
	 */
	private fun parseExponentiation(): Element {
		var expression: Element = parseUnaryOperator()
		while(currentWord?.type == WordType.EXPONENTIATION) {
			val operator = consume(WordType.EXPONENTIATION)
			expression = BinaryOperator(expression, parseUnaryOperator(), operator.getValue())
		}
		return expression
	}

	/**
	 * UnaryOperator:
	 *   <Primary>
	 *   !<Primary>
	 *   +<Primary>
	 *   -<Primary>
	 */
	private fun parseUnaryOperator(): Element {
		if(currentWord?.type == WordType.NOT) {
			val operator = consume(WordType.NOT)
			return UnaryOperator(parsePrimary(), operator)
		}
		if(currentWord?.type == WordType.ADDITION) {
			val operator = consume(WordType.ADDITION)
			return UnaryOperator(parsePrimary(), operator)
		}
		return parsePrimary()
	}

	/**
	 * Primary:
	 *   <Atom>
	 *   (<Expression>)
	 */
	private fun parsePrimary(): Element {
		if(currentWord?.type == WordType.PARENTHESES_OPEN) {
			consume(WordType.PARENTHESES_OPEN)
			val expression = parseExpression()
			consume(WordType.PARENTHESES_CLOSE)
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
	 *   <FunctionCall>
	 */
	private fun parseAtom(): Element {
		val word = getCurrentWord("atom")
		return when(word.type) {
			WordType.NULL_LITERAL -> parseNullLiteral()
			WordType.BOOLEAN_LITERAL -> parseBooleanLiteral()
			WordType.NUMBER_LITERAL -> parseNumberLiteral()
			WordType.STRING_LITERAL -> parseStringLiteral()
			WordType.IDENTIFIER -> parseFunctionCall()
			else -> throw UnexpectedWordError(word, "atom")
		}
	}

	/**
	 * FunctionCall:
	 *   <IdentifierExpression>[([<Expression>[, <Expression>]...])]
	 */
	private fun parseFunctionCall(): Element {
		val identifierReference = parseReferenceChain()
		if(currentWord?.type == WordType.PARENTHESES_OPEN) {
			consume(WordType.PARENTHESES_OPEN)
			val parameters = LinkedList<Element>()
			if(currentWord?.type != WordType.PARENTHESES_CLOSE) {
				parameters.add(parseExpression())
				while(currentWord?.type == WordType.COMMA) {
					consume(WordType.COMMA)
					parameters.add(parseExpression())
				}
			}
			val end = consume(WordType.PARENTHESES_CLOSE).end
			return FunctionCall(identifierReference, parameters, identifierReference.start, end)
		}
		return identifierReference
	}

	/**
	 * ReferenceChain:
	 *   <Identifier>[.<Identifier>]...
	 */
	private fun parseReferenceChain(): Element {
		return if(nextWord?.type == WordType.DOT) {
			val identifiers = LinkedList<Identifier>()
			identifiers.add(parseIdentifier())
			if(currentWord?.type == WordType.DOT) {
				consume(WordType.DOT)
				identifiers.add(parseIdentifier())
			}
			ReferenceChain(identifiers)
		} else {
			parseIdentifier()
		}
	}

	/**
	 * TypedIdentifier:
	 *   <Identifier>: <Type>
	 */
	private fun parseTypedIdentifier(): TypedIdentifier {
		val identifier = parseIdentifier()
		consume(WordType.COLON)
		val type = parseType()
		return TypedIdentifier(identifier, type)
	}

	/**
	 * Identifier:
	 *   <identifier>
	 */
	private fun parseIdentifier(): Identifier {
		return Identifier(consume(WordType.IDENTIFIER))
	}

	/**
	 * Type:
	 *   <Identifier>
	 */
	private fun parseType(): Identifier {
		return parseIdentifier()
	}

	/**
	 * NullLiteral:
	 *   null
	 */
	private fun parseNullLiteral(): NullLiteral {
		return NullLiteral(consume(WordType.NULL_LITERAL))
	}

	/**
	 * BooleanLiteral:
	 *   <number>
	 */
	private fun parseBooleanLiteral(): BooleanLiteral {
		return BooleanLiteral(consume(WordType.BOOLEAN_LITERAL))
	}

	/**
	 * NumberLiteral:
	 *   <number>
	 */
	private fun parseNumberLiteral(): NumberLiteral {
		return NumberLiteral(consume(WordType.NUMBER_LITERAL))
	}

	/**
	 * StringLiteral:
	 *   <string>
	 */
	private fun parseStringLiteral(): StringLiteral {
		return StringLiteral(consume(WordType.STRING_LITERAL))
	}
}