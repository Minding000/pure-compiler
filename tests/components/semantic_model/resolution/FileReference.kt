package components.semantic_model.resolution

import components.semantic_model.context.SemanticModelGenerator
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.types.ObjectType
import components.syntax_parser.element_generator.SyntaxTreeGenerator
import org.junit.jupiter.api.Test
import source_structure.Module
import source_structure.Project
import util.ParseResult
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class FileReference {

	@Test
	fun `types from a referenced file are accessible`() {
		val typeProvider =
			"""
				BirdType enum
            """.trimIndent()
		val typeRequester =
			"""
				referencing ${TestUtil.TEST_MODULE_NAME}.TypeProvider
				var birdType: BirdType
            """.trimIndent()
		val project = Project(TestUtil.TEST_PROJECT_NAME)
		val testModule = Module(project, TestUtil.TEST_MODULE_NAME)
		testModule.addFile(emptyList(), "TypeRequester", typeRequester)
		testModule.addFile(emptyList(), "TypeProvider", typeProvider)
		project.addModule(testModule)
		val syntaxTreeGenerator = SyntaxTreeGenerator(project)
		val parseResult = ParseResult(syntaxTreeGenerator, syntaxTreeGenerator.parseProgram())
		val program = SemanticModelGenerator(project.context).createSemanticModel(parseResult.program)
		val typeRequesterFile = program.getFile(listOf(TestUtil.TEST_MODULE_NAME, "TypeRequester"))
		assertNotNull(typeRequesterFile)
		val declaration = typeRequesterFile.find<ValueDeclaration> { declaration -> declaration.name == "birdType" }
		assertNotNull(declaration)
		assertEquals("BirdType", (declaration.type as? ObjectType)?.getTypeDeclaration()?.name)
	}

	@Test
	fun `types from a non-referenced file are inaccessible`() {
		val typeProvider =
			"""
				BirdType enum
            """.trimIndent()
		val typeRequester =
			"""
				var birdType: BirdType
            """.trimIndent()
		val project = Project(TestUtil.TEST_PROJECT_NAME)
		val testModule = Module(project, TestUtil.TEST_MODULE_NAME)
		testModule.addFile(emptyList(), "TypeRequester", typeRequester)
		testModule.addFile(emptyList(), "TypeProvider", typeProvider)
		project.addModule(testModule)
		val syntaxTreeGenerator = SyntaxTreeGenerator(project)
		val parseResult = ParseResult(syntaxTreeGenerator, syntaxTreeGenerator.parseProgram())
		val program = SemanticModelGenerator(project.context).createSemanticModel(parseResult.program)
		val typeRequesterFile = program.getFile(listOf(TestUtil.TEST_MODULE_NAME, "TypeRequester"))
		assertNotNull(typeRequesterFile)
		val declaration = typeRequesterFile.find<ValueDeclaration> { declaration -> declaration.name == "birdType" }
		assertNotNull(declaration)
		assertNull((declaration.type as? ObjectType)?.getTypeDeclaration())
	}

	@Test
	fun `aliased types from a referenced file are accessible`() {
		val typeProvider =
			"""
				String class
            """.trimIndent()
		val typeRequester =
			"""
				referencing ${TestUtil.TEST_MODULE_NAME}.String {
					String as Text
				}
				var text: Text
            """.trimIndent()
		val project = Project(TestUtil.TEST_PROJECT_NAME)
		val testModule = Module(project, TestUtil.TEST_MODULE_NAME)
		testModule.addFile(emptyList(), "TypeRequester", typeRequester)
		testModule.addFile(emptyList(), "String", typeProvider)
		project.addModule(testModule)
		val syntaxTreeGenerator = SyntaxTreeGenerator(project)
		val parseResult = ParseResult(syntaxTreeGenerator, syntaxTreeGenerator.parseProgram())
		val program = SemanticModelGenerator(project.context).createSemanticModel(parseResult.program)
		val typeRequesterFile = program.getFile(listOf(TestUtil.TEST_MODULE_NAME, "TypeRequester"))
		assertNotNull(typeRequesterFile)
		val declaration = typeRequesterFile.find<ValueDeclaration> { declaration -> declaration.name == "text" }
		assertNotNull(declaration)
		assertEquals("String", (declaration.type as? ObjectType)?.getTypeDeclaration()?.name)
	}

	@Test
	fun `original types from a alias-referenced file are inaccessible`() {
		val typeProvider =
			"""
				String class
            """.trimIndent()
		val typeRequester =
			"""
				referencing ${TestUtil.TEST_MODULE_NAME}.String {
					String as Text
				}
				var string: String
            """.trimIndent()
		val project = Project(TestUtil.TEST_PROJECT_NAME)
		val testModule = Module(project, TestUtil.TEST_MODULE_NAME)
		testModule.addFile(emptyList(), "TypeRequester", typeRequester)
		testModule.addFile(emptyList(), "String", typeProvider)
		project.addModule(testModule)
		val syntaxTreeGenerator = SyntaxTreeGenerator(project)
		val parseResult = ParseResult(syntaxTreeGenerator, syntaxTreeGenerator.parseProgram())
		val program = SemanticModelGenerator(project.context).createSemanticModel(parseResult.program)
		val typeRequesterFile = program.getFile(listOf(TestUtil.TEST_MODULE_NAME, "TypeRequester"))
		assertNotNull(typeRequesterFile)
		val declaration = typeRequesterFile.find<ValueDeclaration> { declaration -> declaration.name == "string" }
		assertNotNull(declaration)
		assertNull((declaration.type as? ObjectType)?.getTypeDeclaration())
	}
}
