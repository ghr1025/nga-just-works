package gov.anzong.androidnga

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * lifecycle-viewmodel >= 2.9 only creates ViewModels reflectively when they have a public no-arg
 * (or Application) constructor. Kotlin `object` ViewModels have a private constructor, so asking
 * a ViewModelProvider for one throws "Cannot create an instance of class ..." at runtime even
 * though it compiles. Such singletons must be referenced directly.
 */
class ViewModelConstructionContractTest {

    private val root = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) {
        it.parentFile
    }.first { File(it, "nga_phone_base_3.0").isDirectory }

    @Test
    fun objectViewModelsAreNeverRequestedFromAViewModelProvider() {
        val sources = root.listFiles().orEmpty()
            .map { File(it, "src/main/java") }
            .filter(File::isDirectory)
            .flatMap { dir -> dir.walkTopDown().filter { it.extension == "kt" || it.extension == "java" } }
            .toList()
        assertTrue(sources.isNotEmpty())

        val objectViewModels = sources.flatMap { file ->
            OBJECT_VIEW_MODEL.findAll(file.readText()).map { it.groupValues[1] }
        }.toSet()
        assertTrue("ForumBoardViewModel is an object ViewModel", "ForumBoardViewModel" in objectViewModels)

        val offenders = sources.flatMap { file ->
            val text = file.readText()
            objectViewModels.filter { name ->
                Regex("""\.get\(\s*$name::class\.java\s*\)|\.get\(\s*$name\.class\s*\)|by\s+viewModels<\s*$name\s*>""")
                    .containsMatchIn(text)
            }.map { "${file.relativeTo(root)} requests $it" }
        }
        assertTrue("Reference object ViewModels directly: $offenders", offenders.isEmpty())
    }

    private companion object {
        val OBJECT_VIEW_MODEL = Regex("""\bobject\s+(\w+)\s*:\s*(?:Android)?ViewModel\s*\(""")
    }
}
