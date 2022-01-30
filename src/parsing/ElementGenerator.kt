package parsing

import parsing.ast.*
import parsing.ast.operations.*
import errors.user.UnexpectedWordError
import errors.user.UnexpectedEndOfFileError
import parsing.ast.access.Index
import parsing.ast.access.ReferenceChain
import parsing.ast.control_flow.*
import parsing.ast.definitions.*
import parsing.ast.general.*
import source_structure.Project
import parsing.ast.literals.*
import parsing.tokenizer.*
import java.util.*

class ElementGenerator(project: Project) {
	private val wordGenerator: WordGenerator
	private var currentWord: Word?
	private var nextWord: Word?
	private var parseForeignLanguageLiteralNext = false

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
		if(parseForeignLanguageLiteralNext) {
			parseForeignLanguageLiteralNext = false
			val foreignLanguage = wordGenerator.getRemainingLine()
			if(foreignLanguage != null) {
				nextWord = foreignLanguage
				return consumedWord
			}
		}
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
	 *   <StatementBlock>
	 *   <FileReference>
	 *   <Print>
	 *   <IfStatement>
	 *   <ReturnStatement>
	 *   <LoopStatement>
	 *   <BreakStatement>
	 *   <NextStatement>
	 *   <ModifiedDefinition>
	 *   <VariableDeclaration>
	 *   <TypeDefinition>
	 *   <Assignment>
	 *   <UnaryModification>
	 *   <BinaryModification>
	 *   <Expression>
	 */
	private fun parseStatement(): Element {
		if(currentWord?.type == WordAtom.BRACES_OPEN)
			return parseStatementBlock()
		if(currentWord?.type == WordAtom.REFERENCING)
			return parseFileReference()
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
		if(WordType.DEFINITION_MODIFIER.includes(currentWord?.type))
			return parseModifiedDefinition()
		if(WordType.VARIABLE_DECLARATION.includes(currentWord?.type))
			return parseVariableDeclaration()
		if(WordType.TYPE_TYPE.includes(currentWord?.type))
			return parseTypeDefinition()
		if(nextWord?.type == WordAtom.ASSIGNMENT)
			return parseAssignment()
		if(WordType.UNARY_MODIFICATION.includes(nextWord?.type))
			return parseUnaryModification()
		return parseExpression()
	}

	/**
	 * ModifiedDefinition:
	 *   <ModifierList> <TypeDefinition>
	 *   <ModifierList> <VariableDeclaration>
	 */
	private fun parseModifiedDefinition(): Element {
		val modifierList = parseModifierList(WordType.DEFINITION_MODIFIER)
		if(WordType.VARIABLE_DECLARATION.includes(currentWord?.type))
			return parseVariableDeclaration(modifierList)
		if(WordType.TYPE_TYPE.includes(currentWord?.type))
			return parseTypeDefinition(modifierList)
		val expectation = "DEFINITION"
		throw UnexpectedWordError(getCurrentWord(expectation), expectation)
	}

	/**
	 * FileReference:
	 *   referencing <ReferenceChain> [<AliasBlock>]
	 */
	private fun parseFileReference(): FileReference {
		val start = consume(WordAtom.REFERENCING).start
		val file = parseReferenceChain()
		val body = parseAliasBlock()
		return FileReference(start, file, body)
	}

	/**
	 * AliasBlock:
	 *   [{[<Alias>\n]...}]
	 */
	private fun parseAliasBlock(): AliasBlock? {
		if(currentWord?.type != WordAtom.BRACES_OPEN)
			return null
		val start = consume(WordAtom.BRACES_OPEN).start
		val aliases = LinkedList<Alias>()
		while(currentWord?.type == WordAtom.LINE_BREAK) {
			while(currentWord?.type == WordAtom.LINE_BREAK)
				consume(WordAtom.LINE_BREAK)
			if(currentWord?.type == WordAtom.IDENTIFIER)
				aliases.add(parseAlias())
		}
		val end = consume(WordAtom.BRACES_CLOSE).end
		return AliasBlock(start, end, aliases)
	}

	/**
	 * Alias:
	 *   <Identifier> as <Identifier>
	 */
	private fun parseAlias(): Alias {
		val originalName = parseIdentifier()
		consume(WordAtom.AS)
		val aliasName = parseIdentifier()
		return Alias(originalName, aliasName)
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
	 * TypeDefinition[modifierList]:
	 *   <TypeType> <Identifier> <InheritanceList> <TypeBody>
	 */
	private fun parseTypeDefinition(modifierList: ModifierList? = null): TypeDefinition {
		val type = parseTypeType()
		val identifier = parseIdentifier()
		val inheritanceList = parseInheritanceList()
		val body = parseTypeBody()
		return TypeDefinition(modifierList, type, identifier, inheritanceList, body)
	}

	/**
	 * InheritanceList:
	 *   [: <Type>[, <Type>]...]
	 */
	private fun parseInheritanceList(): InheritanceList? {
		if(currentWord?.type != WordAtom.COLON)
			return null
		val start = consume(WordAtom.COLON).start
		val parentTypes = LinkedList<Type>()
		parentTypes.add(parseType(false))
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			parentTypes.add(parseType(false))
		}
		return InheritanceList(start, parentTypes)
	}

