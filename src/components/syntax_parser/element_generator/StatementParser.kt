package components.syntax_parser.element_generator

import components.syntax_parser.syntax_tree.access.IndexAccess
import components.syntax_parser.syntax_tree.access.MemberAccess
import components.syntax_parser.syntax_tree.control_flow.*
import components.syntax_parser.syntax_tree.definitions.*
import components.syntax_parser.syntax_tree.definitions.sections.*
import components.syntax_parser.syntax_tree.general.*
import components.syntax_parser.syntax_tree.literals.Identifier
import components.syntax_parser.syntax_tree.operations.Assignment
import components.syntax_parser.syntax_tree.operations.BinaryModification
import components.syntax_parser.syntax_tree.operations.UnaryModification
import components.tokenizer.Word
import components.tokenizer.WordAtom
import components.tokenizer.WordDescriptor
import components.tokenizer.WordType
import errors.user.UnexpectedEndOfFileError
import errors.user.UnexpectedWordError
import errors.user.UserError
import logger.issues.parsing.InvalidSyntax
import logger.issues.parsing.UnexpectedEndOfFile
import logger.issues.parsing.UnexpectedWord
import source_structure.Position
import java.util.*

class StatementParser(private val syntaxTreeGenerator: SyntaxTreeGenerator): Generator() {
	override val currentWord: Word?
		get() = syntaxTreeGenerator.currentWord
	override val nextWord: Word?
		get() = syntaxTreeGenerator.nextWord
	override var parseForeignLanguageLiteralNext: Boolean
		get() = syntaxTreeGenerator.parseForeignLanguageLiteralNext
		set(value) {
			syntaxTreeGenerator.parseForeignLanguageLiteralNext = value
		}

	private val expressionParser
		get() = syntaxTreeGenerator.expressionParser
	private val typeParser
		get() = syntaxTreeGenerator.typeParser
	private val literalParser
		get() = syntaxTreeGenerator.literalParser

	override fun getCurrentPosition(): Position = syntaxTreeGenerator.getCurrentPosition()

	override fun consume(type: WordDescriptor): Word {
		return syntaxTreeGenerator.consume(type)
	}

	private fun handleUserError(error: UserError) {
		syntaxTreeGenerator.addIssue(when(error) {
			is UnexpectedEndOfFileError -> UnexpectedEndOfFile(error.message, error.section)
			is UnexpectedWordError -> UnexpectedWord(error.message, error.section)
			else -> InvalidSyntax(error.message)
		})
		currentWord?.let { invalidWord ->
			syntaxTreeGenerator.skipLine(invalidWord)
		}
	}

	private fun isExpressionAssignable(expression: SyntaxTreeNode): Boolean {
		return expression is Identifier || expression is MemberAccess || expression is IndexAccess
	}

	private fun parseExpression(): ValueSyntaxTreeNode {
		return expressionParser.parseExpression()
	}

	private fun parseIdentifier(): Identifier {
		return literalParser.parseIdentifier()
	}

	fun parseStatements(endWord: WordAtom? = null): List<SyntaxTreeNode> {
		val statements = LinkedList<SyntaxTreeNode>()
		while(currentWord?.type != endWord && currentWord?.type != null) {
			consumeLineBreaks()
			if(currentWord?.type == endWord || currentWord?.type == null)
				break
			try {
				statements.add(parseStatement())
			} catch(error: UserError) {
				handleUserError(error)
			}
		}
		return statements
	}

