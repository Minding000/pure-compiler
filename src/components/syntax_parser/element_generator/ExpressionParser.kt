package components.syntax_parser.element_generator

import components.syntax_parser.syntax_tree.access.IndexAccess
import components.syntax_parser.syntax_tree.access.InstanceAccess
import components.syntax_parser.syntax_tree.access.MemberAccess
import components.syntax_parser.syntax_tree.control_flow.*
import components.syntax_parser.syntax_tree.definitions.LambdaFunctionDefinition
import components.syntax_parser.syntax_tree.definitions.Operator
import components.syntax_parser.syntax_tree.definitions.TypeSpecification
import components.syntax_parser.syntax_tree.general.ForeignLanguageExpression
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.*
import components.syntax_parser.syntax_tree.operations.BinaryOperator
import components.syntax_parser.syntax_tree.operations.Cast
import components.syntax_parser.syntax_tree.operations.HasValueCheck
import components.syntax_parser.syntax_tree.operations.UnaryOperator
import components.tokenizer.Word
import components.tokenizer.WordAtom
import components.tokenizer.WordDescriptor
import components.tokenizer.WordType
import errors.internal.CompilerError
import errors.user.UnexpectedWordError
import source_structure.Position
import java.util.*
import java.util.regex.Pattern

class ExpressionParser(private val syntaxTreeGenerator: SyntaxTreeGenerator): Generator() {
	private val typeParameterListCheck: Pattern = Pattern.compile("[;()\\[\\]]")
	override val currentWord: Word?
		get() = syntaxTreeGenerator.currentWord
	override val nextWord: Word?
		get() = syntaxTreeGenerator.nextWord
	override var parseForeignLanguageLiteralNext: Boolean
		get() = syntaxTreeGenerator.parseForeignLanguageLiteralNext
		set(value) { syntaxTreeGenerator.parseForeignLanguageLiteralNext = value }

	private val statementParser
		get() = syntaxTreeGenerator.statementParser
	private val typeParser
		get() = syntaxTreeGenerator.typeParser
	private val literalParser
		get() = syntaxTreeGenerator.literalParser

	override fun getCurrentPosition(): Position = syntaxTreeGenerator.getCurrentPosition()

	override fun consume(type: WordDescriptor): Word {
		return syntaxTreeGenerator.consume(type)
	}

	/**
	 * Expression:
	 *   <IfExpression>
	 *   <SwitchExpression>
	 *   <BinaryBooleanExpression>
	 */
	fun parseExpression(): ValueSyntaxTreeNode {
		return when(currentWord?.type) {
			WordAtom.IF -> parseIfExpression()
			WordAtom.SWITCH -> parseSwitchExpression()

			else -> parseBinaryBooleanExpression()
		}
	}

	/**
	 * IfExpression:
	 *   if <Expression>
	 *       <Statement>
	 *   [else
	 *   	 <Statement>]
	 */
	fun parseIfExpression(isPartOfExpression: Boolean = true): IfExpression {
		val start = consume(WordAtom.IF).start
		val condition = parseExpression()
		consumeLineBreaks()
		val positiveBranch = statementParser.parseStatement()
		consumeLineBreaks()
		var negativeBranch: SyntaxTreeNode? = null
		if(currentWord?.type == WordAtom.ELSE) {
			consume(WordAtom.ELSE)
			consumeLineBreaks()
			negativeBranch = statementParser.parseStatement()
		}
		return IfExpression(condition, positiveBranch, negativeBranch, isPartOfExpression, start,
			negativeBranch?.end ?: positiveBranch.end)
	}