	/**
	 * ModifierList:
	 *   [<Modifier>[ <Modifier>]...]
	 */
	private fun parseModifierList(allowedModifiers: WordDescriptor): ModifierList? {
		val modifiers = LinkedList<Modifier>()
		while(allowedModifiers.includes(currentWord?.type))
			modifiers.add(parseModifier())
		if(modifiers.isEmpty())
			return null
		return ModifierList(modifiers)
	}

	/**
	 * Modifier:
	 *   <modifier>
	 */
	private fun parseModifier(): Modifier {
		return Modifier(consume(WordType.MODIFIER))
	}

	/**
	 * TypeType:
	 *   <type-type>
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
	 *   <ModifierList> <OperatorDefinition>
	 *   <ModifierList> <PropertyDeclaration>
	 *   <ModifierList> <InitializerDefinition>
	 *   <ModifierList> <FunctionDefinition>
	 *   <ModifierList> <OperatorDefinition>
	 *   <ModifierList> <TypeDefinition>
	 */
	private fun parseMemberDeclaration(): Element {
		if(currentWord?.type == WordAtom.CONTAINING)
			return parseGenericsDeclaration()
		if(currentWord?.type == WordAtom.INSTANCES)
			return parseInstanceList()
		val modifierList = parseModifierList(WordType.DEFINITION_MODIFIER)
		if(WordType.PROPERTY_DECLARATION.includes(currentWord?.type))
			return parsePropertyDeclaration(modifierList)
		if(currentWord?.type == WordAtom.INIT)
			return parseInitializerDefinition(modifierList)
		if(WordType.FUNCTION_DECLARATION.includes(currentWord?.type))
			return parseFunctionDefinition(modifierList)
		if(currentWord?.type == WordAtom.OPERATOR)
			return parseOperatorDeclaration(modifierList)
		if(WordType.TYPE_TYPE.includes(currentWord?.type))
			return parseTypeDefinition(modifierList)
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
	 * InstanceList:
	 *   instances <Identifier>[,<Identifier>]...
	 */
	private fun parseInstanceList(): InstanceList {
		val start = consume(WordAtom.INSTANCES).start
		val types = LinkedList<Element>()
		types.add(parseIdentifier())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			types.add(parseIdentifier())
		}
		return InstanceList(start, types)
	}

	/**
	 * PropertyDeclaration[modifierList]:
	 *   <property-declaration> <VariableDeclarationPart>[,<VariableDeclarationPart>]...
	 */
	private fun parsePropertyDeclaration(modifierList: ModifierList?): PropertyDeclaration {
		val type = consume(WordType.PROPERTY_DECLARATION)
		val declarationParts = LinkedList<Element>()
		if(currentWord?.type == WordAtom.BRACES_OPEN) {
			// Multiline
			consume(WordAtom.BRACES_OPEN)
			while(currentWord?.type != WordAtom.BRACES_CLOSE) {
				consumeLineBreaks()
				if(currentWord?.type == WordAtom.BRACES_CLOSE)
					break
				declarationParts.add(parseVariableDeclarationPart())
			}
			consume(WordAtom.BRACES_CLOSE)
		} else {
			// Single line
			declarationParts.add(parseVariableDeclarationPart())
			while(currentWord?.type == WordAtom.COMMA) {
				consume(WordAtom.COMMA)
				declarationParts.add(parseVariableDeclarationPart())
			}
		}
		return PropertyDeclaration(modifierList, type, declarationParts)
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
	 *   <function-declaration> <Identifier><ParameterList>[: <Type>] [<StatementBlock>]
	 */
	private fun parseFunctionDefinition(modifierList: ModifierList?): Element {
		var start = consume(WordType.FUNCTION_DECLARATION).start
		if(modifierList != null)
			start = modifierList.start
		val identifier = parseIdentifier()
		val parameterList = parseParameterList()
		var returnType: Type? = null
		if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			returnType = parseType()
		}
		val body = if(currentWord?.type == WordAtom.BRACES_OPEN)
			parseStatementBlock()
		else
			null
		return FunctionDefinition(start, modifierList, identifier, parameterList, body, returnType)
	}

