package linting

import linting.semantic_model.access.InstanceAccess
import linting.semantic_model.definitions.TypeDefinition
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.literals.Type
import linting.semantic_model.values.VariableValueDeclaration
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
		val lintResult = TestUtil.lint(sourceCode, false)
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
		val lintResult = TestUtil.lint(sourceCode, false)
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
		val lintResult = TestUtil.lint(sourceCode, false)
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
		val lintResult = TestUtil.lint(sourceCode, false)
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
		val lintResult = TestUtil.lint(sourceCode, false)
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
		val lintResult = TestUtil.lint(sourceCode, false)
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
		val lintResult = TestUtil.lint(sourceCode, false)
		val typeDefinition = lintResult.find<TypeDefinition> { typeDefinition -> typeDefinition.name == "TransportLayerProtocol" }
		val instanceAccess = lintResult.find<InstanceAccess>()
		assertNotNull(typeDefinition)
		assertEquals(typeDefinition, (instanceAccess?.type as? ObjectType)?.definition)
	}
}