	/**
	 * SwitchExpression:
	 *   switch <Expression> {
	 *       <Expression>: <Statement>
	 *       [else: <Statement>]
	 *   }
	 */
	fun parseSwitchExpression(isPartOfExpression: Boolean = true): SwitchExpression {
		val start = consume(WordAtom.SWITCH).start
		val subject = parseExpression()
		consume(WordAtom.OPENING_BRACE)
		consumeLineBreaks()
		val cases = LinkedList<Case>()
		var elseResult: SyntaxTreeNode? = null
		while(currentWord?.type !== WordAtom.CLOSING_BRACE) {
			consumeLineBreaks()
			if(currentWord?.type == WordAtom.ELSE) {
				consume(WordAtom.ELSE)
				consume(WordAtom.COLON)
				consumeLineBreaks()
				elseResult = statementParser.parseStatement()
				consumeLineBreaks()
				break
			}
			cases.add(parseCase())
			consumeLineBreaks()
		}
		val end = consume(WordAtom.CLOSING_BRACE).end
		return SwitchExpression(subject, cases, elseResult, isPartOfExpression, start, end)
	}

	/**
	 * Case:
	 *   <Expression>: <Statement>
	 */
	private fun parseCase(): Case {
		val condition = parseExpression()
		consume(WordAtom.COLON)
		consumeLineBreaks()
		return Case(condition, statementParser.parseStatement())
	}

	/**
	 * BinaryBooleanExpression:
	 *   <EqualityComparison>
	 *   <EqualityComparison> and <EqualityComparison>
	 *   <EqualityComparison> or <EqualityComparison>
	 */
	private fun parseBinaryBooleanExpression(): ValueSyntaxTreeNode {
		var expression: ValueSyntaxTreeNode = parseEqualityComparison()
		while(WordType.BINARY_BOOLEAN_OPERATOR.includes(currentWord?.type)) {
			val operator = parseOperator(WordType.BINARY_BOOLEAN_OPERATOR)
			expression = BinaryOperator(expression, parseEqualityComparison(), operator)
		}
		return expression
	}

	/**
	 * EqualityComparison:
	 *   <IdentityComparison>
	 *   <IdentityComparison> == <IdentityComparison>
	 *   <IdentityComparison> != <IdentityComparison>
	 */
	private fun parseEqualityComparison(): ValueSyntaxTreeNode {
		var expression: ValueSyntaxTreeNode = parseIdentityComparison()
		while(WordType.EQUALITY_COMPARISON.includes(currentWord?.type)) {
			val operator = parseOperator(WordType.EQUALITY_COMPARISON)
			expression = BinaryOperator(expression, parseIdentityComparison(), operator)
		}
		return expression
	}

	/**
	 * IdentityComparison:
	 *   <ValueComparison>
	 *   <ValueComparison> >= <ValueComparison>
	 *   <ValueComparison> <= <ValueComparison>
	 *   <ValueComparison> > <ValueComparison>
	 *   <ValueComparison> < <ValueComparison>
	 */
	private fun parseIdentityComparison(): ValueSyntaxTreeNode {
		var expression: ValueSyntaxTreeNode = parseValueComparison()
		while(WordType.IDENTITY_COMPARISON.includes(currentWord?.type)) {
			val operator = parseOperator(WordType.IDENTITY_COMPARISON)
			expression = BinaryOperator(expression, parseValueComparison(), operator)
		}
		return expression
	}

	/**
	 * Comparison:
	 *   <Addition>
	 *   <Addition> >= <Addition>
	 *   <Addition> <= <Addition>
	 *   <Addition> > <Addition>
	 *   <Addition> < <Addition>
	 */
	private fun parseValueComparison(): ValueSyntaxTreeNode {
		var expression: ValueSyntaxTreeNode = parseAddition()
		while(WordType.VALUE_COMPARISON.includes(currentWord?.type)) {
			val operator = parseOperator(WordType.VALUE_COMPARISON)
			expression = BinaryOperator(expression, parseAddition(), operator)
		}
		return expression
	}

	/**
	 * Addition:
	 *   <Multiplication>
	 *   <Multiplication> + <Multiplication>
	 *   <Multiplication> - <Multiplication>
	 */
	private fun parseAddition(): ValueSyntaxTreeNode {
		var expression: ValueSyntaxTreeNode = parseMultiplication()
		while(WordType.ADDITION.includes(currentWord?.type)) {
			val operator = parseOperator(WordType.ADDITION)
			expression = BinaryOperator(expression, parseMultiplication(), operator)
		}
		return expression
	}