	/**
	 * OperatorDeclaration:
	 *   operator <Operator>[<ParameterList>][: <Type>] [<StatementBlock>]
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
		val body = if(currentWord?.type == WordAtom.BRACES_OPEN)
			parseStatementBlock()
		else
			null
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
	 *   ([<Parameter>[, <Parameter>]])
	 */
	private fun parseParameterList(): ParameterList {
		val start = consume(WordAtom.PARENTHESES_OPEN).start
		val parameters = LinkedList<Parameter>()
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
	 * TypeList:
	 *   [<<TypeParameter>[, <TypeParameter>]...>]
	 */
	private fun parseTypeList(): TypeList? {
		if(WordType.GENERICS_START.includes(currentWord?.type)) {
			val types = LinkedList<TypeParameter>()
			val start = consume(WordType.GENERICS_START).start
			types.add(parseTypeParameter())
			while(currentWord?.type == WordAtom.COMMA) {
				consume(WordAtom.COMMA)
				types.add(parseTypeParameter())
			}
			val end = consume(WordType.GENERICS_END).end
			return TypeList(types, start, end)
		}
		return null
	}

	/**
	 * TypeParameter:
	 *   <Type>[ <GenericModifier>]
	 */
	private fun parseTypeParameter(): TypeParameter {
		val type = parseType()
		var genericModifier: GenericModifier? = null
		if(WordType.GENERICS_MODIFIER.includes(currentWord?.type)) {
			genericModifier = parseGenericModifier()
		}
		return TypeParameter(type, genericModifier)
	}

	/**
	 * GenericModifier:
	 *   <generic-modifier>
	 */
	private fun parseGenericModifier(): GenericModifier {
		return GenericModifier(consume(WordType.GENERICS_MODIFIER))
	}

	/**
	 * Parameter:
	 *   <ModifierList> <TypedIdentifier>
	 */
	private fun parseParameter(): Parameter {
		val modifierList = parseModifierList(WordType.PARAMETER_MODIFIER)
		val identifier = parseTypedIdentifier()
		return Parameter(modifierList, identifier)
	}

	/**
	 * VariableDeclaration[modifierList]:
	 *   <variable-declaration> <VariableDeclarationPart>[,<VariableDeclarationPart>]...
	 */
	private fun parseVariableDeclaration(modifierList: ModifierList? = null): VariableDeclaration {
		val type = consume(WordType.VARIABLE_DECLARATION)
		val declarationParts = LinkedList<Element>()
		declarationParts.add(parseVariableDeclarationPart())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			declarationParts.add(parseVariableDeclarationPart())
		}
		return VariableDeclaration(modifierList, type, declarationParts)
	}

	/**
	 * VariableDeclarationPart:
	 *   <TypedIdentifier> [get <Expression>] [set <Expression>]
	 *   <Assignment:declare>
	 */
	private fun parseVariableDeclarationPart(): Element {
		if(nextWord?.type == WordAtom.ASSIGNMENT)
			return parseAssignment(true)
		val identifier = parseTypedIdentifier()
		if(currentWord?.type == WordAtom.ASSIGNMENT) {
			consume(WordAtom.ASSIGNMENT)
			val value = parseExpression()
			return Assignment(listOf(identifier), value)
		}
		var getExpression: Element? = null
		var setExpression: Element? = null
		consumeLineBreaks()
		if(currentWord?.type == WordAtom.GET) {
			consume(WordAtom.GET)
			getExpression = parseExpression()
		}
		consumeLineBreaks()
		if(currentWord?.type == WordAtom.SET) {
			consume(WordAtom.SET)
			setExpression = parseExpression()
		}
		if(getExpression != null || setExpression != null)
			return ComputedProperty(identifier, getExpression, setExpression)
		return identifier
	}

	/**
	 * Assignment:
	 *   <Identifier> = <Expression>
	 * Assignment[declare]:
	 *   <OptionallyTypedAssignment>
	 */
	private fun parseAssignment(declare: Boolean = false): Element {
		if(declare && nextWord?.type == WordAtom.COLON)
			return parseOptionallyTypedAssignment()
		val targets = LinkedList<Element>()
		var lastExpression = parseReferenceChain()
		do {
			consume(WordAtom.ASSIGNMENT)
			targets.add(lastExpression)
			lastExpression = parseExpression()
			val isExpressionAssignable = lastExpression is Identifier || lastExpression is ReferenceChain
		} while(isExpressionAssignable && currentWord?.type == WordAtom.ASSIGNMENT)
		return Assignment(targets, lastExpression)
	}

