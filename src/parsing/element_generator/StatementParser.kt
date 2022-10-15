package parsing.element_generator

import errors.user.UnexpectedWordError
import parsing.syntax_tree.operations.*
import parsing.syntax_tree.access.IndexAccess
import parsing.syntax_tree.access.MemberAccess
import parsing.syntax_tree.control_flow.*
import parsing.syntax_tree.definitions.*
import parsing.syntax_tree.definitions.sections.*
import parsing.syntax_tree.general.*
import parsing.syntax_tree.literals.*
import parsing.tokenizer.*
import source_structure.Position
import parsing.syntax_tree.general.StatementBlock
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
				|| expression is IndexAccess
	}

	private fun parseExpression(): ValueElement {
		return expressionParser.parseExpression()
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
	 *   <YieldStatement>
	 *   <RaiseStatement>
	 *   <LoopStatement>
	 *   <BreakStatement>
	 *   <NextStatement>
	 *   <ModifiedDefinition>
	 *   <VariableDeclaration>
	 *   <TypeDefinition>
	 *   <TypeAlias>
	 *   <GeneratorDefinition>
	 *   <Expression>
	 *   <Expression><unary-modification>
	 *   <Expression> = <Expression>[ = <Expression>]...
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
		if(currentWord?.type == WordAtom.YIELD)
			return parseYieldStatement()
		if(currentWord?.type == WordAtom.RAISE)
			return parseRaiseStatement()
		if(currentWord?.type == WordAtom.LOOP)
			return parseLoopStatement()
		if(currentWord?.type == WordAtom.BREAK)
			return parseBreakStatement()
		if(currentWord?.type == WordAtom.NEXT)
			return parseNextStatement()
		if(currentWord?.type == WordAtom.ALIAS)
			return parseTypeAlias()
		if(WordType.MODIFIER.includes(currentWord?.type))
			return parseModifierSection()
		if(WordType.VARIABLE_DECLARATION.includes(currentWord?.type))
			return parseVariableSection()
		if(WordType.TYPE_TYPE.includes(currentWord?.type))
			return parseTypeDefinition()
		if(currentWord?.type == WordAtom.GENERATE)
			return parseGeneratorDefinition()

		val statement = expressionParser.parseExpression()
		if(isExpressionAssignable(statement)) {
			if(currentWord?.type == WordAtom.ASSIGNMENT) {
				val targets = LinkedList<ValueElement>()
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
		val subject = parseExpression()
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
			consumeLineBreaks()
		}
		consumeLineBreaks()
		val end = consume(WordAtom.BRACES_CLOSE).end
		return SwitchStatement(subject, cases, elseResult, start, end)
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
	 *   return <YieldStatement>
	 */
	private fun parseReturnStatement(): Element {
		val word = consume(WordAtom.RETURN)
		var value: ValueElement? = null
		if(currentWord?.type == WordAtom.YIELD)
			value = parseYieldStatement()
		else if(currentWord?.type != WordAtom.LINE_BREAK)
			value = parseExpression()
		return ReturnStatement(word.start, value, value?.end ?: word.end)
	}

	/**
	 * YieldStatement:
	 *   yield <Expression>[, <Expression>]
	 */
	private fun parseYieldStatement(): YieldStatement {
		val start = consume(WordAtom.YIELD).start
		var key: Element? = null
		var value = parseExpression()
		if(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			key = value
			value = parseExpression()
		}
		return YieldStatement(start, key, value)
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
	 *   over <Try> as <Identifier>[, <Identifier>]
	 */
	private fun parseOverGenerator(): OverGenerator {
		val start = consume(WordAtom.OVER).start
		val collection = expressionParser.parseTry()
		var keyDeclaration: Identifier? = null
		var valueDeclaration: Identifier? = null
		if(currentWord?.type == WordAtom.AS) {
			consume(WordAtom.AS)
			if(nextWord?.type == WordAtom.COMMA) {
				keyDeclaration = parseIdentifier()
				consume(WordAtom.COMMA)
			}
			valueDeclaration = parseIdentifier()
		}
		return OverGenerator(start, collection, keyDeclaration, valueDeclaration)
	}

	/**
	 * StatementSection:
	 *   <StatementBlock>[<HandleBlock>]...[<AlwaysBlock>]
	 *   <Statement>
	 */
	fun parseStatementSection(): StatementSection {
		if(currentWord?.type != WordAtom.BRACES_OPEN) {
			consumeLineBreaks()
			val statement = parseStatement()
			return StatementSection(StatementBlock(statement))
		}
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
	private fun parseStatementBlock(previousStart: Position? = null): StatementBlock {
		var start = consume(WordAtom.BRACES_OPEN).start
		if(previousStart != null)
			start = previousStart
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
		val type = typeParser.parseType(false)
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
	private fun parseAlwaysBlock(): StatementBlock {
		val start = consume(WordAtom.ALWAYS).start
		return parseStatementBlock(start)
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
	 * TypeDefinition:
	 *   <type-type> <Identifier>[: <Type>] <TypeBody>
	 */
	private fun parseTypeDefinition(): TypeDefinition {
		val type = consume(WordType.TYPE_TYPE)
		val identifier = parseIdentifier()
		var superType: TypeElement? = null
		if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			superType = typeParser.parseType(false)
		}
		val body = parseTypeBody()
		return TypeDefinition(type, identifier, superType, body)
	}

	/**
	 * ModifierList:
	 *   [<Modifier>[ <Modifier>]...]
	 */
	private fun parseModifierList(): ModifierList? {
		if(!WordType.MODIFIER.includes(currentWord?.type))
			return null
		val modifiers = LinkedList<Modifier>()
		do
			modifiers.add(parseModifier())
		while(WordType.MODIFIER.includes(currentWord?.type))
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
	 * TypeAlias:
	 *   alias <Identifier> = <Type>
	 */
	private fun parseTypeAlias(modifierList: ModifierList? = null): TypeAlias {
		var start = consume(WordAtom.ALIAS).start
		if(modifierList != null)
			start = modifierList.start
		val identifier = parseIdentifier()
		consume(WordAtom.ASSIGNMENT)
		val type = typeParser.parseUnionType()
		return TypeAlias(start, modifierList, identifier, type)
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
	 *   <InstanceList>
	 *   <DeclarationSection>
	 */
	private fun parseMemberDeclaration(): Element {
		if(currentWord?.type == WordAtom.CONTAINING)
			return parseGenericsDeclaration()
		if(currentWord?.type == WordAtom.INSTANCES)
			return parseInstanceList()
		return parseDeclarationSection()
	}

	/**
	 * GenericsDeclaration:
	 *   containing <GenericProperty>[,<GenericProperty>]...
	 */
	private fun parseGenericsDeclaration(): GenericsDeclaration {
		val start = consume(WordAtom.CONTAINING).start
		val types = LinkedList<GenericsListElement>()
		types.add(parseGenericsListElement())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			types.add(parseGenericsListElement())
		}
		return GenericsDeclaration(start, types)
	}

	/**
	 * GenericsList:
	 *   [<<GenericsListElement>[, <GenericsListElement>]...>]
	 */
	private fun parseGenericsList(): GenericsList? {
		if(WordType.GENERICS_START.includes(currentWord?.type)) {
			val elements = LinkedList<GenericsListElement>()
			val start = consume(WordType.GENERICS_START).start
			elements.add(parseGenericsListElement())
			while(currentWord?.type == WordAtom.COMMA) {
				consume(WordAtom.COMMA)
				elements.add(parseGenericsListElement())
			}
			val end = consume(WordType.GENERICS_END).end
			return GenericsList(start, elements, end)
		}
		return null
	}

	/**
	 * GenericsListElement:
	 *   <Identifier>: <Type>
	 */
	private fun parseGenericsListElement(): GenericsListElement {
		val identifier = parseIdentifier()
		val type = if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			typeParser.parseType()
		} else null
		return GenericsListElement(identifier, type)
	}

	/**
	 * InstanceList:
	 *   instances <Instance>[,<Instance>]...
	 */
	private fun parseInstanceList(): InstanceList {
		val start = consume(WordAtom.INSTANCES).start
		val instances = LinkedList<Instance>()
		instances.add(parseInstance())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			instances.add(parseInstance())
		}
		return InstanceList(start, instances)
	}

	/**
	 * Instance:
	 *   <Identifier>[([<Expression>[, <Expression>]...])]
	 */
	private fun parseInstance(): Instance {
		val identifier = parseIdentifier()
		val parameters = LinkedList<ValueElement>()
		var end = identifier.end
		if(currentWord?.type == WordAtom.PARENTHESES_OPEN) {
			consume(WordAtom.PARENTHESES_OPEN)
			if(currentWord?.type != WordAtom.PARENTHESES_CLOSE) {
				parameters.add(parseExpression())
				while(currentWord?.type == WordAtom.COMMA) {
					consume(WordAtom.COMMA)
					parameters.add(parseExpression())
				}
			}
			end = consume(WordAtom.PARENTHESES_CLOSE).end
		}
		return Instance(identifier, parameters, end)
	}

	/**
	 * InitializerDeclaration:
	 *   init[<ParameterList>] [<StatementSection>]
	 */
	private fun parseInitializerDefinition(): Element {
		val declarationWord = consume(WordAtom.INIT)
		val parameterList = if(currentWord?.type == WordAtom.PARENTHESES_OPEN) parseParameterList() else null
		val body = if(currentWord?.type == WordAtom.BRACES_OPEN) parseStatementSection() else null
		val end = body?.end ?: parameterList?.end ?: declarationWord.end
		return InitializerDefinition(declarationWord.start, parameterList, body, end)
	}

	/**
	 * DeinitializerDeclaration:
	 *   deinit [<StatementSection>]
	 */
	private fun parseDeinitializerDefinition(): Element {
		val keyword = consume(WordAtom.DEINIT)
		val body = if(currentWord?.type == WordAtom.BRACES_OPEN) parseStatementSection() else null
		val end = body?.end ?: keyword.end
		return DeinitializerDefinition(keyword.start, end, body)
	}

	/**
	 * FunctionDeclaration:
	 *   <Identifier><TypeList><ParameterList>[: <Type>] [<StatementSection>]
	 */
	private fun parseFunctionDefinition(): FunctionDefinition {
		val identifier = parseIdentifier()
		val genericsList = parseGenericsList()
		val parameterList = parseParameterList()
		val returnType = if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			typeParser.parseType()
		} else null
		val body = if(currentWord?.type == WordAtom.BRACES_OPEN)
			parseStatementSection()
		else null
		return FunctionDefinition(identifier, genericsList, parameterList, body, returnType)
	}

	/**
	 * OperatorDefinition:
	 *   operator <Operator>[<ParameterList>][: <Type>] [<StatementSection>]
	 */
	private fun parseOperatorDefinition(): OperatorDefinition {
		val operator = parseOperator()
		val parameterList = if(currentWord?.type == WordAtom.PARENTHESES_OPEN)
			parseParameterList()
		else null
		val returnType = if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			typeParser.parseType()
		} else null
		val body = if(currentWord?.type == WordAtom.BRACES_OPEN)
			parseStatementSection()
		else null
		return OperatorDefinition(operator, parameterList, body, returnType)
	}

	/**
	 * Operator:
	 *   <operator>
	 *   [<TypedIdentifier[, <TypedIdentifier>]>]
	 */
	private fun parseOperator(): Operator {
		if(currentWord?.type == WordAtom.BRACKETS_OPEN) {
			val start = consume(WordAtom.BRACKETS_OPEN).start
			val parameters = LinkedList<Parameter>()
			if(currentWord?.type != WordAtom.BRACKETS_CLOSE)
				parameters.add(parseParameter())
			while(currentWord?.type == WordAtom.COMMA) {
				consume(WordAtom.COMMA)
				parameters.add(parseParameter())
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
	 * Parameter:
	 *   <ModifierList> <Identifier>[: <Type>]
	 */
	fun parseParameter(): Parameter {
		val modifierList = parseModifierList()
		val identifier = parseIdentifier()
		val type = if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			typeParser.parseType()
		} else null
		return Parameter(modifierList, identifier, type)
	}

	/**
	 * DeclarationSection:
	 *   <VariableSection>
	 *   <InitializerDefinition>
	 *   <DeinitializerDefinition>
	 *   <FunctionSection>
	 *   <OperatorSection>
	 *   <TypeDefinition>
	 *   <TypeAlias>
	 *   <ModifierSection>
	 */
	private fun parseDeclarationSection(): Element {
		if(WordType.VARIABLE_DECLARATION.includes(currentWord?.type))
			return parseVariableSection()
		if(currentWord?.type == WordAtom.INIT)
			return parseInitializerDefinition()
		if(currentWord?.type == WordAtom.DEINIT)
			return parseDeinitializerDefinition()
		if(WordType.FUNCTION_DECLARATION.includes(currentWord?.type))
			return parseFunctionSection()
		if(currentWord?.type == WordAtom.OPERATOR)
			return parseOperatorSection()
		if(WordType.TYPE_TYPE.includes(currentWord?.type))
			return parseTypeDefinition()
		if(currentWord?.type == WordAtom.ALIAS)
			return parseTypeAlias()
		if(WordType.MODIFIER.includes(currentWord?.type))
			return parseModifierSection()
		val expectation = "declaration"
		throw UnexpectedWordError(elementGenerator.getCurrentWord(expectation), expectation)
	}

	/**
	 * ModifierSection:
	 *   <ModifierList> <DeclarationSection>
	 *   <ModifierList> {
	 *   	<DeclarationSection>...
	 *   }
	 */
	private fun parseModifierSection(): ModifierSection {
		val modifierList = parseModifierList()
				?: throw UnexpectedWordError(getCurrentWord(WordType.MODIFIER), WordType.MODIFIER)
		val sections = LinkedList<Element>()
		val end = if(currentWord?.type == WordAtom.BRACES_OPEN) {
			consume(WordAtom.BRACES_OPEN)
			while(currentWord?.type != WordAtom.BRACES_CLOSE) {
				consumeLineBreaks()
				if(currentWord?.type == WordAtom.BRACES_CLOSE)
					break
				sections.add(parseDeclarationSection())
			}
			consume(WordAtom.BRACES_CLOSE).end
		} else {
			sections.add(parseDeclarationSection())
			sections.last().end
		}
		return ModifierSection(modifierList, sections, end)
	}

	/**
	 * VariableSection:
	 *   <variable-declaration> <VariableSectionPart>
	 *   <variable-declaration>[: <Type>] [= <Expression>] {
	 *   	<VariableSectionPart>...
	 *   }
	 */
	private fun parseVariableSection(): VariableSection {
		val declarationType = consume(WordType.VARIABLE_DECLARATION)
		val type = if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			typeParser.parseType()
		} else null
		val value = if(currentWord?.type == WordAtom.ASSIGNMENT) {
			consume(WordAtom.ASSIGNMENT)
			parseExpression()
		} else null
		val variables = LinkedList<VariableSectionElement>()
		val end = if(currentWord?.type == WordAtom.BRACES_OPEN) {
			consume(WordAtom.BRACES_OPEN)
			while(currentWord?.type != WordAtom.BRACES_CLOSE) {
				consumeLineBreaks()
				if(currentWord?.type == WordAtom.BRACES_CLOSE)
					break
				variables.add(parseVariableSectionPart())
			}
			consume(WordAtom.BRACES_CLOSE).end
		} else {
			variables.add(parseVariableSectionPart())
			variables.last().end
		}
		return VariableSection(declarationType, type, value, variables, end)
	}

	/**
	 * VariableSectionPart:
	 *   <Identifier>[: <Type>] [get <Expression>] [set <Expression>]
	 *   <Identifier>[: <Type>] [= <Expression>]
	 */
	private fun parseVariableSectionPart(): VariableSectionElement {
		val identifier = parseIdentifier()
		val type = if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			typeParser.parseType()
		} else null
		consumeLineBreaks()
		val getExpression = if(currentWord?.type == WordAtom.GETS) {
			consume(WordAtom.GETS)
			parseExpression()
		} else null
		consumeLineBreaks()
		val setExpression = if(currentWord?.type == WordAtom.SETS) {
			consume(WordAtom.SETS)
			parseExpression()
		} else null
		if(getExpression != null || setExpression != null)
			return ComputedProperty(identifier, type, getExpression, setExpression)
		var value: ValueElement? = null
		if(currentWord?.type == WordAtom.ASSIGNMENT) {
			consume(WordAtom.ASSIGNMENT)
			value = parseExpression()
		}
		return VariableDeclaration(identifier, type, value)
	}

	/**
	 * FunctionSection:
	 *   <function-declaration> <FunctionDefinition>
	 *   <function-declaration> {
	 *   	<FunctionDefinition>...
	 *   }
	 */
	private fun parseFunctionSection(): FunctionSection {
		val type = consume(WordType.FUNCTION_DECLARATION)
		val functions = LinkedList<FunctionDefinition>()
		val end = if(currentWord?.type == WordAtom.BRACES_OPEN) {
			consume(WordAtom.BRACES_OPEN)
			while(currentWord?.type != WordAtom.BRACES_CLOSE) {
				consumeLineBreaks()
				if(currentWord?.type == WordAtom.BRACES_CLOSE)
					break
				functions.add(parseFunctionDefinition())
			}
			consume(WordAtom.BRACES_CLOSE).end
		} else {
			functions.add(parseFunctionDefinition())
			functions.last().end
		}
		return FunctionSection(type, functions, end)
	}

	/**
	 * OperatorSection:
	 *   operator <OperatorDefinition>
	 *   operator {
	 *   	<OperatorDefinition>...
	 *   }
	 */
	private fun parseOperatorSection(): OperatorSection {
		val type = consume(WordAtom.OPERATOR)
		val operators = LinkedList<OperatorDefinition>()
		val end = if(currentWord?.type == WordAtom.BRACES_OPEN) {
			consume(WordAtom.BRACES_OPEN)
			while(currentWord?.type != WordAtom.BRACES_CLOSE) {
				consumeLineBreaks()
				if(currentWord?.type == WordAtom.BRACES_CLOSE)
					break
				operators.add(parseOperatorDefinition())
			}
			consume(WordAtom.BRACES_CLOSE).end
		} else {
			operators.add(parseOperatorDefinition())
			operators.last().end
		}
		return OperatorSection(type, operators, end)
	}

	/**
	 * GeneratorDefinition:
	 *   generate <Identifier><ParameterList>: <Type>[, <Type>] <StatementSection>
	 */
	private fun parseGeneratorDefinition(): Element {
		val start = consume(WordAtom.GENERATE).start
		val identifier = parseIdentifier()
		val parameterList = parseParameterList()
		consume(WordAtom.COLON)
		var keyReturnType: TypeElement? = null
		var valueReturnType = typeParser.parseType()
		if(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			keyReturnType = valueReturnType
			valueReturnType = typeParser.parseType()
		}
		val body = parseStatementSection()
		return GeneratorDefinition(start, identifier, parameterList, keyReturnType, valueReturnType, body)
	}
}
