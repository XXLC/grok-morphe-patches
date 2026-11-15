package io.github.xxlc.patches.grok

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val shareFilePatch = bytecodePatch(
    name = "Share file",
    description = "Thêm lối tắt tới luồng chia sẻ sẵn có: mở bảng chọn đích rồi chọn chia sẻ tệp, gồm hai bước. Giữ điều kiện cho phép của mục Share gốc. Chưa kiểm trên thiết bị.",
    default = true,
) {
    compatibleWith(grokCompatibility)
    dependsOn(downloadResourcesPatch, postActionFlagsPatch)
    val enabled by enabledOption(default = true)

    execute {
        if (enabled != true) return@execute
        postMenuForExtraAction().method.appendExtraPostAction(GrokNames.SHARE_FILE_FLAG, "share", false)
        postLabelForExtraAction().method.labelExtraPostAction(
            GrokNames.SHARE_FILE_FLAG, "share", stringId("grok_xxlc_share_file"),
        )
        postHandlerForExtraAction().method.apply {
            requireRegisterCount(22)
            val mediaIndex = indexOfReference(Opcode.INVOKE_VIRTUAL, GrokNames.FIND_CURRENT_MEDIA)
            requireRegisters(mediaIndex, 1, 2)
            // v5 chưa sống; chỉ thay hành động trong v0, giữ media và continuation.
            addInstructionsWithLabels(
                mediaIndex,
                """
                    instance-of v5, v0, ${GrokNames.DOWNLOAD_ACTION}
                    if-eqz v5, :d2_share_handler_original
                    move-object v5, v0
                    check-cast v5, ${GrokNames.DOWNLOAD_ACTION}
                    iget-boolean v5, v5, ${GrokNames.SHARE_FILE_FLAG}
                    if-eqz v5, :d2_share_handler_original
                    sget-object v0, ${GrokNames.SHARE_SINGLETON}
                    :d2_share_handler_original
                    nop
                """,
            )
        }
    }
}
