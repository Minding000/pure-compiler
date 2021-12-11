package code

import word_generation.Word
import word_generation.WordGenerator
import word_generation.WordType
import elements.*
import elements.control_flow.FunctionCall
import elements.control_flow.IfStatement
import elements.definitions.ClassDefinition
import elements.literals.NumberLiteral
import elements.literals.StringLiteral
import elements.operations.*
import errors.user.UnexpectedWordError
import elements.definitions.FunctionDefinition
import elements.identifier.*
import errors.internal.ParserError
import errors.user.TypeMismatchError
import errors.user.UnexpectedEndOfFileError
import scopes.*
import source_structure.Project
import types.NativeTypes
import types.Type
import java.util.*

class ElementGenerator(project: Project) {
	private val wordGenerator: WordGenerator
	private val identifierReferences = LinkedList<IdentifierReference<out Identifier>>()
	private val scopeDeque = ArrayDeque<Scope>()
	private val globalScope = GlobalScope(null)
	private var currentScope: Scope = globalScope
	private var currentWord: Word?
	private var nextWord: Word?

	init {
		wordGenerator = WordGenerator(project)
		scopeDeque.push(globalScope)
		currentWord = wordGenerator.getNextWord()
		nextWord = wordGenerator.getNextWord()
	}

	private fun pushScope(scope: SubScope) {
		currentScope.childScopes.add(scope)
		scopeDeque.push(scope)
		currentScope = scope
	}

