package parsing.element_generator

import parsing.ast.*
import parsing.ast.operations.*
import errors.user.UnexpectedWordError
import parsing.ast.access.Index
import parsing.ast.access.MemberAccess
import parsing.ast.control_flow.*
import parsing.ast.definitions.*
import parsing.ast.general.*
import parsing.ast.literals.*
import parsing.tokenizer.*
import java.util.*

class StatementParser(private val elementGenerator: ElementGenerator): Generator() {
	override var currentWord: Word?
		get() = elementGenerator.currentWord
		set(value) { elementGenerator.currentWord = value }
	override var nextWord: Word?
		get() = elementGenerator.nextWord
		set(value) { elementGenerator.nextWord = value }
	override var parseForeignLanguageLiteralNext: Boolean
		get() = elementGenerator.parseForeignLanguageLiteralNext
		set(value) { elementGenerator.parseForeignLanguageLiteralNext = value }

	private val expressionParser
		get() = elementGenerator.expressionParser
	private val typeParser
		get() = elementGenerator.typeParser
	private val literalParser
		get() = elementGenerator.literalParser

	override fun consume(type: WordDescriptor): Word {
		return elementGenerator.consume(type)
	}

	private fun isExpressionAssignable(expression: Element): Boolean {
		return expression is Identifier
				|| expression is MemberAccess
				|| expression is Index
	}

	private fun parseExpression(): Element {
		return expressionParser.parseExpression()
	}

	private fun parseRequiredType(): Type {
		return typeParser.parseType(false)
	}

	private fun parseIdentifier(): Identifier {
		return literalParser.parseIdentifier()
	}

