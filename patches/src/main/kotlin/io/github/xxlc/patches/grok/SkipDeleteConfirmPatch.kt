package io.github.xxlc.patches.grok

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
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

@Suppress("unused")
val skipDeleteConfirmationPatch = bytecodePatch(
    name = "Skip delete confirmation",
    description = "Xoá ảnh/video Imagine ngay khi chọn xoá. Dữ liệu bị xoá vĩnh viễn trên máy chủ, không có hoàn tác. Mặc định tắt; chưa kiểm trên thiết bị.",
    default = false,
) {
    compatibleWith(grokCompatibility)

    val enabled by enabledOption(default = true)

    execute {
        if (enabled != true) return@execute

        ImagineDeleteConfirmFingerprint.method.apply {
            requireRegisterCount(14) // .locals 9; p1 = v10, p2 = v11, p3 = v12.
            val startIndex = indexOfReference(Opcode.INVOKE_VIRTUAL, GrokNames.COMPOSER_START)
            val executeIndex = indexOfReference(Opcode.INVOKE_VIRTUAL, GrokNames.COMPOSER_EXECUTE)
            val dialogIndex = indexOfReference(Opcode.INVOKE_STATIC_RANGE, GrokNames.DRAW_CONFIRM_DIALOG)
            val endIndex = indexOfReference(Opcode.INVOKE_VIRTUAL, GrokNames.COMPOSER_END)
            requireRegisters(startIndex, 12, 0)
            requireRegisters(executeIndex, 12, 4, 1)
            requireRegisters(dialogIndex, 0, 1, 2, 3, 4, 5, 6, 7)
            requireRegisters(endIndex, 12)
            check(startIndex < executeIndex && executeIndex < dialogIndex && dialogIndex < endIndex) {
                "Thứ tự nhóm Compose của hộp thoại xoá không khớp smali gốc."
            }
            check(implementation!!.instructions[executeIndex + 1].opcode == Opcode.MOVE_RESULT &&
                implementation!!.instructions[executeIndex + 2].opcode == Opcode.IF_EQZ) {
                "Cổng recomposition của hộp thoại xoá không khớp smali gốc."
            }
            requireRegisters(executeIndex + 1, 1)
            requireRegisters(executeIndex + 2, 1)
            // Lấy chính đích goto_6 sau lời gọi dialog, không nhảy qua bước đóng nhóm.
            val endBranch = implementation!!.instructions[dialogIndex + 1] as BuilderOffsetInstruction
            check(endBranch.opcode == Opcode.GOTO && endBranch.target.location.index == endIndex) {
                "Đích kết thúc nhóm Compose của hộp thoại xoá không hợp lệ."
            }
            // Chỉ chạy trong nhánh W() cho phép dựng UI. Không dùng thanh ghi tạm;
            // p0..p4 còn sống tại phần đăng ký callback recomposition ở cuối.
            addInstructionsWithLabels(
                executeIndex + 3,
                """
                    invoke-interface {p1}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;
                    invoke-interface {p2}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;
                    goto/16 :finish_delete_group
                """,
                ExternalLabel("finish_delete_group", endIndex),
            )
        }
    }
}