	private fun popScope(scope: SubScope) {
		val removedScope = scopeDeque.pop()
		currentScope = scope.parentScope
		if(removedScope != scope)
			throw ParserError("Popped wrong scope.")
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

	private fun checkTypeCompatibility(required: Type?, provided: Type?) {
		if(required != provided)
			throw TypeMismatchError(required, provided)
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
		for(identifierReference in identifierReferences)
			identifierReference.resolve()
		return Program(statements)
	}

	/**
	 * Statement:
	 *   <Print>
	 *   <IfStatement>
	 *   <VariableDeclaration>
	 *   <ClassDefinition>
	 *   <Assignment>
	 *   <Expression>
	 */
	private fun parseStatement(): Element {
		if(currentWord?.type == WordType.ECHO)
			return parsePrint()
		if(currentWord?.type == WordType.IF)
			return parseIfStatement()
		if(currentWord?.type == WordType.VAR)
			return parseVariableDeclaration()
		if(currentWord?.type == WordType.CLASS)
			return parseClassDefinition()
		if(nextWord?.type == WordType.ASSIGNMENT)
			return parseAssignment()
		return parseExpression()
	}

	/**
	 * IfStatement:
	 *   if <condition>
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
	 * Print:
	 *   echo <Atom>[,<Atom>]...
	 */
	private fun parsePrint(): Print {
		val start = consume(WordType.ECHO).start
		val elements = LinkedList<ValueElement>()
		elements.add(parseAtom())
		while(currentWord?.type == WordType.COMMA) {
			consume(WordType.COMMA)
			elements.add(parseAtom())
		}
		return Print(start, elements.last.end, elements)
	}

	/**
	 * ClassDefinition:
	 *   class <ClassIdentifier> { [<MemberDeclaration>\n]... }
	 */
	private fun parseClassDefinition(): ClassDefinition {
		val start = consume(WordType.CLASS).start
		val identifier = parseClassIdentifier()
		consume(WordType.BRACES_OPEN)
		val scope = ClassScope(globalScope)
		pushScope(scope)
		val members = LinkedList<Element>()
		while(currentWord?.type != WordType.BRACES_CLOSE) {
			consumeLineBreaks()
			if(currentWord?.type == WordType.BRACES_CLOSE)
				break
			members.add(parseMemberDeclaration())
		}
		popScope(scope)
		val end = consume(WordType.BRACES_CLOSE).end
		val classDefinition = ClassDefinition(start, end, identifier, scope, members)
		identifier.definition = classDefinition
		return classDefinition
	}

	/**
	 * MemberDeclaration:
	 *   <PropertyDeclaration>
	 *   <FunctionDefinition>
	 */
	private fun parseMemberDeclaration(): VoidElement {
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
	 *   fun <FunctionIdentifier>(<Parameter>[,<Parameter>])[: <Type>] { [<Statement>\n]... }
	 */
	private fun parseFunctionDefinition(): VoidElement {
		val start = consume(WordType.FUN).start
		val identifier = parseVariableIdentifier(typeRequired = false, typeDisallowed = true)
		consume(WordType.PARENTHESES_OPEN)
		val scope = FunctionScope(currentScope as? ClassScope ?: throw ParserError("Cannot add function to non-class scope."))
		pushScope(scope)
		val parameters = LinkedList<VariableIdentifier>()
		parameters.add(parseParameter())
		while(currentWord?.type == WordType.COMMA) {
			consume(WordType.COMMA)
			parameters.add(parseParameter())
		}
		consume(WordType.PARENTHESES_CLOSE)
		var returnType: Type? = null
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
		popScope(scope)
		val end = consume(WordType.BRACES_CLOSE).end
		return FunctionDefinition(start, end, identifier, parameters, statements, returnType)
	}

	/**
	 * Parameter:
	 *   <VariableDefinition:typeRequired>
	 */
	private fun parseParameter(): VariableIdentifier {
		return parseVariableIdentifier(true)
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
	 *   <VariableDefinition:typeRequired>
	 *   <Assignment:declare>
	 */
	private fun parseVariableDeclarationPart(): Element {
		if(nextWord?.type == WordType.ASSIGNMENT)
			return parseAssignment(true)
		return parseVariableIdentifier(true)
	}

	/**
	 * Assignment:
	 *   <VariableReference> = <Expression>
	 * Assignment[declare]:
	 *   <VariableIdentifier> = <Expression>
	 */
	private fun parseAssignment(declare: Boolean = false): Assignment {
		val identifier = if(declare)
				parseVariableIdentifier()
			else
				parseVariableReference()
		consume(WordType.ASSIGNMENT)
		val expression = parseExpression()
		if(identifier is VariableIdentifier) {
			if(identifier.type == null)
				identifier.type = expression.type
			else
				checkTypeCompatibility(identifier.type, expression.type)
		}
		return Assignment(identifier, expression)
	}

	/**
	 * Expression:
	 *   <Addition>
	 */
	private fun parseExpression(): ValueElement {
		return parseAddition()
	}

	/**
	 * Addition:
	 *   <Multiplication>
	 *   <Addition> + <Atom>
	 *   <Addition> - <Atom>
	 */
	private fun parseAddition(): ValueElement {
		var addition: ValueElement = parseMultiplication()
		while(currentWord?.type == WordType.ADDITION) {
			val operator = consume(WordType.ADDITION)
			addition = Addition(addition, parseMultiplication(), operator.getValue() == "-")
		}
		return addition
	}

	/**
	 * Multiplication:
	 *   <Exponentiation>
	 *   <Multiplication> * <Atom>
	 *   <Multiplication> / <Atom>
	 */
	private fun parseMultiplication(): ValueElement {
		var multiplication: ValueElement = parseExponentiation()
		while(currentWord?.type == WordType.MULTIPLICATION) {
			val operator = consume(WordType.MULTIPLICATION)
			multiplication = Multiplication(multiplication, parsePrimary(), operator.getValue() == "/")
		}
		return multiplication
	}

	/**
	 * Exponentiation:
	 *   <Atom>
	 *   <Exponentiation> ^ <Atom>
	 */
	private fun parseExponentiation(): ValueElement {
		var exponentiation: ValueElement = parsePrimary()
		while(currentWord?.type == WordType.EXPONENTIATION) {
			consume(WordType.EXPONENTIATION)
			exponentiation = Exponentiation(exponentiation, parsePrimary())
		}
		return exponentiation
	}

	/**
	 * Primary:
	 *   <Atom>
	 *   (<Expression>)
	 */
	private fun parsePrimary(): ValueElement {
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
	 *   <NumberLiteral>
	 *   <StringLiteral>
	 *   <FunctionCall>
	 */
	private fun parseAtom(): ValueElement {
		val word = getCurrentWord("atom")
		return when(word.type) {
			WordType.NUMBER_LITERAL -> parseNumberLiteral()
			WordType.STRING_LITERAL -> parseStringLiteral()
			WordType.IDENTIFIER -> parseFunctionCall()
			else -> throw UnexpectedWordError(word, "atom")
		}
	}

	/**
	 * FunctionCall:
	 *   <IdentifierExpression>[([<Expression>[, <Expression>]])]
	 */
	private fun parseFunctionCall(): ValueElement {
		val identifierReference = parseIdentifierExpression()
		if(currentWord?.type == WordType.PARENTHESES_OPEN) {
			consume(WordType.PARENTHESES_OPEN)
			val parameters = LinkedList<ValueElement>()
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
	 * IdentifierExpression:
	 *   <IdentifierExpression>
	 *   <IdentifierReference>
	 */
	private fun parseIdentifierExpression(): IdentifierReference<out Identifier> {
		var element = parseIdentifierReference()
		if(currentWord?.type == WordType.DOT) {
			consume(WordType.DOT)
			val expression = parseIdentifierExpression()
			expression.context = element
			element = expression
		}
		return element
	}

	/**
	 * VariableIdentifier:
	 *   <variable-identifier>[: <Type>]
	 * VariableIdentifier[typeRequired]:
	 *   <variable-identifier>: <Type>
	 * VariableIdentifier[typeDisallowed]:
	 *   <variable-identifier>
	 */
	private fun parseVariableIdentifier(typeRequired: Boolean = false, typeDisallowed: Boolean = false): VariableIdentifier {
		val word = consume(WordType.IDENTIFIER)
		val identifier = VariableIdentifier(currentScope, word)
		currentScope.declareIdentifier(identifier)
		if((typeRequired || currentWord?.type == WordType.COLON) && !typeDisallowed) {
			consume(WordType.COLON)
			identifier.type = parseType()
		}
		return identifier
	}

	/**
	 * IdentifierReference:
	 *   <VariableReference>
	 *   <ClassReference>
	 */
	private fun parseIdentifierReference(): IdentifierReference<out Identifier> {
		if(currentWord?.getValue()?.first()?.isUpperCase() == true) {
			return parseClassReference()
		}
		return parseVariableReference()
	}

	/**
	 * VariableReference:
	 *   <variable-identifier>
	 */
	private fun parseVariableReference(): VariableReference {
		val word = consume(WordType.IDENTIFIER)
		val reference = VariableReference(currentScope, word)
		identifierReferences.add(reference)
		return reference
	}

	/**
	 * ClassIdentifier:
	 *   <class-identifier>
	 */
	private fun parseClassIdentifier(): ClassIdentifier {
		val word = consume(WordType.IDENTIFIER)
		val identifier = ClassIdentifier(currentScope, word)
		currentScope.declareIdentifier(identifier)
		return identifier
	}

	/**
	 * ClassReference:
	 *   <class-identifier>
	 */
	private fun parseClassReference(): ClassReference {
		val word = consume(WordType.IDENTIFIER)
		val reference = ClassReference(currentScope, word)
		identifierReferences.add(reference)
		return reference
	}

	/**
	 * Type:
	 *   <NativeType>
	 *   <ClassReference>
	 */
	private fun parseType(): Type {
		val type = NativeTypes.getByName(getCurrentWord(WordType.IDENTIFIER).getValue())
		if(type != null) {
			consume(WordType.IDENTIFIER)
			return type
		}
		return parseClassReference()
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