	/**
	 * Statement:
	 *   <StatementSection>
	 *   <FileReference>
	 *   <Print>
	 *   <IfStatement>
	 *   <ReturnStatement>
	 *   <RaiseStatement>
	 *   <LoopStatement>
	 *   <BreakStatement>
	 *   <NextStatement>
	 *   <ModifiedDefinition>
	 *   <VariableDeclaration>
	 *   <TypeDefinition>
	 *   <Expression>
	 *   <Expression><unary-modification>
	 *   <Expression> = <Expression>
	 *   <Expression> <binary-modification> <Expression>
	 */
	fun parseStatement(): Element {
		if(currentWord?.type == WordAtom.BRACES_OPEN)
			return parseStatementSection()
		if(currentWord?.type == WordAtom.REFERENCING)
			return parseFileReference()
		if(currentWord?.type == WordAtom.ECHO)
			return parsePrint()
		if(currentWord?.type == WordAtom.IF)
			return parseIfStatement()
		if(currentWord?.type == WordAtom.SWITCH)
			return parseSwitchStatement()
		if(currentWord?.type == WordAtom.RETURN)
			return parseReturnStatement()
		if(currentWord?.type == WordAtom.RAISE)
			return parseRaiseStatement()
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

		val statement = expressionParser.parseExpression()
		if(isExpressionAssignable(statement)) {
			if(currentWord?.type == WordAtom.ASSIGNMENT) {
				val targets = LinkedList<Element>()
				var lastExpression = statement
				do {
					consume(WordAtom.ASSIGNMENT)
					targets.add(lastExpression)
					lastExpression = expressionParser.parseExpression()
				} while(isExpressionAssignable(lastExpression) && currentWord?.type == WordAtom.ASSIGNMENT)
				return Assignment(targets, lastExpression)
			}
			if(WordType.BINARY_MODIFICATION.includes(currentWord?.type)) {
				val operator = consume(WordType.BINARY_MODIFICATION)
				val value = expressionParser.parseExpression()
				return BinaryModification(statement, value, operator.getValue())
			}
			if(WordType.UNARY_MODIFICATION.includes(currentWord?.type)) {
				val operator = consume(WordType.UNARY_MODIFICATION)
				return UnaryModification(statement, operator)
			}
		}
		return statement
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
	 *   referencing <Identifier>[.<Identifier>]... [<AliasBlock>]
	 */
	private fun parseFileReference(): FileReference {
		val start = consume(WordAtom.REFERENCING).start
		val parts = LinkedList<Identifier>()
		parts.add(parseIdentifier())
		while(currentWord?.type == WordAtom.DOT) {
			consume(WordAtom.DOT)
			parts.add(parseIdentifier())
		}
		val body = parseAliasBlock()
		return FileReference(start, parts, body)
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
	 * SwitchStatement:
	 *   switch <Expression> {
	 *       <Expression>: <Statement>
	 *       [else: <Statement>]
	 *   }
	 */
	private fun parseSwitchStatement(isExpression: Boolean = false): SwitchStatement {
		val start = consume(WordAtom.SWITCH).start
		val condition = parseExpression()
		consume(WordAtom.BRACES_OPEN)
		consumeLineBreaks()
		val cases = LinkedList<Case>()
		var elseResult: Element? = null
		while(currentWord?.type !== WordAtom.BRACES_CLOSE) {
			consumeLineBreaks()
			if(currentWord?.type == WordAtom.ELSE) {
				consume(WordAtom.ELSE)
				consume(WordAtom.COLON)
				consumeLineBreaks()
				elseResult = if(isExpression) parseExpression() else parseStatement()
				break
			}
			cases.add(parseCase(isExpression))
		}
		consumeLineBreaks()
		val end = consume(WordAtom.BRACES_CLOSE).end
		return SwitchStatement(condition, cases, elseResult, start, end)
	}

	/**
	 * Case:
	 *   <Expression>: <Statement>
	 */
	private fun parseCase(isExpression: Boolean): Case {
		val condition = parseExpression()
		consume(WordAtom.COLON)
		consumeLineBreaks()
		val result = if(isExpression) parseExpression() else parseStatement()
		return Case(condition, result)
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
	 * RaiseStatement:
	 *   raise <Expression>
	 */
	private fun parseRaiseStatement(): RaiseStatement {
		val word = consume(WordAtom.RAISE)
		val value = parseExpression()
		return RaiseStatement(value, word.start)
	}

	/**
	 * LoopStatement:
	 *   loop <StatementSection>
	 *   loop while <Expression> <StatementSection>
	 *   loop <StatementSection> while <Expression>
	 *   loop over <Identifier> as <Identifier>[, <Identifier>] <StatementSection>
	 */
	private fun parseLoopStatement(): LoopStatement {
		val start = consume(WordAtom.LOOP).start
		var generator = when(currentWord?.type) {
			WordAtom.WHILE -> parseWhileGenerator()
			WordAtom.OVER -> parseOverGenerator()
			else -> null
		}
		val body = parseStatementSection()
		if(currentWord?.type == WordAtom.WHILE)
			generator = parseWhileGenerator(true)
		return LoopStatement(start, generator, body)
	}

	/**
	 * WhileGenerator:
	 *   while <Expression>
	 */
	private fun parseWhileGenerator(isPostCondition: Boolean = false): WhileGenerator {
		val start = consume(WordAtom.WHILE).start
		val condition = parseExpression()
		return WhileGenerator(start, condition, isPostCondition)
	}

	/**
	 * OverGenerator:
	 *   over <Index> as <Identifier>[, <Identifier>]
	 */
	private fun parseOverGenerator(): OverGenerator {
		val start = consume(WordAtom.OVER).start
		val collection = expressionParser.parseIndex()
		consume(WordAtom.AS)
		var keyDeclaration: Identifier? = null
		if(nextWord?.type == WordAtom.COMMA) {
			keyDeclaration = parseIdentifier()
			consume(WordAtom.COMMA)
		}
		val valueDeclaration = parseIdentifier()
		return OverGenerator(start, collection, keyDeclaration, valueDeclaration)
	}

	/**
	 * StatementSection:
	 *   <StatementBlock>[<HandleBlock>]...[<AlwaysBlock>]
	 */
	private fun parseStatementSection(): StatementSection {
		val mainBlock = parseStatementBlock()
		val handleBlocks = LinkedList<HandleBlock>()
		while(currentWord?.type == WordAtom.HANDLE)
			handleBlocks.add(parseHandleBlock())
		val alwaysBlock = if(currentWord?.type == WordAtom.ALWAYS) parseAlwaysBlock() else null
		return StatementSection(mainBlock, handleBlocks, alwaysBlock)
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
	 * HandleBlock:
	 *   handle <Type> [<Identifier>] <StatementBlock>
	 */
	private fun parseHandleBlock(): HandleBlock {
		val start = consume(WordAtom.HANDLE).start
		val type = parseRequiredType()
		val identifier = if(currentWord?.type == WordAtom.IDENTIFIER)
			parseIdentifier()
		else
			null
		val block = parseStatementBlock()
		return HandleBlock(start, type, identifier, block)
	}

	/**
	 * AlwaysBlock:
	 *   always <StatementBlock>
	 */
	private fun parseAlwaysBlock(): AlwaysBlock {
		val start = consume(WordAtom.ALWAYS).start
		val block = parseStatementBlock()
		return AlwaysBlock(start, block)
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
		parentTypes.add(parseRequiredType())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			parentTypes.add(parseRequiredType())
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
	 *   <ModifierList> <DeinitializerDefinition>
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
		if(currentWord?.type == WordAtom.DEINIT)
			return parseDeinitializerDefinition(modifierList)
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
		types.add(typeParser.parseOptionallyTypedIdentifier())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			types.add(typeParser.parseOptionallyTypedIdentifier())
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
	 *   init<ParameterList> [<StatementSection>]
	 */
	private fun parseInitializerDefinition(modifierList: ModifierList?): Element {
		var start = consume(WordAtom.INIT).start
		if(modifierList != null)
			start = modifierList.start
		val parameterList = parseParameterList(false)
		val body = if(currentWord?.type == WordAtom.BRACES_OPEN) parseStatementSection() else null
		return InitializerDefinition(start, modifierList, parameterList, body)
	}

	/**
	 * DeinitializerDeclaration:
	 *   deinit [<StatementSection>]
	 */
	private fun parseDeinitializerDefinition(modifierList: ModifierList?): Element {
		val keyword = consume(WordAtom.DEINIT)
		val body = if(currentWord?.type == WordAtom.BRACES_OPEN) parseStatementSection() else null
		val start = modifierList?.start ?: keyword.start
		val end = body?.end ?: keyword.end
		return DeinitializerDefinition(start, end, modifierList, body)
	}

	/**
	 * FunctionDeclaration:
	 *   <function-declaration> <Identifier><ParameterList>[: <Type>] [<StatementSection>]
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
			returnType = parseRequiredType()
		}
		val body = if(currentWord?.type == WordAtom.BRACES_OPEN)
			parseStatementSection()
		else
			null
		return FunctionDefinition(start, modifierList, identifier, parameterList, body, returnType)
	}

	/**
	 * OperatorDeclaration:
	 *   operator <Operator>[<ParameterList>][: <Type>] [<StatementSection>]
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
			returnType = parseRequiredType()
		}
		val body = if(currentWord?.type == WordAtom.BRACES_OPEN)
			parseStatementSection()
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
				parameters.add(typeParser.parseTypedIdentifier())
			while(currentWord?.type == WordAtom.COMMA) {
				consume(WordAtom.COMMA)
				parameters.add(typeParser.parseTypedIdentifier())
			}
			val end = consume(WordAtom.BRACKETS_CLOSE).end
			return IndexOperator(start, end, parameters)
		}
		return Operator(consume(WordType.OPERATOR))
	}

	/**
	 * ParameterList[areTypesRequired]:
	 *   ([<Parameter[areTypesRequired]>[, <Parameter[areTypesRequired]>]])
	 */
	private fun parseParameterList(areTypesRequired: Boolean = true): ParameterList {
		val start = consume(WordAtom.PARENTHESES_OPEN).start
		val parameters = LinkedList<Parameter>()
		if(currentWord?.type != WordAtom.PARENTHESES_CLOSE)
			parameters.add(parseParameter(areTypesRequired))
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			parameters.add(parseParameter(areTypesRequired))
		}
		val end = consume(WordAtom.PARENTHESES_CLOSE).end
		return ParameterList(start, end, parameters)
	}

	/**
	 * Parameter[isTypeRequired]:
	 *   <ModifierList> <TypedIdentifier>
	 * Parameter:
	 *   <ModifierList> <OptionallyTypedIdentifier>
	 */
	private fun parseParameter(isTypeRequired: Boolean = true): Parameter {
		val modifierList = parseModifierList(WordType.PARAMETER_MODIFIER)
		val identifier = if(isTypeRequired)
			typeParser.parseTypedIdentifier()
		else
			typeParser.parseOptionallyTypedIdentifier()
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
			return parseDeclarationAssignment()
		val identifier = typeParser.parseTypedIdentifier()
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
	 *   <Identifier> = [<Identifier> = ]...<Expression>
	 */
	private fun parseDeclarationAssignment(): Element {
		val targets = LinkedList<Element>()
		var lastExpression: Element = parseIdentifier()
		do {
			consume(WordAtom.ASSIGNMENT)
			targets.add(lastExpression)
			lastExpression = parseExpression()
		} while(lastExpression is Identifier && currentWord?.type == WordAtom.ASSIGNMENT)
		return Assignment(targets, lastExpression)
	}
}