	/**
	 * Multiplication:
	 *   <NullCoalescence>
	 *   <NullCoalescence> * <NullCoalescence>
	 *   <NullCoalescence> / <NullCoalescence>
	 */
	private fun parseMultiplication(): ValueSyntaxTreeNode {
		var expression: ValueSyntaxTreeNode = parseNullCoalescence()
		while(WordType.MULTIPLICATION.includes(currentWord?.type)) {
			val operator = parseOperator(WordType.MULTIPLICATION)
			expression = BinaryOperator(expression, parseNullCoalescence(), operator)
		}
		return expression
	}

	/**
	 * NullCoalescence:
	 *   <Cast>
	 *   <Cast> ?? <Cast>
	 */
	private fun parseNullCoalescence(): ValueSyntaxTreeNode {
		var expression: ValueSyntaxTreeNode = parseCast()
		while(currentWord?.type == WordAtom.NULL_COALESCENCE) {
			val operator = parseOperator(WordAtom.NULL_COALESCENCE)
			expression = BinaryOperator(expression, parseCast(), operator)
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
	private fun parseCast(): ValueSyntaxTreeNode {
		var expression: ValueSyntaxTreeNode = parseTry()
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
	fun parseTry(): ValueSyntaxTreeNode {
		if(WordType.TRY.includes(currentWord?.type)) {
			val operator = consume(WordType.TRY)
			return Try(parseUnaryOperator(), operator.type == WordAtom.OPTIONAL_TRY, operator.start)
		}
		return parseUnaryOperator()
	}

	/**
	 * UnaryOperator:
	 *   <HasValueCheck>
	 *   !<HasValueCheck>
	 *   +<HasValueCheck>
	 *   -<HasValueCheck>
	 *   ...<HasValueCheck>
	 */
	private fun parseUnaryOperator(): ValueSyntaxTreeNode {
		if(WordType.UNARY_OPERATOR.includes(currentWord?.type)) {
			val operator = parseOperator(WordType.UNARY_OPERATOR)
			return UnaryOperator(parseHasValueCheck(), operator)
		}
		return parseHasValueCheck()
	}

	/**
	 * HasValueCheck:
	 *   <Accessor>?
	 */
	private fun parseHasValueCheck(): ValueSyntaxTreeNode {
		val accessor = parseAccessor()
		if(currentWord?.type == WordAtom.QUESTION_MARK) {
			consume(WordAtom.QUESTION_MARK)
			return HasValueCheck(accessor)
		}
		return accessor
	}

	/**
	 * Accessor:
	 *   <Primary>
	 *   <Accessor><MemberAccess>
	 *   <Accessor><FunctionCall>
	 *   <Accessor><Index>
	 */
	private fun parseAccessor(): ValueSyntaxTreeNode {
		var expression = parsePrimary()
		while(WordType.ACCESSOR.includes(currentWord?.type)) {
			expression = if(WordType.MEMBER_ACCESSOR.includes(currentWord?.type))
				parseMemberAccess(expression)
			else if(currentWord?.type == WordAtom.OPENING_PARENTHESIS)
				parseFunctionCall(expression)
			else if(currentWord?.type == WordAtom.OPENING_BRACKET)
				parseIndex(expression)
			else
				throw CompilerError("Failed to parse accessor: '${currentWord?.type}'")
		}
		return expression
	}

	/**
	 * MemberAccess:
	 *   <Accessor>[?].<Identifier>
	 *   <Accessor>[?].<InitializerReference>
	 *   <Accessor>[?].<TypeSpecification>
	 */
	private fun parseMemberAccess(rootExpression: ValueSyntaxTreeNode): MemberAccess {
		val accessor = consume(WordType.MEMBER_ACCESSOR)
		val memberExpression = if(WordType.GENERICS_START.includes(currentWord?.type))
			parseTypeSpecification()
		else if(currentWord?.type == WordAtom.INITIALIZER)
			parseInitializerReference()
		else
			literalParser.parseIdentifier()
		return MemberAccess(rootExpression, memberExpression, accessor.type == WordAtom.OPTIONAL_ACCESSOR)
	}

	/**
	 * Index:
	 *   <Accessor>[[[<Type>[, <Type>]...];][<Expression>[, <Expression>]...]]
	 */
	private fun parseIndex(expression: ValueSyntaxTreeNode): ValueSyntaxTreeNode {
		return parseCall(expression, WordAtom.OPENING_BRACKET, WordAtom.CLOSING_BRACKET)
	}

	/**
	 * FunctionCall:
	 *   <Accessor>([[<Type>[, <Type>]...];][<Expression>[, <Expression>]...])
	 */
	private fun parseFunctionCall(expression: ValueSyntaxTreeNode): ValueSyntaxTreeNode {
		return parseCall(expression, WordAtom.OPENING_PARENTHESIS, WordAtom.CLOSING_PARENTHESIS)
	}

	/**
	 * This is a helper for:
	 * <FunctionCall>
	 * <Index>
	 */
	private fun parseCall(expression: ValueSyntaxTreeNode, startWord: WordAtom, endWord: WordAtom): ValueSyntaxTreeNode {
		val parameterListStart = consume(startWord).end
		var typeParameters: List<TypeSyntaxTreeNode>? = null
		val nextSpecialCharacter = syntaxTreeGenerator.wordGenerator.scanForCharacters(parameterListStart,
			typeParameterListCheck)
		if(nextSpecialCharacter == ';') {
			typeParameters = LinkedList()
			typeParameters.add(typeParser.parseType())
			while(currentWord?.type == WordAtom.COMMA) {
				consume(WordAtom.COMMA)
				typeParameters.add(typeParser.parseType())
			}
			consume(WordAtom.SEMICOLON)
		}
		val valueParameters = LinkedList<ValueSyntaxTreeNode>()
		if(currentWord?.type != endWord) {
			valueParameters.add(parseExpression())
			while(currentWord?.type == WordAtom.COMMA) {
				consume(WordAtom.COMMA)
				valueParameters.add(parseExpression())
			}
		}
		val end = consume(endWord).end
		return if(startWord == WordAtom.OPENING_PARENTHESIS)
			FunctionCall(expression, typeParameters, valueParameters, end)
		else
			IndexAccess(expression, typeParameters, valueParameters, end)
	}

	/**
	 * Primary:
	 *   <Atom>
	 *   (<Expression>)
	 *   <LambdaFunction>
	 */
	private fun parsePrimary(): ValueSyntaxTreeNode {
		if(currentWord?.type == WordAtom.OPENING_PARENTHESIS) {
			val start = consume(WordAtom.OPENING_PARENTHESIS).start
			val isEmptyParameterListPresent = currentWord?.type == WordAtom.CLOSING_PARENTHESIS
					&& nextWord?.type == WordAtom.ARROW
			val isParameterModifierPresent = WordType.MODIFIER.includes(currentWord?.type)
			val isParameterPresent = currentWord?.type == WordAtom.IDENTIFIER
				&& nextWord?.type in listOf(WordAtom.COMMA, WordAtom.SEMICOLON, WordAtom.COLON)
			if(isEmptyParameterListPresent || isParameterModifierPresent || isParameterPresent)
				return parseLambdaFunctionDefinition(start)
			val expression = parseExpression()
			consume(WordAtom.CLOSING_PARENTHESIS)
			return expression
		}
		return parseAtom()
	}

	/**
	 * LambdaFunction:
	 *   (<ParameterList>)[: <Type>] => <Statement>
	 */
	private fun parseLambdaFunctionDefinition(start: Position): LambdaFunctionDefinition {
		val parameterList = statementParser.parseParameterList(
			WordAtom.OPENING_PARENTHESIS, WordAtom.CLOSING_PARENTHESIS,
			start)
		val returnType = if(currentWord?.type == WordAtom.COLON) {
			consume(WordAtom.COLON)
			typeParser.parseType()
		} else null
		consume(WordAtom.ARROW)
		val body = statementParser.parseStatementSection()
		return LambdaFunctionDefinition(start, parameterList, body, returnType)
	}

	/**
	 * Atom:
	 *   <NullLiteral>
	 *   <BooleanLiteral>
	 *   <NumberLiteral>
	 *   <StringLiteral>
	 *   <SelfReference>
	 *   <SuperReference>
	 *   <InitializerReference>
	 *   <ForeignLanguageExpression>
	 *   <Identifier>
	 *   <InstanceAccess>
	 *   <TypeSpecification>
	 */
	private fun parseAtom(): ValueSyntaxTreeNode {
		val word = getCurrentWord("atom")
		return when(word.type) {
			WordAtom.NULL_LITERAL -> literalParser.parseNullLiteral()
			WordAtom.BOOLEAN_LITERAL -> literalParser.parseBooleanLiteral()
			WordAtom.NUMBER_LITERAL -> literalParser.parseNumberLiteral()
			WordAtom.STRING_LITERAL -> literalParser.parseStringLiteral()
			WordAtom.SELF_REFERENCE -> parseSelfReference()
			WordAtom.SUPER_REFERENCE -> parseSuperReference()
			WordAtom.INITIALIZER -> parseInitializerReference()
			WordAtom.IDENTIFIER -> {
				when(nextWord?.type) {
					WordAtom.FOREIGN_EXPRESSION -> parseForeignLanguageExpression()
					else -> literalParser.parseIdentifier()
				}
			}
			WordAtom.DOT -> parseInstanceAccess()
			else -> {
				if(WordType.GENERICS_START.includes(word.type))
					parseTypeSpecification()
				else
					throw UnexpectedWordError(word, "atom")
			}
		}
	}

	/**
	 * SelfReference:
	 *   <self-reference>[<<ObjectType>>]
	 */
	private fun parseSelfReference(): SelfReference {
		val word = consume(WordAtom.SELF_REFERENCE)
		var end = word.end
		var specifier: ObjectType? = null
		if(WordType.GENERICS_START.includes(currentWord?.type)) {
			consume(WordType.GENERICS_START)
			specifier = typeParser.parseObjectType(false)
			end = consume(WordType.GENERICS_END).end
		}
		return SelfReference(word, specifier, end)
	}

	/**
	 * SuperReference:
	 *   <super-reference>[<<ObjectType>>]
	 */
	private fun parseSuperReference(): SuperReference {
		val word = consume(WordAtom.SUPER_REFERENCE)
		var end = word.end
		var type: ObjectType? = null
		if(WordType.GENERICS_START.includes(currentWord?.type)) {
			consume(WordType.GENERICS_START)
			type = typeParser.parseObjectType()
			end = consume(WordType.GENERICS_END).end
		}
		return SuperReference(word, type, end)
	}

	/**
	 * InitializerReference:
	 *   <init>
	 */
	private fun parseInitializerReference(): InitializerReference {
		return InitializerReference(consume(WordAtom.INITIALIZER))
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
	 * InstanceAccess:
	 *   .<Identifier>
	 */
	private fun parseInstanceAccess(): InstanceAccess {
		val start = consume(WordAtom.DOT).start
		val identifier = literalParser.parseIdentifier()
		return InstanceAccess(start, identifier)
	}

	/**
	 * TypeSpecification:
	 *   <TypeList><Identifier>
	 */
	private fun parseTypeSpecification(): TypeSpecification {
		val typeList = typeParser.parseTypeList()
		val identifier = literalParser.parseIdentifier()
		return TypeSpecification(typeList, identifier)
	}

	/**
	 * Operator:
	 *   <operator>
	 */
	fun parseOperator(descriptor: WordDescriptor): Operator {
		return Operator(consume(descriptor))
	}
}
