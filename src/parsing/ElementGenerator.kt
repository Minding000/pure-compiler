package parsing

import parsing.ast.*
import parsing.ast.control_flow.FunctionCall
import parsing.ast.control_flow.IfStatement
import parsing.ast.operations.*
import errors.user.UnexpectedWordError
import errors.user.UnexpectedEndOfFileError
import parsing.ast.access.ReferenceChain
import parsing.ast.control_flow.ReturnStatement
import parsing.ast.definitions.*
import source_structure.Project
import parsing.ast.literals.*
import parsing.tokenizer.*
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

	private fun consume(type: WordDescriptor): Word {
		val consumedWord = getCurrentWord(type)
		if(!type.includes(consumedWord.type))
			throw UnexpectedWordError(consumedWord, type)
		currentWord = nextWord
		nextWord = wordGenerator.getNextWord()
		return consumedWord
	}

	private fun consumeLineBreaks() {
		while(currentWord?.type == WordAtom.LINE_BREAK)
			consume(WordAtom.LINE_BREAK)
	}

	private fun getCurrentWord(expectation: String): Word {
		return currentWord ?: throw UnexpectedEndOfFileError(expectation)
	}

	private fun getCurrentWord(expectation: WordDescriptor): Word {
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
	 *   <ObjectDefinition>
	 *   <Assignment>
	 *   <UnaryModification>
	 *   <BinaryModification>
	 *   <Expression>
	 */
	private fun parseStatement(): Element {
		if(currentWord?.type == WordAtom.ECHO)
			return parsePrint()
		if(currentWord?.type == WordAtom.IF)
			return parseIfStatement()
		if(currentWord?.type == WordAtom.RETURN)
			return parseReturnStatement()
		if(currentWord?.type == WordAtom.VAR)
			return parseVariableDeclaration()
		if(currentWord?.type == WordAtom.CLASS)
			return parseClassDefinition()
		if(currentWord?.type == WordAtom.OBJECT)
			return parseObjectDefinition()
		if(nextWord?.type == WordAtom.ASSIGNMENT)
			return parseAssignment()
		if(WordType.UNARY_MODIFICATION.includes(nextWord?.type))
			return parseUnaryModification()
		if(WordType.BINARY_MODIFICATION.includes(nextWord?.type))
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
		val start = consume(WordAtom.IF).start
		val condition = parseExpression()
		consumeLineBreaks()
		val trueBranch = parseStatement()
		consumeLineBreaks()
		var falseBranch: Element? = null
		if(currentWord?.type == WordAtom.ELSE) {
			consume(WordAtom.ELSE)
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
		val word = consume(WordAtom.RETURN)
		var value: Element? = null
		if(currentWord?.type != WordAtom.LINE_BREAK)
			value = parseExpression()
		return ReturnStatement(value, word.start, value?.end ?: word.end)
	}

	/**
	 * Print:
	 *   echo <Expression>[,<Expression>]...
	 */
	private fun parsePrint(): Print {
		val start = consume(WordAtom.ECHO).start
		val elements = LinkedList<Element>()
		elements.add(parseExpression())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			elements.add(parseExpression())
		}
		return Print(start, elements.last.end, elements)
	}

	/**
	 * ClassDefinition:
	 *   class <Identifier> { [<MemberDeclaration>\n]... }
	 */
	private fun parseClassDefinition(): ClassDefinition {
		val start = consume(WordAtom.CLASS).start
		val identifier = parseIdentifier()
		consume(WordAtom.BRACES_OPEN)
		val members = LinkedList<Element>()
		while(currentWord?.type != WordAtom.BRACES_CLOSE) {
			consumeLineBreaks()
			if(currentWord?.type == WordAtom.BRACES_CLOSE)
				break
			members.add(parseMemberDeclaration())
		}
		val end = consume(WordAtom.BRACES_CLOSE).end
		return ClassDefinition(start, end, identifier, members)
	}

	/**
	 * ObjectDefinition:
	 *   object <Identifier> { [<MemberDeclaration>\n]... }
	 */
	private fun parseObjectDefinition(): ObjectDefinition {
		val start = consume(WordAtom.OBJECT).start
		val identifier = parseIdentifier()
		consume(WordAtom.BRACES_OPEN)
		val members = LinkedList<Element>()
		while(currentWord?.type != WordAtom.BRACES_CLOSE) {
			consumeLineBreaks()
			if(currentWord?.type == WordAtom.BRACES_CLOSE)
				break
			members.add(parseMemberDeclaration())
		}
		val end = consume(WordAtom.BRACES_CLOSE).end
		return ObjectDefinition(start, end, identifier, members)
	}

	/**
	 * MemberDeclaration:
	 *   <PropertyDeclaration>
	 *   <FunctionDefinition>
	 */
	private fun parseMemberDeclaration(): Element {
		if(currentWord?.type == WordAtom.VAR)
			return parsePropertyDeclaration()
		if(currentWord?.type == WordAtom.FUN)
			return parseFunctionDefinition()
		throw UnexpectedWordError(getCurrentWord(WordType.MEMBER), WordType.MEMBER)
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
		val start = consume(WordAtom.FUN).start
		val identifier = parseIdentifier()
		consume(WordAtom.PARENTHESES_OPEN)
		val parameters = LinkedList<TypedIdentifier>()
		parameters.add(parseParameter())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			parameters.add(parseParameter())
		}
		consume(WordAtom.PARENTHESES_CLOSE)
		var returnType: Identifier? = null
		if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			returnType = parseType()
		}
		consume(WordAtom.BRACES_OPEN)
		val statements = LinkedList<Element>()
		while(currentWord?.type != WordAtom.BRACES_CLOSE) {
			consumeLineBreaks()
			if(currentWord?.type == WordAtom.BRACES_CLOSE)
				break
			statements.add(parseStatement())
		}
		val end = consume(WordAtom.BRACES_CLOSE).end
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
		val start = consume(WordAtom.VAR).start
		val declarationParts = LinkedList<Element>()
		declarationParts.add(parseVariableDeclarationPart())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
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
		if(nextWord?.type == WordAtom.ASSIGNMENT)
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
			if(nextWord?.type == WordAtom.COLON) {
				parseTypedIdentifier()
			} else {
				parseIdentifier()
			}
		} else {
			parseReferenceChain()
		}
		consume(WordAtom.ASSIGNMENT)
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
	 *   <UnaryOperator>
	 *   <UnaryOperator> * <UnaryOperator>
	 *   <UnaryOperator> / <UnaryOperator>
	 */
	private fun parseMultiplication(): Element {
		var expression: Element = parseUnaryOperator()
		while(WordType.MULTIPLICATION.includes(currentWord?.type)) {
			val operator = consume(WordType.MULTIPLICATION)
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
		if(WordType.UNARY_OPERATOR.includes(currentWord?.type)) {
			val operator = consume(WordType.UNARY_OPERATOR)
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
	 *   <FunctionCall>
	 */
	private fun parseAtom(): Element {
		val word = getCurrentWord("atom")
		return when(word.type) {
			WordAtom.NULL_LITERAL -> parseNullLiteral()
			WordAtom.BOOLEAN_LITERAL -> parseBooleanLiteral()
			WordAtom.NUMBER_LITERAL -> parseNumberLiteral()
			WordAtom.STRING_LITERAL -> parseStringLiteral()
			WordAtom.IDENTIFIER -> parseFunctionCall()
			else -> throw UnexpectedWordError(word, "atom")
		}
	}

	/**
	 * FunctionCall:
	 *   <IdentifierExpression>[([<Expression>[, <Expression>]...])]
	 */
	private fun parseFunctionCall(): Element {
		val identifierReference = parseReferenceChain()
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
			return FunctionCall(identifierReference, parameters, identifierReference.start, end)
		}
		return identifierReference
	}

	/**
	 * ReferenceChain:
	 *   <Identifier>[.<Identifier>]...
	 */
	private fun parseReferenceChain(): Element {
		return if(nextWord?.type == WordAtom.DOT) {
			val identifiers = LinkedList<Identifier>()
			identifiers.add(parseIdentifier())
			if(currentWord?.type == WordAtom.DOT) {
				consume(WordAtom.DOT)
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
		consume(WordAtom.COLON)
		val type = parseType()
		return TypedIdentifier(identifier, type)
	}

	/**
	 * Identifier:
	 *   <identifier>
	 */
	private fun parseIdentifier(): Identifier {
		return Identifier(consume(WordAtom.IDENTIFIER))
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
		return NullLiteral(consume(WordAtom.NULL_LITERAL))
	}

	/**
	 * BooleanLiteral:
	 *   <number>
	 */
	private fun parseBooleanLiteral(): BooleanLiteral {
		return BooleanLiteral(consume(WordAtom.BOOLEAN_LITERAL))
	}

	/**
	 * NumberLiteral:
	 *   <number>
	 */
	private fun parseNumberLiteral(): NumberLiteral {
		return NumberLiteral(consume(WordAtom.NUMBER_LITERAL))
	}

	/**
	 * StringLiteral:
	 *   <string>
	 */
	private fun parseStringLiteral(): StringLiteral {
		return StringLiteral(consume(WordAtom.STRING_LITERAL))
	}
}