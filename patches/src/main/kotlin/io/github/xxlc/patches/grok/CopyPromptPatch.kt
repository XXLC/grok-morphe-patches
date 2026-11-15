package io.github.xxlc.patches.grok

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val copyPromptPatch = bytecodePatch(
    name = "Copy prompt",
    description = "Thêm mục sao chép prompt media hiện tại vào clipboard; ẩn khi prompt rỗng. Model 1.2.56 không có seed nên không sao chép seed. Chưa kiểm trên thiết bị.",
    default = true,
) {
    compatibleWith(grokCompatibility)
    dependsOn(downloadResourcesPatch, postActionFlagsPatch)
    extendWith("extensions/extension.mpe")
    val enabled by enabledOption(default = true)

    execute {
        if (enabled != true) return@execute
        postMenuForExtraAction().method.appendExtraPostAction(GrokNames.COPY_PROMPT_FLAG, "copy", true)
        postLabelForExtraAction().method.labelExtraPostAction(
            GrokNames.COPY_PROMPT_FLAG, "copy", stringId("grok_xxlc_copy_prompt"),
        )
        postHandlerForExtraAction().method.apply {
            requireRegisterCount(22)
            val mediaIndex = indexOfReference(Opcode.INVOKE_VIRTUAL, GrokNames.FIND_CURRENT_MEDIA)
            requireRegisters(mediaIndex, 1, 2)
            // Sau ba move-object đầu: v0/v1/v2 sống, v5/v6 chưa sống.
            // Cả nhánh không phải Copy và đoạn gốc đều giữ p0..p3.
            addInstructionsWithLabels(
                mediaIndex,
                """
                    instance-of v5, v0, ${GrokNames.DOWNLOAD_ACTION}
                    if-eqz v5, :d2_copy_handler_original
                    move-object v5, v0
                    check-cast v5, ${GrokNames.DOWNLOAD_ACTION}
                    iget-boolean v5, v5, ${GrokNames.COPY_PROMPT_FLAG}
                    if-eqz v5, :d2_copy_handler_original
                    invoke-virtual {v1}, ${GrokNames.CURRENT_POST_STATE}
                    move-result-object v5
                    if-eqz v5, :d2_copy_handler_original
                    iget-object v6, v5, ${GrokNames.CURRENT_PROMPT}
                    iget-object v5, v1, ${GrokNames.POST_CONTEXT}
                    invoke-static {v5, v6}, Lio/github/xxlc/extension/grok/ClipboardHelper;->copyPrompt(Landroid/content/Context;Ljava/lang/String;)V
                    sget-object v0, Lkotlin/Unit;->a:Lkotlin/Unit;
                    return-object v0
                    :d2_copy_handler_original
                    nop
                """,
            )
        }
    }
}
