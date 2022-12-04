package components.semantic_analysis

import components.semantic_analysis.semantic_model.control_flow.FunctionCall
import components.semantic_analysis.semantic_model.operations.InstanceAccess
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.OptionalType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.values.VariableValueDeclaration
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class TypeInference {

	@Test
	fun `infers variable type in declaration`() {
		val sourceCode =
			"""
				Basketball class {
					init
				}
				val ball = Basketball()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValueDeclaration = lintResult.find<VariableValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.name == "ball" }
		val type = variableValueDeclaration?.value?.type
		assertNotNull(type)
		assertEquals(type, variableValueDeclaration.type)
	}

	@Test
	fun `resolves instance accesses in variable declarations`() {
		val sourceCode =
			"""
				TransportLayerProtocol enum {
					instances TCP, UDP
				}
				val protocol: TransportLayerProtocol = .TCP
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val type = lintResult.find<ObjectType> { type -> type.name == "TransportLayerProtocol" }
		assertNotNull(type)
		val instanceAccess = lintResult.find<InstanceAccess>()
		assertEquals(type, instanceAccess?.type)
	}

	@Test
	fun `resolves instance accesses in assignments`() {
		val sourceCode =
			"""
				TransportLayerProtocol enum {
					instances TCP, UDP
					init
				}
				var protocol: TransportLayerProtocol? = null
				protocol = .TCP
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val type = lintResult.find<ObjectType> { type -> type.name == "TransportLayerProtocol" }
		assertNotNull(type)
		val instanceAccess = lintResult.find<InstanceAccess>()
		assertEquals(type, instanceAccess?.type)
	}

	@Test
	fun `resolves instance accesses in initializer calls`() {
		val sourceCode =
			"""
				TransportLayerProtocol enum {
					instances TCP, UDP
					init
				}
				Stream class {
					val protocol: TransportLayerProtocol

					init(protocol)
				}
				val stream = Stream(.TCP)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val type = lintResult.find<ObjectType> { type -> type.name == "TransportLayerProtocol" }
		assertNotNull(type)
		val instanceAccess = lintResult.find<InstanceAccess>()
		assertEquals(type, instanceAccess?.type)
	}

	@Test
	fun `resolves instance accesses in function calls`() {
		val sourceCode =
			"""
				TransportLayerProtocol enum {
					instances TCP, UDP
					init
				}
				Port class {}
				NetworkInterface object {
					to getOpenPort(protocol: TransportLayerProtocol): Port {}
				}
				val openUdpPort = NetworkInterface.getOpenPort(.UDP)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val type = lintResult.find<ObjectType> { type -> type.name == "TransportLayerProtocol" }
		assertNotNull(type)
		val instanceAccess = lintResult.find<InstanceAccess>()
		assertEquals(type, instanceAccess?.type)
	}

	@Test
	fun `resolves instance accesses in operator calls`() {
		val sourceCode =
			"""
				TransportLayerProtocol enum {
					instances TCP, UDP
					init
				}
				Ports class {}
				NetworkInterface object {
					operator [protocol: TransportLayerProtocol](): Ports {}
				}
				val udpPorts = NetworkInterface[.UDP]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val type = lintResult.find<ObjectType> { type -> type.name == "TransportLayerProtocol" }
		assertNotNull(type)
		val instanceAccess = lintResult.find<InstanceAccess>()
		assertEquals(type, instanceAccess?.type)
	}

	@Test
	fun `resolves instance accesses in switch cases`() {
		val sourceCode =
			"""
				TransportLayerProtocol enum {
					instances TCP, UDP
					init
				}
				val protocol = TransportLayerProtocol.TCP
				switch protocol {
					.TCP:
						cli.printLine("TCP")
					.UDP:
						cli.printLine("UDP")
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val type = lintResult.find<ObjectType> { type -> type.name == "TransportLayerProtocol" }
		assertNotNull(type)
		val instanceAccess = lintResult.find<InstanceAccess>()
		assertEquals(type, instanceAccess?.type)
	}

	@Test
	fun `allows for recursive use of generic types`() {
		val sourceCode =
			"""
				Receipt class {}
				List class {
					containing Item
					var backup: <Item>List? = null
					init
				}
				val receipts = <Receipt>List()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerResult = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function.type as? StaticType)?.definition?.name == "List" }?.type as? ObjectType
		assertNotNull(initializerResult)
	}

	@Test
	fun `allows for recursive use of generic function`() {
		val sourceCode =
			"""
				referencing Pure
				Plant class {
					init
				}
				Package class {
					containing Item
					val item: Item
					init(item)
				}
				PackageOpener object {
					to unwrap(Item; package: <Item>Package): Item {
						if(package.item is p: <Any producing>Package)
							return unwrap(p)
						return package.item
					}
				}
				val plant = PackageOpener.unwrap(Package(Plant()))
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val variableValueDeclaration = lintResult.find<VariableValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.name == "plant" }
		assertNotNull(variableValueDeclaration?.type)
	}

	@Test
	fun `emits errors when generic type can't be inferred`() {
		val sourceCode =
			"""
				Box class {
					containing Item
					init
				}
				val letterBox = Box()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Initializer 'Box()' hasn't been declared yet")
	}

	@Test
	fun `infers generic type before constructor call`() {
		val sourceCode =
			"""
				Letter class {
					init
				}
				Box class {
					containing Item
					val firstItem: Item
					init(firstItem)
				}
				val letterBox = Box(Letter())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val genericParameter = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function.type as? StaticType)?.definition?.name == "Letter" }?.type
		val variableValueDeclaration = lintResult.find<VariableValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.name == "letterBox" }
		val returnType = variableValueDeclaration?.type as? ObjectType
		assertNotNull(returnType)
		assertEquals(genericParameter, returnType.typeParameters.firstOrNull())
	}

	@Test
	fun `infers generic type in constructor call`() {
		val sourceCode =
			"""
				List class {
					containing Item
					init
					to add(item: Item) {}
				}
				Message class {
					var actions: <Message>Actions
				}
				NewsletterMessage class: Message {}
				Actions class {
					containing M: Message
					init
				}
				Account class {
					val incomingMessages = <Message>List()
					init
				}
				MailFolder class {
					val messages: <Message>List
					init(MessageType: Message; account: Account, availableActions: <MessageType>Actions) {
						loop over account.incomingMessages as incomingMessage {
							if(incomingMessage is MessageType) {
								incomingMessage.actions = availableActions
								messages.add(incomingMessage)
							}
						}
					}
				}
				val spamFolder = MailFolder(Account(), <NewsletterMessage>Actions())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerResult = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function.type as? StaticType)?.definition?.name == "MailFolder" }?.type as? ObjectType
		assertNotNull(initializerResult)
	}

	@Test
	fun `infers generic type in function call`() {
		val sourceCode =
			"""
				Letter class {}
				PostCard class: Letter {
					init
				}
				PostOffice object {
					to stamp(L: Letter; letter: L): L {
						return letter
					}
				}
				val stampedPostCard = PostOffice.stamp(PostCard())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val genericParameter = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function.type as? StaticType)?.definition?.name == "PostCard" }?.type
		val variableValueDeclaration = lintResult.find<VariableValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.name == "stampedPostCard" }
		val returnType = variableValueDeclaration?.type as? ObjectType
		assertNotNull(returnType)
		assertEquals(genericParameter, returnType)
	}

	@Test
	fun `infers generic type in function call with optional type usage`() {
		val sourceCode =
			"""
				Letter class {}
				PostCard class: Letter {
					init
				}
				PostOffice object {
					to stamp(L: Letter; letter: L?): L? {
						return letter
					}
				}
				val stampedPostCard = PostOffice.stamp(PostCard())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val genericParameter = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function.type as? StaticType)?.definition?.name == "PostCard" }?.type
		val variableValueDeclaration = lintResult.find<VariableValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.name == "stampedPostCard" }
		val returnType = variableValueDeclaration?.type as? OptionalType
		assertNotNull(returnType)
		assertEquals(genericParameter, returnType.baseType)
	}

	@Test
	fun `infers generic type in operator call`() {
		val sourceCode =
			"""
				IpAddress class {}
				Ipv4Address class: IpAddress {
					init
				}
				Ipv6Address class: IpAddress {
					init
				}
				Client class {
					containing A: IpAddress
					init
				}
				Server object {
					operator [A: IpAddress; ipAddress: A]: <A>Client {}
				}
				val client = Server[Ipv4Address()]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val genericParameter = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function.type as? StaticType)?.definition?.name == "Ipv4Address" }?.type
		val variableValueDeclaration = lintResult.find<VariableValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.name == "client" }
		val returnType = variableValueDeclaration?.type as? ObjectType
		assertNotNull(returnType)
		assertEquals(genericParameter, returnType.typeParameters.firstOrNull())
	}
}
