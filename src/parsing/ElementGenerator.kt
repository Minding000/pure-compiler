package parsing

import parsing.ast.*
import parsing.ast.operations.*
import errors.user.UnexpectedWordError
import errors.user.UnexpectedEndOfFileError
import linter.elements.Parameter
import parsing.ast.access.ReferenceChain
import parsing.ast.control_flow.*
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
	 *   <LoopStatement>
	 *   <BreakStatement>
	 *   <NextStatement>
	 *   <VariableDeclaration>
	 *   <TypeDefinition>
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
		if(currentWord?.type == WordAtom.LOOP)
			return parseLoopStatement()
		if(currentWord?.type == WordAtom.BREAK)
			return parseBreakStatement()
		if(currentWord?.type == WordAtom.NEXT)
			return parseNextStatement()
		if(currentWord?.type == WordAtom.VAR)
			return parseVariableDeclaration()
		if(WordType.MODIFIER.includes(currentWord?.type) || WordType.TYPE_TYPE.includes(currentWord?.type))
			return parseTypeDefinition()
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
	 * LoopStatement:
	 *   loop <StatementBlock>
	 */
	private fun parseLoopStatement(): LoopStatement {
		val start = consume(WordAtom.LOOP).start
		val body = parseStatementBlock()
		return LoopStatement(start, body)
	}

	/**
	 * StatementBlock:
	 *   { [<Statement>\n]... }
	 */
	private fun parseStatementBlock(): StatementBlock {
		val start = consume(WordAtom.BRACES_OPEN).start
		val statements = LinkedList<Element>()
		while(currentWord?.type != WordAtom.BRACES_CLOSE) {
			consumeLineBreaks()
			if(currentWord?.type == WordAtom.BRACES_CLOSE)
				break
			statements.add(parseStatement())
		}
		val end = consume(WordAtom.BRACES_CLOSE).end
		return StatementBlock(start, end, statements)
	}

	/**
	 * BreakStatement:
	 *   break
	 */
	private fun parseBreakStatement(): BreakStatement {
		return BreakStatement(consume(WordAtom.BREAK))
	}

	/**
	 * NextStatement:
	 *   next
	 */
	private fun parseNextStatement(): NextStatement {
		return NextStatement(consume(WordAtom.NEXT))
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
	 * TypeDefinition:
	 *   <ModifierList> <TypeType> <Identifier> <TypeBody>
	 */
	private fun parseTypeDefinition(): TypeDefinition {
		val modifierList = parseModifierList()
		val type = parseTypeType()
		val identifier = parseIdentifier()
		val body = parseTypeBody()
		return TypeDefinition(modifierList, type, identifier, body)
	}

	/**
	 * ModifierList:
	 *   [<Modifier>[ <Modifier>]...]
	 */
	private fun parseModifierList(): ModifierList? {
		val modifiers = LinkedList<Modifier>()
		while(WordType.MODIFIER.includes(currentWord?.type))
			modifiers.add(parseModifier())
		if(modifiers.isEmpty())
			return null
		return ModifierList(modifiers)
	}

	/**
	 * Modifier:
	 *   native
	 */
	private fun parseModifier(): Modifier {
		return Modifier(consume(WordType.MODIFIER))
	}

	/**
	 * TypeType:
	 *   class
	 *   generic
	 *   object
	 */
	private fun parseTypeType(): TypeType {
		return TypeType(consume(WordType.TYPE_TYPE))
	}

	/**
	 * TypeBody:
	 *   { [<MemberDeclaration>\n]... }
	 */
	private fun parseTypeBody(): TypeBody {
		val start = consume(WordAtom.BRACES_OPEN).start
		val members = LinkedList<Element>()
		while(currentWord?.type != WordAtom.BRACES_CLOSE) {
			consumeLineBreaks()
			if(currentWord?.type == WordAtom.BRACES_CLOSE)
				break
			members.add(parseMemberDeclaration())
		}
		val end = consume(WordAtom.BRACES_CLOSE).end
		return TypeBody(start, end, members)
	}

	/**
	 * MemberDeclaration:
	 *   <GenericsDeclaration>
	 *   <ModifierList> <PropertyDeclaration>
	 *   <ModifierList> <InitializerDefinition>
	 *   <ModifierList> <FunctionDefinition>
	 *   <ModifierList> <OperatorDefinition>
	 */
	private fun parseMemberDeclaration(): Element {
		if(currentWord?.type == WordAtom.CONTAINING)
			return parseGenericsDeclaration()
		val modifierList = parseModifierList()
		if(currentWord?.type == WordAtom.VAR)
			return parsePropertyDeclaration(modifierList)
		if(currentWord?.type == WordAtom.INIT)
			return parseInitializerDefinition(modifierList)
		if(currentWord?.type == WordAtom.FUN)
			return parseFunctionDefinition(modifierList)
		if(currentWord?.type == WordAtom.OPERATOR)
			return parseOperatorDeclaration(modifierList)
		throw UnexpectedWordError(getCurrentWord(WordType.MEMBER), WordType.MEMBER)
	}

	/**
	 * GenericsDeclaration:
	 *   containing <TypedIdentifier>[,<TypedIdentifier>]...
	 */
	private fun parseGenericsDeclaration(): GenericsDeclaration {
		val start = consume(WordAtom.CONTAINING).start
		val types = LinkedList<Element>()
		types.add(parseOptionallyTypedIdentifier())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			types.add(parseOptionallyTypedIdentifier())
		}
		return GenericsDeclaration(start, types)
	}

	/**
	 * PropertyDeclaration:
	 *   <ModifierList> var <VariableDeclarationPart>[,<VariableDeclarationPart>]...
	 */
	private fun parsePropertyDeclaration(modifierList: ModifierList?): PropertyDeclaration {
		var start = consume(WordAtom.VAR).start
		if(modifierList != null)
			start = modifierList.start
		val declarationParts = LinkedList<Element>()
		declarationParts.add(parseVariableDeclarationPart())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			declarationParts.add(parseVariableDeclarationPart())
		}
		return PropertyDeclaration(start, declarationParts.last.end, modifierList, declarationParts)
	}

	/**
	 * InitializerDeclaration:
	 *   init([<OptionallyTypedIdentifier>[,<OptionallyTypedIdentifier>]]) <StatementBlock>
	 */
	private fun parseInitializerDefinition(modifierList: ModifierList?): Element {
		var start = consume(WordAtom.INIT).start
		if(modifierList != null)
			start = modifierList.start
		consume(WordAtom.PARENTHESES_OPEN)
		val parameters = LinkedList<Element>()
		if(currentWord?.type != WordAtom.PARENTHESES_CLOSE)
			parameters.add(parseOptionallyTypedIdentifier())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			parameters.add(parseOptionallyTypedIdentifier())
		}
		val parameterListEnd = consume(WordAtom.PARENTHESES_CLOSE).end
		val body = if(currentWord?.type == WordAtom.BRACES_OPEN) parseStatementBlock() else null
		return InitializerDefinition(start, body?.end ?: parameterListEnd, modifierList, parameters, body)
	}

	/**
	 * FunctionDeclaration:
	 *   fun <Identifier><ParameterList>[: <Type>] <StatementBlock>
	 */
	private fun parseFunctionDefinition(modifierList: ModifierList?): Element {
		var start = consume(WordAtom.FUN).start
		if(modifierList != null)
			start = modifierList.start
		val identifier = parseIdentifier()
		val parameterList = parseParameterList()
		var returnType: Type? = null
		if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			returnType = parseType()
		}
		val body = parseStatementBlock()
		return FunctionDefinition(start, modifierList, identifier, parameterList, body, returnType)
	}

	/**
	 * OperatorDeclaration:
	 *   operator <Operator>[<ParameterList>][: <Type>] <StatementBlock>
	 */
	private fun parseOperatorDeclaration(modifierList: ModifierList?): Element {
		var start = consume(WordAtom.OPERATOR).start
		if(modifierList != null)
			start = modifierList.start
		val operator = parseOperator()
		val parameterList = if(currentWord?.type == WordAtom.PARENTHESES_OPEN)
			parseParameterList()
		else
			null
		var returnType: Type? = null
		if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			returnType = parseType()
		}
		val body = parseStatementBlock()
		return OperatorDefinition(start, modifierList, operator, parameterList, body, returnType)
	}

	/**
	 * Operator:
	 *   <operator>
	 *   [<TypedIdentifier[, <TypedIdentifier>]>]
	 */
	private fun parseOperator(): Operator {
		if(currentWord?.type == WordAtom.BRACKETS_OPEN) {
			val start = consume(WordAtom.BRACKETS_OPEN).start
			val parameters = LinkedList<TypedIdentifier>()
			if(currentWord?.type != WordAtom.BRACKETS_CLOSE)
				parameters.add(parseTypedIdentifier())
			while(currentWord?.type == WordAtom.COMMA) {
				consume(WordAtom.COMMA)
				parameters.add(parseTypedIdentifier())
			}
			val end = consume(WordAtom.BRACKETS_CLOSE).end
			return IndexOperator(start, end, parameters)
		}
		return Operator(consume(WordType.OPERATOR))
	}

	/**
	 * ParameterList:
	 *   ([<TypedIdentifier>[, <TypedIdentifier>]])
	 */
	private fun parseParameterList(): ParameterList {
		val start = consume(WordAtom.PARENTHESES_OPEN).start
		val parameters = LinkedList<TypedIdentifier>()
		if(currentWord?.type != WordAtom.PARENTHESES_CLOSE)
			parameters.add(parseParameter())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			parameters.add(parseParameter())
		}
		val end = consume(WordAtom.PARENTHESES_CLOSE).end
		return ParameterList(start, end, parameters)
	}

	/**
	 * GenericsList:
	 *   [<<Identifier>[, <Identifier>]...>]
	 */
	private fun parseGenericsList(): GenericsList? {
		if(WordType.GENERICS_START.includes(currentWord?.type)) {
			val identifiers = LinkedList<Identifier>()
			val start = consume(WordType.GENERICS_START).start
			identifiers.add(parseIdentifier())
			while(currentWord?.type == WordAtom.COMMA) {
				consume(WordAtom.COMMA)
				identifiers.add(parseIdentifier())
			}
			val end = consume(WordType.GENERICS_END).end
			return GenericsList(identifiers, start, end)
		}
		return null
	}

	/**
	 * TypeList:
	 *   [<<Type>[, <Type>]...>]
	 */
	private fun parseTypeList(): TypeList? {
		if(WordType.GENERICS_START.includes(currentWord?.type)) {
			val types = LinkedList<Type>()
			val start = consume(WordType.GENERICS_START).start
			types.add(parseType())
			while(currentWord?.type == WordAtom.COMMA) {
				consume(WordAtom.COMMA)
				types.add(parseType())
			}
			val end = consume(WordType.GENERICS_END).end
			return TypeList(types, start, end)
		}
		return null
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
	 *   <OptionallyTypedIdentifier> = <Expression>
	 */
	private fun parseAssignment(declare: Boolean = false): Assignment {
		val identifier = if(declare)
			parseOptionallyTypedIdentifier()
		else
			parseReferenceChain()
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
			else -> {
				if(WordType.GENERICS_START.includes(word.type))
					parseFunctionCall()
				else
					throw UnexpectedWordError(word, "atom")
			}
		}
	}

	/**
	 * FunctionCall:
	 *   [<TypeList>]<IdentifierExpression>[([<Expression>[, <Expression>]...])]
	 */
	private fun parseFunctionCall(): Element {
		val typeList = parseTypeList()
		val identifierReference = parseReferenceChain()
		if(typeList !== null || currentWord?.type == WordAtom.PARENTHESES_OPEN) {
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
			return FunctionCall(typeList, identifierReference, parameters, identifierReference.start, end)
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
	 * OptionallyTypedIdentifier:
	 *   <Identifier>
	 *   <TypedIdentifier>
	 */
	private fun parseOptionallyTypedIdentifier(): Element {
		return if(nextWord?.type == WordAtom.COLON)
			parseTypedIdentifier()
		else
			parseIdentifier()
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
	 *   <TypeList><Identifier>
	 */
	private fun parseType(): Type {
		val typeList = parseTypeList()
		val identifier = parseIdentifier()
		return Type(identifier, typeList)
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