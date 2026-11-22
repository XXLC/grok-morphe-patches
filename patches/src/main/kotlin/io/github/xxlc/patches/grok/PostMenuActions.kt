package io.github.xxlc.patches.grok

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode

// Chỉ tra .method trong execute có BytecodePatchContext; các điểm chèn tìm trên mã mutable.
internal fun postMenuForExtraAction(): Fingerprint = Fingerprint(
    definingClass = GrokNames.POST_MENU,
    name = GrokNames.POST_MENU_METHOD,
    isStatic = true,
    returnType = "Ljava/util/List;",
    parameters = listOf(GrokNames.POST_STATE),
)

internal fun postLabelForExtraAction(): Fingerprint = Fingerprint(
    definingClass = GrokNames.POST_MENU_LABEL,
    name = GrokNames.POST_MENU_LABEL_METHOD,
    isStatic = true,
    returnType = GrokNames.MENU_LABEL,
    parameters = listOf(GrokNames.MENU_ACTION),
)

internal fun postHandlerForExtraAction(): Fingerprint = Fingerprint(
    definingClass = GrokNames.POST_HANDLER,
    name = GrokNames.POST_ACTION_METHOD,
    isStatic = false,
    returnType = "Ljava/lang/Object;",
    parameters = listOf(GrokNames.MENU_ACTION, GrokNames.MEDIA_ID, GrokNames.POST_CONTINUATION),
)

internal fun MutableMethod.appendCopyPromptAction() {
    requireRegisterCount(27)
    val returnIndex = implementation!!.instructions.lastIndex
    check(implementation!!.instructions[returnIndex].opcode == Opcode.RETURN_OBJECT) {
        "Điểm kết thúc danh sách menu không khớp smali gốc."
    }
    requireRegisters(returnIndex, 0)
    // v0 là danh sách; giữ p0=v26 và v10/v24 của đường gốc.
    // v1..v6 đã chết ở return cuối; khối tự tạo bản sao danh sách.
    // Chỉ thêm khi media hiện tại đã tạo xong và không bị kiểm duyệt (Ll70/f0;->D != null và
    // Le8/o;->s() == false): lỗi/đang tạo thì D trả null; media bị kiểm duyệt vẫn là Le8/o; nhưng
    // s() trả true, khi đó app tự hiển thị Retry nên không được chen mục Copy prompt.
    addInstructionsWithLabels(
        returnIndex,
        """
            move-object/from16 v5, p0
            invoke-static {v5}, ${GrokNames.COMPLETED_MEDIA}
            move-result-object v5
            if-eqz v5, :d2_copy_menu_done
            invoke-interface {v5}, ${GrokNames.MEDIA_MODERATED}
            move-result v5
            if-nez v5, :d2_copy_menu_done
            move-object/from16 v5, p0
            iget-object v5, v5, ${GrokNames.CURRENT_PROMPT}
            invoke-static {v5}, Lio/github/xxlc/extension/grok/ClipboardHelper;->canCopy(Ljava/lang/String;)Z
            move-result v5
            if-eqz v5, :d2_copy_menu_done
            new-instance v1, Ljava/util/ArrayList;
            invoke-direct {v1, v0}, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V
            new-instance v2, ${GrokNames.DOWNLOAD_ACTION}
            sget-object v3, ${GrokNames.DEFAULT_DOWNLOAD_STATE}
            invoke-direct {v2, v3}, ${GrokNames.NEW_DOWNLOAD_ACTION}
            const/4 v3, 0x1
            iput-boolean v3, v2, ${GrokNames.COPY_PROMPT_FLAG}
            const/4 v6, 0x1
            new-instance v4, ${GrokNames.MENU_ITEM}
            invoke-direct {v4, v2, v6}, ${GrokNames.NEW_MENU_ITEM}
            invoke-virtual {v1, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
            move-object v0, v1
            :d2_copy_menu_done
            nop
        """,
    )
}

internal fun MutableMethod.labelCopyPromptAction(stringId: Int) {
    requireRegisterCount(10)
    // Ở đầu chỉ p0=v9 sống. Đường gốc khởi tạo lại v0 trước khi đọc.
    addInstructionsWithLabels(
        0,
        """
            instance-of v0, p0, ${GrokNames.DOWNLOAD_ACTION}
            if-eqz v0, :d2_copy_label_original
            move-object v0, p0
            check-cast v0, ${GrokNames.DOWNLOAD_ACTION}
            iget-boolean v0, v0, ${GrokNames.COPY_PROMPT_FLAG}
            if-eqz v0, :d2_copy_label_original
            new-instance v3, ${GrokNames.MENU_LABEL}
            const/4 v7, 0x0
            sget-object v8, ${GrokNames.COPY_ICON}
            const/16 v6, 0xc
            const v4, $stringId
            const/4 v5, 0x0
            invoke-direct/range {v3 .. v8}, ${GrokNames.NEW_MENU_LABEL}
            return-object v3
            :d2_copy_label_original
            nop
        """,
    )
}
