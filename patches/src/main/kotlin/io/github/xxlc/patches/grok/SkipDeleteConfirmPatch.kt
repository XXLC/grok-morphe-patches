package io.github.xxlc.patches.grok

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction

private object ImagineDeleteConfirmFingerprint : Fingerprint(
    definingClass = GrokNames.IMAGINE_UI,
    name = GrokNames.DELETE_CONFIRM_METHOD,
    isStatic = true,
    returnType = "V",
    parameters = listOf(
        GrokNames.DELETE_MEDIA_KIND, "Lkotlin/jvm/functions/Function0;",
        "Lkotlin/jvm/functions/Function0;", GrokNames.COMPOSER, "I",
    ),
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "grok_image_delete_post_confirm_action"),
        methodCall(definingClass = GrokNames.CONFIRM_DIALOG, name = "b", returnType = "V"),
    ),
)

private object LibraryBulkDeleteConfirmFingerprint : Fingerprint(
    definingClass = GrokNames.LIBRARY_BUTTON_RENDERER,
    name = GrokNames.BULK_DELETE_CONFIRM_METHOD,
    isStatic = true,
    returnType = "V",
    parameters = listOf(
        "I", "Lkotlin/jvm/functions/Function0;",
        "Lkotlin/jvm/functions/Function0;", GrokNames.COMPOSER, "I",
    ),
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "grok_imagine_bulk_delete_dialog_title"),
        resourceLiteral(ResourceType.STRING, "grok_imagine_bulk_delete_dialog_description"),
        methodCall(definingClass = GrokNames.CONFIRM_DIALOG, name = "b", returnType = "V"),
    ),
)

private fun MutableMethod.skipConfirmDialogWithSideEffect(
    expectedRegisterCount: Int,
    composerReg: Int,
    executeFlagReg: Int,
    labelName: String,
) {
    requireRegisterCount(expectedRegisterCount)
    val startIndex = indexOfReference(Opcode.INVOKE_VIRTUAL, GrokNames.COMPOSER_START)
    val executeIndex = indexOfReference(Opcode.INVOKE_VIRTUAL, GrokNames.COMPOSER_EXECUTE)
    val dialogIndex = indexOfReference(Opcode.INVOKE_STATIC_RANGE, GrokNames.DRAW_CONFIRM_DIALOG)
    val endIndex = indexOfReference(Opcode.INVOKE_VIRTUAL, GrokNames.COMPOSER_END)
    requireRegisters(startIndex, composerReg, 0)
    requireRegisters(executeIndex, composerReg, executeFlagReg, 1)
    requireRegisters(dialogIndex, 0, 1, 2, 3, 4, 5, 6, 7)
    requireRegisters(endIndex, composerReg)
    check(startIndex < executeIndex && executeIndex < dialogIndex && dialogIndex < endIndex) {
        "Thứ tự nhóm Compose của hộp thoại xoá không khớp smali gốc: $this"
    }
    check(
        implementation!!.instructions[executeIndex + 1].opcode == Opcode.MOVE_RESULT &&
            implementation!!.instructions[executeIndex + 2].opcode == Opcode.IF_EQZ,
    ) {
        "Cổng recomposition của hộp thoại xoá không khớp smali gốc: $this"
    }
    requireRegisters(executeIndex + 1, 1)
    requireRegisters(executeIndex + 2, 1)
    // Lấy chính đích goto sau lời gọi dialog, không nhảy qua bước đóng nhóm.
    val endBranch = implementation!!.instructions[dialogIndex + 1] as BuilderOffsetInstruction
    check(endBranch.opcode == Opcode.GOTO && endBranch.target.location.index == endIndex) {
        "Đích kết thúc nhóm Compose của hộp thoại xoá không hợp lệ: $this"
    }
    // Chạy sau khi composition commit qua SideEffect để tránh backwards-write;
    // v0..v1 đã chết tại điểm chèn và p0..p4 còn sống cho phần đăng ký recomposition ở cuối.
    addInstructionsWithLabels(
        executeIndex + 3,
        """
            new-instance v0, ${GrokNames.COMBINED_CALLBACK}
            const/4 v1, ${GrokNames.DELETE_CONFIRM_EFFECT_DISCRIMINATOR}
            invoke-direct {v0, p1, p2, v1}, ${GrokNames.NEW_COMBINED_CALLBACK}
            invoke-static {p3, v0}, ${GrokNames.COMPOSE_SIDE_EFFECT}
            goto/16 :$labelName
        """,
        ExternalLabel(labelName, endIndex),
    )
}

@Suppress("unused")
val skipDeleteConfirmationPatch = bytecodePatch(
    name = "Skip delete confirmation",
    description = "Xoá ảnh/video Imagine và xoá hàng loạt trong thư viện ngay khi chọn xoá (không áp dụng cho xoá hội thoại). Dữ liệu bị xoá vĩnh viễn trên máy chủ, không có hoàn tác. Mặc định tắt; chưa kiểm trên thiết bị.",
    default = false,
) {
    compatibleWith(grokCompatibility)

    val enabled by enabledOption(default = true)

    execute {
        if (enabled != true) return@execute

        ImagineDeleteConfirmFingerprint.method.skipConfirmDialogWithSideEffect(
            expectedRegisterCount = 14, // .locals 9; p1 = v10, p2 = v11, p3 = v12.
            composerReg = 12,
            executeFlagReg = 4,
            labelName = "finish_delete_group",
        )
        LibraryBulkDeleteConfirmFingerprint.method.skipConfirmDialogWithSideEffect(
            expectedRegisterCount = 16, // .locals 11; p1 = v12, p2 = v13, p3 = v14.
            composerReg = 14,
            executeFlagReg = 2,
            labelName = "finish_bulk_delete_group",
        )
    }
}
