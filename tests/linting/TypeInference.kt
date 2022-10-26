package linting

import linting.semantic_model.control_flow.FunctionCall
import linting.semantic_model.operations.InstanceAccess
import linting.semantic_model.definitions.TypeDefinition
import linting.semantic_model.types.ObjectType
import linting.semantic_model.types.OptionalType
import linting.semantic_model.types.StaticType
import linting.semantic_model.types.Type
import linting.semantic_model.values.VariableValueDeclaration
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TypeInference {

	@Test
	fun `infers variable type in declaration`() {
		val sourceCode =
			"""
				class Basketball {
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
				enum TransportLayerProtocol {
					instances TCP, UDP
				}
				val protocol: TransportLayerProtocol = .TCP
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val type = lintResult.find<Type> { type -> type.toString() == "TransportLayerProtocol" }
		val instanceAccess = lintResult.find<InstanceAccess>()
		assertNotNull(type)
		assertEquals(type, instanceAccess?.type)
	}

	@Test
	fun `resolves instance accesses in assignments`() {
		val sourceCode =
			"""
				enum TransportLayerProtocol {
					instances TCP, UDP
				}
				var protocol: TransportLayerProtocol? = null
				protocol = .TCP
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val type = lintResult.find<Type> { type -> type.toString() == "TransportLayerProtocol" }
		val instanceAccess = lintResult.find<InstanceAccess>()
		assertNotNull(type)
		assertEquals(type, instanceAccess?.type)
	}

	@Test
	fun `resolves instance accesses in initializer calls`() {
		val sourceCode =
			"""
				enum TransportLayerProtocol {
					instances TCP, UDP
				}
				class Stream {
					val protocol: TransportLayerProtocol

					init(protocol)
				}
				val stream = Stream(.TCP)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val type = lintResult.find<Type> { type -> type.toString() == "TransportLayerProtocol" }
		val instanceAccess = lintResult.find<InstanceAccess>()
		assertNotNull(type)
		assertEquals(type, instanceAccess?.type)
	}

	@Test
	fun `resolves instance accesses in function calls`() {
		val sourceCode =
			"""
				enum TransportLayerProtocol {
					instances TCP, UDP
				}
				class Port {}
				object NetworkInterface {
					to getOpenPort(protocol: TransportLayerProtocol): Port {}
				}
				val openUdpPort = NetworkInterface.getOpenPort(.UDP)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val type = lintResult.find<Type> { type -> type.toString() == "TransportLayerProtocol" }
		val instanceAccess = lintResult.find<InstanceAccess>()
		assertNotNull(type)
		assertEquals(type, instanceAccess?.type)
	}

	@Test
	fun `resolves instance accesses in operator calls`() {
		val sourceCode =
			"""
				enum TransportLayerProtocol {
					instances TCP, UDP
				}
				class Ports {}
				object NetworkInterface {
					operator [protocol: TransportLayerProtocol](): Ports {}
				}
				val udpPorts = NetworkInterface[.UDP]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val type = lintResult.find<Type> { type -> type.toString() == "TransportLayerProtocol" }
		val instanceAccess = lintResult.find<InstanceAccess>()
		assertNotNull(type)
		assertEquals(type, instanceAccess?.type)
	}

	@Test
	fun `resolves instance accesses in switch cases`() {
		val sourceCode =
			"""
				enum TransportLayerProtocol {
					instances TCP, UDP
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
		val typeDefinition = lintResult.find<TypeDefinition> { typeDefinition ->
			typeDefinition.name == "TransportLayerProtocol" }
		val instanceAccess = lintResult.find<InstanceAccess>()
		assertNotNull(typeDefinition)
		assertEquals(typeDefinition, (instanceAccess?.type as? ObjectType)?.definition)
	}

	@Test
	fun `emits errors when generic type can't be inferred`() {
		val sourceCode =
			"""
				class Box {
					containing Item
					init
				}
				val letterBox = Box()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Missing generic parameter")
	}

	@Test
	fun `infers generic type before constructor call`() {
		val sourceCode =
			"""
				class Letter {
					init
				}
				class Box {
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
				class List {
					containing Item
					//val backup: <Item>List? = null // <--- This causes a ConcurrentModificationException
					init
					to add(item: Item) {}
				}
				class Message {
					var actions: <Message>Actions
				}
				class NewsletterMessage: Message {}
				class Actions {
					containing M: Message
					init
				}
				class Account {
					val incomingMessages = <Message>List()
					init
				}
				class MailFolder {
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
				trait Letter {}
				class PostCard: Letter {
					init
				}
				object PostOffice {
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
				trait Letter {}
				class PostCard: Letter {
					init
				}
				object PostOffice {
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
				trait IpAddress {}
				class Ipv4Address: IpAddress {
					init
				}
				class Ipv6Address: IpAddress {
					init
				}
				class Client {
					containing A: IpAddress
					init
				}
				object Server {
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