	/**
	 * OptionallyTypedAssignment:
	 *   <OptionallyTypedIdentifier> = <Expression>
	 */
	private fun parseOptionallyTypedAssignment(): Element {
		val identifier = parseOptionallyTypedIdentifier()
		consume(WordAtom.ASSIGNMENT)
		val expression = parseExpression()
		return Assignment(listOf(identifier), expression)
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
	 * Expression:
	 *   <BinaryModification>
	 */
	private fun parseExpression(): Element {
		return parseBinaryModification()
	}

	/**
	 * BinaryModification:
	 *   <BinaryBooleanExpression>
	 *   <BinaryBooleanExpression> += <BinaryBooleanExpression>
	 *   <BinaryBooleanExpression> -= <BinaryBooleanExpression>
	 *   <BinaryBooleanExpression> *= <BinaryBooleanExpression>
	 *   <BinaryBooleanExpression> /= <BinaryBooleanExpression>
	 */
	private fun parseBinaryModification(): Element {
		var expression = parseBinaryBooleanExpression()
		if(WordType.BINARY_MODIFICATION.includes(currentWord?.type)) {
			val operator = consume(WordType.BINARY_MODIFICATION)
			val value = parseBinaryBooleanExpression()
			expression = BinaryModification(expression, value, operator.getValue())
		}
		return expression
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
	 *   <UnaryOperator>
	 *   <UnaryOperator> as <Type>
	 *   <UnaryOperator> as? <Type>
	 *   <UnaryOperator> as! <Type>
	 *   <UnaryOperator> is <TypedIdentifier>
	 *   <UnaryOperator> !is <TypedIdentifier>
	 */
	private fun parseCast(): Element {
		var expression: Element = parseUnaryOperator()
		if(WordType.CAST.includes(currentWord?.type)) {
			val operator = consume(WordType.CAST)
			val type = if(nextWord?.type == WordAtom.COLON)
				parseTypedIdentifier()
			else
				parseType()
			expression = Cast(expression, operator.getValue(), type)
		}
		return expression
	}

	/**
	 * UnaryOperator:
	 *   <Primary>
	 *   !<Primary>
	 *   +<Primary>
	 *   -<Primary>
	 *   ...<Primary>
	 */
	private fun parseUnaryOperator(): Element {
		if(WordType.UNARY_OPERATOR.includes(currentWord?.type)) {
			val operator = consume(WordType.UNARY_OPERATOR)
			return UnaryOperator(parseIndex(), operator)
		}
		return parseIndex()
	}

	/**
	 * Index:
	 *   <Primary>
	 *   <Primary>[<Expression>[, <Expression>]...]
	 */
	private fun parseIndex(): Element {
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
	 *   <FunctionCall>
	 *   <ForeignLanguageExpression>
	 */
	private fun parseAtom(): Element {
		val word = getCurrentWord("atom")
		return when(word.type) {
			WordAtom.NULL_LITERAL -> parseNullLiteral()
			WordAtom.BOOLEAN_LITERAL -> parseBooleanLiteral()
			WordAtom.NUMBER_LITERAL -> parseNumberLiteral()
			WordAtom.STRING_LITERAL -> parseStringLiteral()
			WordAtom.IDENTIFIER -> {
				if(nextWord?.type == WordAtom.DOUBLE_COLON)
					parseForeignLanguageExpression()
				else
					parseFunctionCall()
			}
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
	 * ForeignLanguageExpression:
	 *   <Identifier>::<ForeignLanguageLiteral>
	 */
	private fun parseForeignLanguageExpression(): ForeignLanguageExpression {
		parseForeignLanguageLiteralNext = true
		val identifier = parseIdentifier()
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
	 * ReferenceChain:
	 *   <Identifier>[.<Identifier>]...
	 */
	private fun parseReferenceChain(): Element {
		return if(nextWord?.type == WordAtom.DOT) {
			val identifiers = LinkedList<Identifier>()
			identifiers.add(parseIdentifier())
			while(currentWord?.type == WordAtom.DOT) {
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
	 *   [...]<TypeList><Identifier>[?]
	 */
	private fun parseType(optionalAllowed: Boolean = true): Type {
		var hasDynamicQuantity = false
		if(currentWord?.type == WordAtom.TRIPLE_DOT) {
			consume(WordAtom.TRIPLE_DOT)
			hasDynamicQuantity = true
		}
		val typeList = parseTypeList()
		val identifier = parseIdentifier()
		var isOptional = false
		if(optionalAllowed && currentWord?.type == WordAtom.QUESTION_MARK) {
			consume(WordAtom.QUESTION_MARK)
			isOptional = true
		}
		return Type(identifier, hasDynamicQuantity, isOptional, typeList)
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