	/**
	 * Statement:
	 *   <StatementSection>
	 *   <FileReference>
	 *   <IfExpression>
	 *   <SwitchExpression>
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
	private fun parseStatement(): SyntaxTreeNode {
		if(currentWord?.type == WordAtom.OPENING_BRACE)
			return parseStatementSection()
		if(currentWord?.type == WordAtom.REFERENCING)
			return parseFileReference()
		if(currentWord?.type == WordAtom.IF)
			return expressionParser.parseIfExpression(false)
		if(currentWord?.type == WordAtom.SWITCH)
			return expressionParser.parseSwitchExpression(false)
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
		if(currentWord?.type == WordAtom.TYPE_ALIAS)
			return parseTypeAlias()
		if(WordType.MODIFIER.includes(currentWord?.type))
			return parseModifierSection(DeclarationContext.STATEMENT)
		if(WordType.VARIABLE_DECLARATION.includes(currentWord?.type))
			return parseVariableSection(DeclarationContext.STATEMENT)
		if(WordType.TYPE_TYPE.includes(nextWord?.type))
			return parseTypeDefinition()
		if(currentWord?.type == WordAtom.GENERATOR)
			return parseGeneratorDefinition()

		val expression = parseExpression()
		//TODO also allow operators on self references
		if(isExpressionAssignable(expression)) {
			if(currentWord?.type == WordAtom.ASSIGNMENT) {
				val targets = LinkedList<ValueSyntaxTreeNode>()
				var lastExpression = expression
				do {
					consume(WordAtom.ASSIGNMENT)
					targets.add(lastExpression)
					lastExpression = parseExpression()
				} while(isExpressionAssignable(lastExpression) && currentWord?.type == WordAtom.ASSIGNMENT)
				return Assignment(targets, lastExpression)
			}
		}
		if(WordType.BINARY_MODIFICATION.includes(currentWord?.type)) {
			val operator = expressionParser.parseOperator(WordType.BINARY_MODIFICATION)
			val value = parseExpression()
			return BinaryModification(expression, value, operator)
		}
		if(WordType.UNARY_MODIFICATION.includes(currentWord?.type)) {
			val operator = expressionParser.parseOperator(WordType.UNARY_MODIFICATION)
			return UnaryModification(expression, operator)
		}
		return expression
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
		if(currentWord?.type != WordAtom.OPENING_BRACE)
			return null
		val start = consume(WordAtom.OPENING_BRACE).start
		val referenceAliases = LinkedList<ReferenceAlias>()
		while(currentWord?.type == WordAtom.LINE_BREAK) {
			while(currentWord?.type == WordAtom.LINE_BREAK)
				consume(WordAtom.LINE_BREAK)
			if(currentWord?.type == WordAtom.IDENTIFIER)
				referenceAliases.add(parseAlias())
		}
		val end = consume(WordAtom.CLOSING_BRACE).end
		return AliasBlock(start, end, referenceAliases)
	}

	/**
	 * Alias:
	 *   <Identifier> as <Identifier>
	 */
	private fun parseAlias(): ReferenceAlias {
		val originalName = parseIdentifier()
		consume(WordAtom.AS)
		val localName = parseIdentifier()
		return ReferenceAlias(originalName, localName)
	}

	/**
	 * ReturnStatement:
	 *   return
	 *   return <Expression>
	 *   return <YieldStatement>
	 */
	private fun parseReturnStatement(): SyntaxTreeNode {
		val word = consume(WordAtom.RETURN)
		var value: ValueSyntaxTreeNode? = null
		if(currentWord != null) {
			if(currentWord?.type == WordAtom.YIELD)
				value = parseYieldStatement()
			else if(currentWord?.type != WordAtom.LINE_BREAK)
				value = parseExpression()
		}
		return ReturnStatement(word.start, value, value?.end ?: word.end)
	}

