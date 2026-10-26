package gov.anzong.androidnga

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Modules without the Compose compiler plugin (lib_base_ui, Java sources) see `@Composable`
 * function types as plain Function0/Function1, while lib_base_ui_compose exports the
 * transformed Function2(Composer, Int). Calls across that boundary compile but crash at runtime
 * with NoSuchMethodError, as `miuixViewShell(..., floatingAction)` did in FragmentTemplateActivity.
 *
 * The View-facing Miuix entry points therefore must not take `@Composable` parameters.
 */
class MiuixApiBoundaryContractTest {

    private val root = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) {
        it.parentFile
    }.first { File(it, "nga_phone_base_3.0").isDirectory }

    private val viewFacing = listOf(
        "lib_base_ui_compose/src/main/java/com/justwen/androidnga/ui/compose/miuix/MiuixViewShell.kt" to "fun miuixViewShell(",
    )

    @Test
    fun viewFacingMiuixEntryPointsTakeNoComposableParameters() {
        viewFacing.forEach { (path, signature) ->
            val source = File(root, path).readText()
            val start = source.indexOf(signature)
            assertTrue("$signature not found in $path", start >= 0)
            val parameters = source.substring(start, source.indexOf("): View", start))
            assertTrue(
                "$signature must stay callable from non-Compose modules: $parameters",
                !parameters.contains("@Composable"),
            )
        }
    }

    @Test
    fun viewShellAttachesTheClassicTreeBeforeComposition() {
        // ProfileActivity binds with ButterKnife right after setContentView; with the classic
        // root passed as content it was only attached on first composition and the bind crashed.
        val source = File(root, viewFacing.first().first).readText()
        val body = source.substring(source.indexOf("return FrameLayout(activity).apply {"))
            .substringBefore("\n    }\n")
        assertTrue("classic root must be attached unconditionally", body.contains("addView(classicRoot"))
        assertTrue("classic root must not be attached conditionally", !body.contains("if (classicRoot"))
    }

    @Test
    fun nonComposeModulesDoNotEnableTheComposeCompilerSilently() {
        // lib_base_ui calls miuixViewShell; if it ever gains the Compose plugin the boundary
        // above becomes irrelevant, so keep the assumption explicit.
        val gradle = File(root, "lib_base_ui/build.gradle").readText()
        assertTrue(!gradle.contains("compose.compiler"))
    }
}
