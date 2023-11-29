package components.semantic_model.types

import components.semantic_model.operations.FunctionCall
import components.semantic_model.operations.InstanceAccess
import components.semantic_model.values.ValueDeclaration
import logger.issues.resolution.NotFound
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class TypeInference {

	@Test
	fun `infers variable type in declaration`() {
		val sourceCode =
			"""
				Basketball class
				val ball = Basketball()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val valueDeclaration = lintResult.find<ValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.name == "ball" }
		val type = valueDeclaration?.value?.type
		assertNotNull(type)
		assertEquals(type, valueDeclaration.type)
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
	fun `resolves instance accesses in variadic initializer calls`() {
		val sourceCode =
			"""
				TransportLayerProtocol enum {
					instances TCP, UDP
				}
				Stream class {
					val protocols: ...TransportLayerProtocol

					init(...protocols)
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
				}
				Ports class
				NetworkInterface object {
					to getOpenPorts(protocol: TransportLayerProtocol): Ports
				}
				val openUdpPort = NetworkInterface.getOpenPorts(.UDP)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val type = lintResult.find<ObjectType> { type -> type.name == "TransportLayerProtocol" }
		assertNotNull(type)
		val instanceAccess = lintResult.find<InstanceAccess>()
		assertEquals(type, instanceAccess?.type)
	}

	@Test
	fun `resolves instance accesses in variadic function calls`() {
		val sourceCode =
			"""
				TransportLayerProtocol enum {
					instances TCP, UDP
				}
				Ports class
				NetworkInterface object {
					to getOpenPorts(...protocols: ...TransportLayerProtocol): Ports
				}
				val openUdpPort = NetworkInterface.getOpenPorts(.UDP)
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
				}
				Ports class
				NetworkInterface object {
					operator [protocol: TransportLayerProtocol](): Ports
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
	fun `resolves instance accesses in variadic operator calls`() {
		val sourceCode =
			"""
				TransportLayerProtocol enum {
					instances TCP, UDP
				}
				Ports class
				NetworkInterface object {
					operator [...protocols: ...TransportLayerProtocol](): Ports
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
				}
				val protocol = TransportLayerProtocol.TCP
				var isLosslessProtocol = no
				switch protocol {
					.TCP:
						isLosslessProtocol = yes
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotFound>()
		val instanceAccess = lintResult.find<InstanceAccess>()
		assertEquals("TransportLayerProtocol", instanceAccess?.type.toString())
	}

	@Test
	fun `resolves instance accesses in return statements`() {
		val sourceCode =
			"""
				TransportLayerProtocol enum {
					instances TCP, UDP
				}
				Networking object {
					to getLosslessProtocol(): TransportLayerProtocol {
						return .TCP
					}
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
				Receipt class
				List class {
					containing Item
					var backup: <Item>List? = null
				}
				val receipts = <Receipt>List()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val initializerResult = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function.type as? StaticType)?.typeDeclaration?.name == "List" }?.type as? ObjectType
		assertNotNull(initializerResult)
	}

	@Test
	fun `allows for recursive use of generic function`() {
		val sourceCode =
			"""
				referencing Pure
				Plant class
				Package class {
					containing Item
					val item: Item
					init(item)
				}
				PackageOpener object {
					to unwrap(Item; package: <Item>Package): Item {
						if package.item is p: <Any producing>Package
							return unwrap(p)
						return package.item
					}
				}
				val plant = PackageOpener.unwrap(Package(Plant()))
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val valueDeclaration = lintResult.find<ValueDeclaration> { variableValueDeclaration -> variableValueDeclaration.name == "plant" }
		assertNotNull(valueDeclaration?.type)
	}

	@Test
	fun `emits errors when generic type can't be inferred`() {
		val sourceCode =
			"""
				Box class {
					containing Item
				}
				val letterBox = Box()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<NotFound>("Initializer 'Box()' hasn't been declared yet.")
	}

	@Test
	fun `infers generic type before constructor call`() {
		val sourceCode =
			"""
				Letter class
				Box class {
					containing Item
					val firstItem: Item
					init(firstItem)
				}
				val letterBox = Box(Letter())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val genericParameter = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function.type as? StaticType)?.typeDeclaration?.name == "Letter" }?.type
		val valueDeclaration = lintResult.find<ValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.name == "letterBox" }
		val returnType = valueDeclaration?.type as? ObjectType
		assertNotNull(returnType)
		assertEquals(genericParameter, returnType.typeParameters.firstOrNull())
	}

	@Test
	fun `infers generic type in initializer call`() {
		val sourceCode =
			"""
				List class {
					containing Item
					to add(item: Item) {}
				}
				Message class {
					var actions: <Message>Actions
				}
				NewsletterMessage class: Message
				Actions class {
					containing M: Message
				}
				Account class {
					val incomingMessages = <Message>List()
				}
				MailFolder class {
					val messages: <Message>List
					init(MessageType: Message; account: Account, availableActions: <MessageType>Actions) {
						loop over account.incomingMessages as incomingMessage {
							if incomingMessage is MessageType {
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
			(functionCall.function.type as? StaticType)?.typeDeclaration?.name == "MailFolder" }?.type as? ObjectType
		assertNotNull(initializerResult)
	}

	@Test
	fun `infers generic type in variadic initializer call`() {
		val sourceCode =
			"""
				List class {
					containing Item
					to add(item: Item) {}
				}
				Message class {
					var actions: ...<Message>Action
				}
				NewsletterMessage class: Message
				Action class {
					containing M: Message
				}
				Account class {
					val incomingMessages = <Message>List()
				}
				MailFolder class {
					val messages: <Message>List
					init(MessageType: Message; account: Account, ...availableActions: ...<MessageType>Action) {
						loop over account.incomingMessages as incomingMessage {
							if incomingMessage is MessageType {
								incomingMessage.actions = availableActions
								messages.add(incomingMessage)
							}
						}
					}
				}
				val spamFolder = MailFolder(Account(), <NewsletterMessage>Action())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerResult = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function.type as? StaticType)?.typeDeclaration?.name == "MailFolder" }?.type as? ObjectType
		assertNotNull(initializerResult)
	}

	@Test
	fun `infers generic type in function call`() {
		val sourceCode =
			"""
				Letter class
				PostCard class: Letter
				PostOffice object {
					to stamp(L: Letter; letter: L): L {
						return letter
					}
				}
				val stampedPostCard = PostOffice.stamp(PostCard())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val genericParameter = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function.type as? StaticType)?.typeDeclaration?.name == "PostCard" }?.type
		val valueDeclaration = lintResult.find<ValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.name == "stampedPostCard" }
		val returnType = valueDeclaration?.type as? ObjectType
		assertNotNull(returnType)
		assertEquals(genericParameter, returnType)
	}

	@Test
	fun `infers generic type in function call with optional type usage`() {
		val sourceCode =
			"""
				Letter class
				PostCard class: Letter
				PostOffice object {
					to stamp(L: Letter; letter: L?): L? {
						return letter
					}
				}
				val stampedPostCard = PostOffice.stamp(PostCard())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val genericParameter = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function.type as? StaticType)?.typeDeclaration?.name == "PostCard" }?.type
		val valueDeclaration = lintResult.find<ValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.name == "stampedPostCard" }
		val returnType = valueDeclaration?.type as? OptionalType
		assertNotNull(returnType)
		assertEquals(genericParameter, returnType.baseType)
	}

	@Test
	fun `infers generic type in variadic function call`() {
		val sourceCode =
			"""
				Letter class
				PostCard class: Letter
				PostOffice object {
					to stamp(L: Letter; ...letters: ...L): ...L {
						return letters
					}
				}
				val stampedPostCard = PostOffice.stamp(PostCard())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val genericParameter = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function.type as? StaticType)?.typeDeclaration?.name == "PostCard" }?.type
		val valueDeclaration = lintResult.find<ValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.name == "stampedPostCard" }
		val returnType = valueDeclaration?.type as? PluralType
		assertNotNull(returnType)
		assertEquals(genericParameter, returnType.baseType)
	}

	@Test
	fun `infers generic type in operator call`() {
		val sourceCode =
			"""
				IpAddress class
				Ipv4Address class: IpAddress
				Ipv6Address class: IpAddress
				Client class {
					containing A: IpAddress
				}
				Server object {
					operator [A: IpAddress; ipAddress: A]: <A>Client
				}
				val client = Server[Ipv4Address()]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val genericParameter = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function.type as? StaticType)?.typeDeclaration?.name == "Ipv4Address" }?.type
		val valueDeclaration = lintResult.find<ValueDeclaration> { variableValueDeclaration ->
			variableValueDeclaration.name == "client" }
		val returnType = valueDeclaration?.type as? ObjectType
		assertNotNull(returnType)
		assertEquals(genericParameter, returnType.typeParameters.firstOrNull())
	}
}