	/**
	 * YieldStatement:
	 *   yield <Expression>[, <Expression>]
	 */
	private fun parseYieldStatement(): YieldStatement {
		val start = consume(WordAtom.YIELD).start
		var key: ValueSyntaxTreeNode? = null
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
	 *   loop <WhileGenerator> <StatementSection>
	 *   loop <StatementSection> <WhileGenerator>
	 *   loop <OverGenerator> <StatementSection>
	 */
	private fun parseLoopStatement(): LoopStatement {
		val start = consume(WordAtom.LOOP).start
		var generator = when(currentWord?.type) {
			WordAtom.WHILE, WordAtom.UNTIL -> parseWhileGenerator()
			WordAtom.OVER -> parseOverGenerator()
			else -> null
		}
		val body = parseStatementSection()
		if(WordType.CONDITIONAL_GENERATOR.includes(currentWord?.type))
			generator = parseWhileGenerator(true)
		return LoopStatement(start, generator, body)
	}

	/**
	 * WhileGenerator:
	 *   while <Expression>
	 *   until <Expression>
	 */
	private fun parseWhileGenerator(isPostCondition: Boolean = false): WhileGenerator {
		val conditionalGeneratorWord = consume(WordType.CONDITIONAL_GENERATOR)
		val condition = parseExpression()
		return WhileGenerator(conditionalGeneratorWord, condition, isPostCondition)
	}

	/**
	 * OverGenerator:
	 *   over <Try>[ using <Identifier>][ as <Identifier>[, <Identifier>]...]
	 */
	private fun parseOverGenerator(): OverGenerator {
		val start = consume(WordAtom.OVER).start
		val collection = expressionParser.parseTry()
		var iteratorVariable: Identifier? = null
		if(currentWord?.type == WordAtom.USING) {
			consume(WordAtom.USING)
			iteratorVariable = parseIdentifier()
		}
		val variableDeclarations = LinkedList<Identifier>()
		if(currentWord?.type == WordAtom.AS) {
			consume(WordAtom.AS)
			while(true) {
				variableDeclarations.add(parseIdentifier())
				if(currentWord?.type == WordAtom.COMMA) {
					consume(WordAtom.COMMA)
					continue
				}
				break
			}
		}
		return OverGenerator(start, collection, iteratorVariable, variableDeclarations)
	}

	/**
	 * StatementSection:
	 *   <StatementBlock>[<HandleBlock>]...[<AlwaysBlock>]
	 *   <Statement>
	 */
	fun parseStatementSection(): StatementSection {
		if(currentWord?.type != WordAtom.OPENING_BRACE) {
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
	 *   { [<Statement>]... }
	 */
	private fun parseStatementBlock(previousStart: Position? = null): StatementBlock {
		var start = consume(WordAtom.OPENING_BRACE).start
		if(previousStart != null)
			start = previousStart
		val statements = parseStatements(WordAtom.CLOSING_BRACE)
		val end = consume(WordAtom.CLOSING_BRACE).end
		return StatementBlock(start, end, statements)
	}

	/**
	 * HandleBlock:
	 *   handle [<Identifier>:] <Type> <StatementBlock>
	 */
	private fun parseHandleBlock(): HandleBlock {
		val start = consume(WordAtom.HANDLE).start
		var identifier: Identifier? = null
		if(nextWord?.type == WordAtom.COLON) {
			identifier = parseIdentifier()
			consume(WordAtom.COLON)
		}
		val type = typeParser.parseType(false)
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
	 *   <Identifier> <type-type>[ in <ObjectType>][: <Type>] <TypeBody>
	 */
	private fun parseTypeDefinition(): TypeDefinition {
		val identifier = parseIdentifier()
		val type = consume(WordType.TYPE_TYPE)
		val explicitParentType = if(currentWord?.type == WordAtom.IN) {
			consume(WordAtom.IN)
			typeParser.parseObjectType()
		} else null
		val superType = if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			typeParser.parseType(false)
		} else null
		val body = parseTypeBody()
		return TypeDefinition(identifier, type, explicitParentType, superType, body)
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
	 *   alias <Identifier> = <Type>[ { [<InstanceList>]... }]
	 */
	private fun parseTypeAlias(modifierList: ModifierList? = null): TypeAlias {
		var start = consume(WordAtom.TYPE_ALIAS).start
		if(modifierList != null)
			start = modifierList.start
		val identifier = parseIdentifier()
		consume(WordAtom.ASSIGNMENT)
		val type = typeParser.parseUnionType()
		val instanceLists = LinkedList<InstanceList>()
		val end = if(currentWord?.type == WordAtom.OPENING_BRACE) {
			consume(WordAtom.OPENING_BRACE)
			while(currentWord?.type != WordAtom.CLOSING_BRACE && currentWord?.type != null) {
				consumeLineBreaks()
				if(currentWord?.type == WordAtom.CLOSING_BRACE || currentWord?.type == null)
					break
				try {
					instanceLists.add(parseInstanceList())
				} catch(error: UserError) {
					handleUserError(error)
				}
			}
			consume(WordAtom.CLOSING_BRACE).end
		} else {
			type.end
		}
		return TypeAlias(modifierList, identifier, type, instanceLists, start, end)
	}

	/**
	 * TypeBody:
	 *   [{ [<MemberDeclaration>]... }]
	 */
	private fun parseTypeBody(): TypeBody? {
		if(currentWord?.type != WordAtom.OPENING_BRACE)
			return null
		val start = consume(WordAtom.OPENING_BRACE).start
		val members = LinkedList<SyntaxTreeNode>()
		while(currentWord?.type != WordAtom.CLOSING_BRACE && currentWord?.type != null) {
			consumeLineBreaks()
			if(currentWord?.type == WordAtom.CLOSING_BRACE || currentWord?.type == null)
				break
			try {
				members.add(parseMemberDeclaration())
			} catch(error: UserError) {
				handleUserError(error)
			}
		}
		val end = consume(WordAtom.CLOSING_BRACE).end
		return TypeBody(start, end, members)
	}

	/**
	 * MemberDeclaration:
	 *   <GenericsDeclaration>
	 *   <DeclarationSection>
	 */
	private fun parseMemberDeclaration(): SyntaxTreeNode {
		if(currentWord?.type == WordAtom.CONTAINING)
			return parseGenericsDeclaration()
		return parseDeclarationSection(DeclarationContext.TYPE_DEFINITION)
	}

	/**
	 * GenericsDeclaration:
	 *   containing <Parameter>[,<Parameter>]...
	 */
	private fun parseGenericsDeclaration(): GenericsDeclaration {
		val start = consume(WordAtom.CONTAINING).start
		val types = LinkedList<Parameter>()
		types.add(parseParameter())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			types.add(parseParameter())
		}
		return GenericsDeclaration(start, types)
	}

	/**
	 * InstanceList:
	 *   instances <Instance>[,<Instance>]...
	 */
	private fun parseInstanceList(): InstanceList {
		val start = consume(WordAtom.INSTANCES).start
		val instances = LinkedList<Instance>()
		consumeLineBreaks()
		instances.add(parseInstance())
		while(currentWord?.type == WordAtom.COMMA) {
			consume(WordAtom.COMMA)
			consumeLineBreaks()
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
		val parameters = LinkedList<ValueSyntaxTreeNode>()
		var end = identifier.end
		if(currentWord?.type == WordAtom.OPENING_PARENTHESIS) {
			consume(WordAtom.OPENING_PARENTHESIS)
			if(currentWord?.type != WordAtom.CLOSING_PARENTHESIS && currentWord?.type != null) {
				parameters.add(parseExpression())
				while(currentWord?.type == WordAtom.COMMA) {
					consume(WordAtom.COMMA)
					parameters.add(parseExpression())
				}
			}
			end = consume(WordAtom.CLOSING_PARENTHESIS).end
		}
		return Instance(identifier, parameters, end)
	}

	/**
	 * InitializerDeclaration:
	 *   init[<ParameterList>] [<StatementSection>]
	 */
	private fun parseInitializerDefinition(): SyntaxTreeNode {
		val declarationWord = consume(WordAtom.INITIALIZER)
		val parameterList = if(currentWord?.type == WordAtom.OPENING_PARENTHESIS) parseParameterList() else null
		val body = if(currentWord?.type == WordAtom.OPENING_BRACE) parseStatementSection() else null
		val end = body?.end ?: parameterList?.end ?: declarationWord.end
		return InitializerDefinition(declarationWord.start, parameterList, body, end)
	}

	/**
	 * DeinitializerDeclaration:
	 *   deinit [<StatementSection>]
	 */
	private fun parseDeinitializerDefinition(): SyntaxTreeNode {
		val keyword = consume(WordAtom.DEINITIALIZER)
		val body = if(currentWord?.type == WordAtom.OPENING_BRACE) parseStatementSection() else null
		val end = body?.end ?: keyword.end
		return DeinitializerDefinition(keyword.start, end, body)
	}

	/**
	 * ComputedPropertyDeclaration:
	 *   <Identifier>[: <Type>][ <WhereClause>] [gets <Expression>] [sets <Statement>]
	 *   <Identifier>[: <Type>][ <WhereClause>] [gets <StatementSection>] [sets <Statement>]
	 */
	private fun parseComputedPropertyDeclaration(): ComputedPropertyDeclaration {
		val identifier = parseIdentifier()
		val type = if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			typeParser.parseType()
		} else null
		val whereClause = parseWhereClause()
		consumeLineBreaks()
		val getter = if(currentWord?.type == WordAtom.GETS) {
			consume(WordAtom.GETS)
			if(currentWord?.type == WordAtom.OPENING_BRACE)
				parseStatementSection()
			else
				parseExpression()
		} else null
		consumeLineBreaks()
		val setter = if(currentWord?.type == WordAtom.SETS) {
			consume(WordAtom.SETS)
			parseStatement()
		} else null
		return ComputedPropertyDeclaration(identifier, type, whereClause, getter, setter)
	}

	/**
	 * FunctionDefinition:
	 *   <Identifier><ParameterList>[: <Type>][ <WhereClause>] [<StatementSection>]
	 */
	private fun parseFunctionDefinition(): FunctionDefinition {
		val identifier = parseIdentifier()
		val parameterList = parseParameterList()
		val returnType = if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			typeParser.parseType()
		} else null
		val whereClause = parseWhereClause()
		val body = if(currentWord?.type == WordAtom.OPENING_BRACE)
			parseStatementSection()
		else null
		return FunctionDefinition(identifier, parameterList, returnType, whereClause, body)
	}

	/**
	 * OperatorDefinition:
	 *   operator <Operator>[<ParameterList>][: <Type>][ <WhereClause>] [<StatementSection>]
	 *   operator [<ParameterList>][<ParameterList>][: <Type>][ <WhereClause>] [<StatementSection>]
	 */
	private fun parseOperatorDefinition(): OperatorDefinition {
		val operator = if(currentWord?.type == WordAtom.OPENING_BRACKET) {
			val parameterList = parseParameterList(WordAtom.OPENING_BRACKET, WordAtom.CLOSING_BRACKET)
			IndexOperator(parameterList)
		} else {
			expressionParser.parseOperator(WordType.OPERATOR)
		}
		val parameterList = if(currentWord?.type == WordAtom.OPENING_PARENTHESIS)
			parseParameterList()
		else null
		val returnType = if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			typeParser.parseType()
		} else null
		val whereClause = parseWhereClause()
		val body = if(currentWord?.type == WordAtom.OPENING_BRACE)
			parseStatementSection()
		else null
		return OperatorDefinition(operator, parameterList, returnType, whereClause, body)
	}

	/**
	 * WhereClause:
	 *   where <WhereClauseCondition>[ and <WhereClauseCondition>]...
	 */
	private fun parseWhereClause(): WhereClause? {
		if(currentWord?.type !== WordAtom.WHERE)
			return null
		val start = consume(WordAtom.WHERE).start
		val conditions = LinkedList<WhereClauseCondition>()
		conditions.add(parseWhereClauseCondition())
		while(currentWord?.type == WordAtom.AND_OPERATOR) {
			consume(WordAtom.AND_OPERATOR)
			conditions.add(parseWhereClauseCondition())
		}
		return WhereClause(conditions, start)
	}

	/**
	 * WhereClauseCondition:
	 *   <Identifier> is <Type>
	 */
	private fun parseWhereClauseCondition(): WhereClauseCondition {
		val subject = parseIdentifier()
		consume(WordAtom.IS)
		val override = typeParser.parseType()
		return WhereClauseCondition(subject, override)
	}

	/**
	 * ParameterList:
	 *   <startWord>[[<Parameter>[, <Parameter>]...];][<Parameter>[, <Parameter>]...]<endWord>
	 */
	fun parseParameterList(startWord: WordAtom = WordAtom.OPENING_PARENTHESIS,
						   endWord: WordAtom = WordAtom.CLOSING_PARENTHESIS, startPosition: Position? = null):
		ParameterList {
		val start = startPosition ?: consume(startWord).start
		var genericParameters: List<Parameter>? = null
		var parameters = LinkedList<Parameter>()
		if(currentWord?.type != endWord && currentWord?.type != null) {
			if(currentWord?.type != WordAtom.SEMICOLON) {
				parameters.add(parseParameter())
				while(currentWord?.type == WordAtom.COMMA) {
					consume(WordAtom.COMMA)
					parameters.add(parseParameter())
				}
			}
			if(currentWord?.type == WordAtom.SEMICOLON) {
				consume(WordAtom.SEMICOLON)
				genericParameters = parameters
				parameters = LinkedList()
				if(currentWord?.type != endWord) {
					parameters.add(parseParameter())
					while(currentWord?.type == WordAtom.COMMA) {
						consume(WordAtom.COMMA)
						parameters.add(parseParameter())
					}
				}
			}
		}
		val end = consume(endWord).end
		return ParameterList(start, end, genericParameters, parameters)
	}

	/**
	 * Parameter:
	 *   <ModifierList> <Identifier>[: <Type>]
	 */
	private fun parseParameter(): Parameter {
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
	 *   <InstanceList>
	 *   <InitializerDefinition>
	 *   <DeinitializerDefinition>
	 *   <ComputedPropertySection>
	 *   <FunctionSection>
	 *   <OperatorSection>
	 *   <TypeDefinition>
	 *   <TypeAlias>
	 *   <ModifierSection>
	 */
	private fun parseDeclarationSection(context: DeclarationContext): SyntaxTreeNode {
		if(WordType.VARIABLE_DECLARATION.includes(currentWord?.type))
			return parseVariableSection(context)
		if(context == DeclarationContext.TYPE_DEFINITION) {
			if(currentWord?.type == WordAtom.INSTANCES)
				return parseInstanceList()
			if(currentWord?.type == WordAtom.INITIALIZER)
				return parseInitializerDefinition()
			if(currentWord?.type == WordAtom.DEINITIALIZER)
				return parseDeinitializerDefinition()
			if(currentWord?.type == WordAtom.COMPUTED)
				return parseComputedPropertySection()
			if(WordType.FUNCTION_DECLARATION.includes(currentWord?.type))
				return parseFunctionSection()
			if(currentWord?.type == WordAtom.OPERATOR)
				return parseOperatorSection()
		}
		if(WordType.TYPE_TYPE.includes(nextWord?.type))
			return parseTypeDefinition()
		if(currentWord?.type == WordAtom.TYPE_ALIAS)
			return parseTypeAlias()
		if(WordType.MODIFIER.includes(currentWord?.type))
			return parseModifierSection(context)
		val expectation = "declaration"
		throw UnexpectedWordError(syntaxTreeGenerator.getCurrentWord(expectation), expectation)
	}

	/**
	 * ModifierSection:
	 *   <ModifierList> <DeclarationSection>
	 *   <ModifierList> {
	 *   	<DeclarationSection>...
	 *   }
	 */
	private fun parseModifierSection(context: DeclarationContext): ModifierSection {
		val modifierList = parseModifierList()
			?: throw UnexpectedWordError(getCurrentWord(WordType.MODIFIER), WordType.MODIFIER)
		val sections = LinkedList<SyntaxTreeNode>()
		val end = if(currentWord?.type == WordAtom.OPENING_BRACE) {
			consume(WordAtom.OPENING_BRACE)
			while(currentWord?.type != WordAtom.CLOSING_BRACE && currentWord?.type != null) {
				consumeLineBreaks()
				if(currentWord?.type == WordAtom.CLOSING_BRACE || currentWord?.type == null)
					break
				try {
					sections.add(parseDeclarationSection(context))
				} catch(error: UserError) {
					handleUserError(error)
				}
			}
			consume(WordAtom.CLOSING_BRACE).end
		} else {
			sections.add(parseDeclarationSection(context))
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
	private fun parseVariableSection(context: DeclarationContext): VariableSection {
		val declarationType = consume(WordType.VARIABLE_DECLARATION)
		val type = if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			typeParser.parseType()
		} else null
		val value = if(currentWord?.type == WordAtom.ASSIGNMENT) {
			consume(WordAtom.ASSIGNMENT)
			parseExpression()
		} else null
		val variables = LinkedList<VariableSectionSyntaxTreeNode>()
		val end = if(currentWord?.type == WordAtom.OPENING_BRACE) {
			consume(WordAtom.OPENING_BRACE)
			while(currentWord?.type != WordAtom.CLOSING_BRACE && currentWord?.type != null) {
				consumeLineBreaks()
				if(currentWord?.type == WordAtom.CLOSING_BRACE || currentWord?.type == null)
					break
				try {
					variables.add(parseVariableSectionPart(context))
				} catch(error: UserError) {
					handleUserError(error)
				}
			}
			consume(WordAtom.CLOSING_BRACE).end
		} else {
			variables.add(parseVariableSectionPart(context))
			variables.last().end
		}
		return VariableSection(declarationType, type, value, variables, end)
	}

	/**
	 * VariableSectionPart:
	 *   <LocalVariableDeclaration>
	 *   <PropertyDeclaration>
	 */
	private fun parseVariableSectionPart(context: DeclarationContext): VariableSectionSyntaxTreeNode {
		return if(context == DeclarationContext.TYPE_DEFINITION)
			parseProperty()
		else
			parseLocalVariableDeclaration()
	}

	/**
	 * LocalVariableDeclaration:
	 *   <Identifier>[: <Type>] [= <Expression>]
	 */
	private fun parseLocalVariableDeclaration(): LocalVariableDeclaration {
		val identifier = parseIdentifier()
		val type = if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			typeParser.parseType()
		} else null
		consumeLineBreaks()
		var value: ValueSyntaxTreeNode? = null
		if(currentWord?.type == WordAtom.ASSIGNMENT) {
			consume(WordAtom.ASSIGNMENT)
			value = parseExpression()
		}
		return LocalVariableDeclaration(identifier, type, value)
	}

	/**
	 * PropertyDeclaration:
	 *   <Identifier>[: <Type>] [= <Expression>]
	 */
	private fun parseProperty(): VariableSectionSyntaxTreeNode {
		val identifier = parseIdentifier()
		val type = if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			typeParser.parseType()
		} else null
		var value: ValueSyntaxTreeNode? = null
		if(currentWord?.type == WordAtom.ASSIGNMENT) {
			consume(WordAtom.ASSIGNMENT)
			value = parseExpression()
		}
		return PropertyDeclaration(identifier, type, value)
	}

	/**
	 * ComputedPropertySection:
	 *   computed <ComputedPropertyDeclaration>
	 *   computed[: <Type>] {
	 *   	<ComputedPropertyDeclaration>...
	 *   }
	 */
	private fun parseComputedPropertySection(): ComputedPropertySection {
		val start = consume(WordAtom.COMPUTED).start
		val type = if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			typeParser.parseType()
		} else null
		val computedProperties = LinkedList<ComputedPropertyDeclaration>()
		val end = if(currentWord?.type == WordAtom.OPENING_BRACE) {
			consume(WordAtom.OPENING_BRACE)
			while(currentWord?.type != WordAtom.CLOSING_BRACE && currentWord?.type != null) {
				consumeLineBreaks()
				if(currentWord?.type == WordAtom.CLOSING_BRACE || currentWord?.type == null)
					break
				try {
					computedProperties.add(parseComputedPropertyDeclaration())
				} catch(error: UserError) {
					handleUserError(error)
				}
			}
			consume(WordAtom.CLOSING_BRACE).end
		} else {
			computedProperties.add(parseComputedPropertyDeclaration())
			computedProperties.last().end
		}
		return ComputedPropertySection(start, end, type, computedProperties)
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
		val end = if(currentWord?.type == WordAtom.OPENING_BRACE) {
			consume(WordAtom.OPENING_BRACE)
			while(currentWord?.type != WordAtom.CLOSING_BRACE && currentWord?.type != null) {
				consumeLineBreaks()
				if(currentWord?.type == WordAtom.CLOSING_BRACE || currentWord?.type == null)
					break
				try {
					functions.add(parseFunctionDefinition())
				} catch(error: UserError) {
					handleUserError(error)
				}
			}
			consume(WordAtom.CLOSING_BRACE).end
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
		val end = if(currentWord?.type == WordAtom.OPENING_BRACE) {
			consume(WordAtom.OPENING_BRACE)
			while(currentWord?.type != WordAtom.CLOSING_BRACE && currentWord?.type != null) {
				consumeLineBreaks()
				if(currentWord?.type == WordAtom.CLOSING_BRACE || currentWord?.type == null)
					break
				try {
					operators.add(parseOperatorDefinition())
				} catch(error: UserError) {
					handleUserError(error)
				}
			}
			consume(WordAtom.CLOSING_BRACE).end
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
	private fun parseGeneratorDefinition(): SyntaxTreeNode {
		val start = consume(WordAtom.GENERATOR).start
		val identifier = parseIdentifier()
		val parameterList = parseParameterList()
		consume(WordAtom.COLON)
		var keyReturnType: TypeSyntaxTreeNode? = null
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
