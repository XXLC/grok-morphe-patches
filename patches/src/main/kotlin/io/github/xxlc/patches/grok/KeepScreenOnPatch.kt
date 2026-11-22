package io.github.xxlc.patches.grok

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val keepScreenOnPatch = bytecodePatch(
    name = "Keep screen on",
    description = "Keep the screen on for as long as Grok is in the foreground, including while reading chat. The flag is cleared when the activity pauses. Uses more battery, so it is off by default; not tested on a device.",
    default = false,
) {
    compatibleWith(grokCompatibility)
    val enabled by enabledOption(default = true)

    execute {
        if (enabled != true) return@execute
        listOf("onResume" to "addFlags", "onPause" to "clearFlags").forEach { (lifecycle, flags) ->
            Fingerprint(
                definingClass = GrokNames.MAIN_ACTIVITY,
                name = lifecycle,
                isStatic = false,
                returnType = "V",
                parameters = emptyList(),
            ).method.apply {
                requireRegisterCount(4) // .locals 3; p0 = v3, v0/v1 chưa sống.
                val superIndex = indexOfReference(Opcode.INVOKE_SUPER, "${GrokNames.ACTIVITY_SUPER}->$lifecycle()V")
                check(superIndex == 0) { "Lời gọi vòng đời cha không ở đầu phương thức." }
                requireRegisters(superIndex, 3)
                // Đứng trước try_start_0; không chuyển nhãn try/catch sang mã mới.
                addInstructions(
                    superIndex + 1,
                    """
                        invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
                        move-result-object v0
                        const/16 v1, 0x80
                        invoke-virtual {v0, v1}, Landroid/view/Window;->$flags(I)V
                    """,
                )
            }
        }
    }
}
