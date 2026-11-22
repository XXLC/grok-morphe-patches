package io.github.xxlc.patches.grok

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private object ImagineTabHiddenFingerprint : Fingerprint(
    definingClass = GrokNames.IMAGINE_TAB_VISIBILITY,
    name = GrokNames.IMAGINE_TAB_HIDDEN_METHOD,
    isStatic = false,
    returnType = "Ljava/lang/Object;",
    parameters = listOf(GrokNames.SUSPEND_CONTINUATION),
)

@Suppress("unused")
val showImagineTabPatch = bytecodePatch(
    name = "Show Imagine tab",
    description = "Always show the Imagine tab on the home screen, even when the home layout v2 flag hides it for new accounts or accounts that have not used Imagine recently. Does not change the Team/Org Imagine lock. Not tested on a device.",
    default = true,
) {
    compatibleWith(grokCompatibility)
    val enabled by enabledOption(default = true)

    execute {
        if (enabled != true) return@execute
        ImagineTabHiddenFingerprint.method.apply {
            // .locals 13, p0 = v13 (this), p1 = v14 (continuation); chỉ dùng p0 làm thanh ghi tạm.
            requireRegisterCount(15)
            // Kết quả Boolean.TRUE nghĩa là ẩn tab; trả FALSE ngay, không treo coroutine,
            // nên không đọc cờ cấu hình, DataStore hay gọi mạng khi tính trạng thái.
            addInstructions(
                0,
                """
                    sget-object p0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                    return-object p0
                """,
            )
        }
    }
}
