package io.github.xxlc.patches.grok

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.addInstructionsAtControlFlowLabel
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val STATE_CLASS = "Lio/github/xxlc/extension/grok/DownloadAllState;"
private const val LIBRARY_CONTEXT = "Lw8/h;->l:Landroid/content/Context;"

@Suppress("unused")
val libraryDownloadAllPatch = bytecodePatch(
    name = "Download all (library)",
    description = "Thêm nút tải tất cả media của các bài Imagine đã chọn, giữ nguyên nút Download.",
    default = true,
) {
    compatibleWith(grokCompatibility)
    dependsOn(downloadResourcesPatch)
    extendWith("extensions/extension.mpe")

    val enabled by enabledOption()

    execute {
        if (enabled != true) return@execute

        LibraryClickFingerprint.method.apply {
            requireRegisterCount(6) // .locals 5 và p0; tiền đề chỉ dùng v0, v1.
            val downloadIndex = indexOfReference(Opcode.SGET_OBJECT, GrokNames.LIBRARY_DOWNLOAD_SINGLETON)
            requireRegisters(downloadIndex, 0)
            // Nhánh switch gốc phải chạy qua thao tác tắt cờ khi bấm Download.
            addInstructionsAtControlFlowLabel(
                downloadIndex,
                """
                    const/4 v0, 0x0
                    sput-boolean v0, $STATE_CLASS->all:Z
                """,
            )
            // 0x7f nằm ngoài dải 0x0..0x1c; bắt riêng trước packed-switch gốc.
            // Đường đi thông thường khởi tạo lại v0, v1 trong phần mã gốc.
            addInstructionsWithLabels(
                0,
                """
                    iget-byte v0, p0, ${GrokNames.LIBRARY_CLICK_KIND}
                    const/16 v1, ${GrokNames.LIBRARY_DOWNLOAD_ALL_DISCRIMINATOR}
                    if-ne v0, v1, :original_click
                    iget-object v1, p0, ${GrokNames.LIBRARY_CLICK_ACTION}
                    const/4 v0, 0x1
                    sput-boolean v0, $STATE_CLASS->all:Z
                    sget-object v0, ${GrokNames.LIBRARY_DOWNLOAD_SINGLETON}
                    invoke-interface {v1, v0}, Lkotlin/jvm/functions/Function1;->invoke(Ljava/lang/Object;)Ljava/lang/Object;
                    sget-object v0, Lkotlin/Unit;->a:Lkotlin/Unit;
                    return-object v0
                    :original_click
                    nop
                """,
            )
        }

        LibraryButtonsFingerprint.method.apply {
            requireRegisterCount(32)
            val instructions = implementation!!.instructions
            val saveIndex = LibraryButtonsFingerprint.instructionMatches.first().index
            requireRegisters(saveIndex, 0)
            // Có bốn lời gọi cùng chữ ký; lấy lời gọi đầu tiên sau tài nguyên save.
            val buttonIndex = instructions.withIndex().first { (index, instruction) ->
                index > saveIndex && instruction.opcode == Opcode.INVOKE_STATIC_RANGE &&
                    (instruction as? ReferenceInstruction)?.reference?.toString() ==
                    GrokNames.DRAW_LIBRARY_BUTTON
            }.index
            // Khóa đúng callback Download 0x0a giữa save và lời gọi vẽ nút.
            val callbackIndex = instructions.withIndex().single { (index, instruction) ->
                index in (saveIndex + 1) until buttonIndex && instruction.opcode == Opcode.INVOKE_DIRECT &&
                    (instruction as? ReferenceInstruction)?.reference?.toString() ==
                    GrokNames.NEW_LIBRARY_CLICK
            }.index
            requireRegisters(callbackIndex, 2, 9, 0)
            requireRegisters(callbackIndex - 1, 0)
            check(instructions[callbackIndex - 1].opcode == Opcode.CONST_16 &&
                (instructions[callbackIndex - 1] as? NarrowLiteralInstruction)?.narrowLiteral == GrokNames.LIBRARY_DOWNLOAD_DISCRIMINATOR) {
                "Callback của nút Download không khớp smali gốc."
            }
            requireRegisters(buttonIndex, 14, 15, 16, 17, 18, 19, 20)
            // Chèn sau lời gọi vẽ Download gốc, giữ cùng bố cục và trạng thái bật/tắt.
            addInstructionsWithLabels(
                buttonIndex + 1,
                """
                    sget-object v18, ${GrokNames.DOWNLOAD_ICON}
                    const v0, $downloadAllStringId
                    invoke-static {v0, v4}, ${GrokNames.GET_STRING}
                    move-result-object v15
                    invoke-static {v3, v1}, ${GrokNames.BUTTON_MODIFIER}
                    move-result-object v19
                    invoke-virtual {v4, v9}, ${GrokNames.COMPOSER_CHANGED}
                    move-result v0
                    invoke-virtual {v4}, ${GrokNames.COMPOSER_REMEMBERED}
                    move-result-object v2
                    if-nez v0, :sd_all_a
                    if-ne v2, v5, :sd_all_b
                    :sd_all_a
                    new-instance v2, ${GrokNames.LIBRARY_CLICK}
                    const/16 v0, ${GrokNames.LIBRARY_DOWNLOAD_ALL_DISCRIMINATOR}
                    invoke-direct {v2, v9, v0}, ${GrokNames.NEW_LIBRARY_CLICK}
                    invoke-virtual {v4, v2}, ${GrokNames.COMPOSER_UPDATE}
                    :sd_all_b
                    move-object/from16 v17, v2
                    check-cast v17, Lkotlin/jvm/functions/Function0;
                    move-object/from16 v16, v4
                    invoke-static/range {v14 .. v20}, ${GrokNames.DRAW_LIBRARY_BUTTON}
                """,
            )
        }

        LibrarySelectionFingerprint.method.apply {
            requireRegisterCount(44) // .locals 42, giữ nguyên số thanh ghi.
            val postIndex = indexOfReference(Opcode.IGET_OBJECT, GrokNames.SELECTED_POST_VALUE)
            val singleIndex = indexOfReference(Opcode.NEW_INSTANCE, GrokNames.LIBRARY_DOWNLOAD_REQUEST)
            val resetIndex = indexOfReference(Opcode.IGET_OBJECT, GrokNames.LIBRARY_DOWNLOAD_PROCESSOR)
            requireRegisters(postIndex, 7, 7)
            requireRegisters(singleIndex, 7)
            requireRegisters(resetIndex, 6, 1)
            // v9 được ghi lại ngay sau processor; dùng tạm để truyền Context.
            check(implementation!!.instructions[resetIndex + 1].opcode == Opcode.IGET_OBJECT) {
                "Lệnh ghi lại v9 sau processor không khớp smali gốc."
            }
            requireRegisters(resetIndex + 1, 9, 1)

            // Lấy đích :cond_2f từ if-eqz v7 ngay sau constructor Lo7/b1.
            val nextPostBranch = implementation!!.instructions[singleIndex + 2] as BuilderOffsetInstruction
            check(nextPostBranch.opcode == Opcode.IF_EQZ) {
                "Nhánh chuyển sang bài tiếp theo không khớp smali gốc."
            }
            requireRegisters(singleIndex + 2, 7)
            val nextPostIndex = nextPostBranch.target.location.index

            // Chèn từ cuối về đầu để các chỉ số chưa dùng không bị dịch chuyển.
            // :goto_e là đích nhảy nên phải giữ nhãn trên phần xoá cờ.
            addInstructionsAtControlFlowLabel(
                resetIndex,
                """
                    const/4 v6, 0x0
                    sput-boolean v6, $STATE_CLASS->all:Z
                """,
            )
            // Chèn trước khối xoá cờ vừa thêm: v5 là danh sách request đã gom,
            // v6/v9 được ghi lại ngay sau đó; v1 giữ handler có Context công khai.
            // Không đụng vòng coroutine hoặc tăng số thanh ghi.
            addInstructionsAtControlFlowLabel(
                resetIndex,
                """
                    sget-object v6, ${GrokNames.LIBRARY_DOWNLOAD_SINGLETON}
                    if-ne v0, v6, :d2_library_notify_done
                    sget-boolean v6, $STATE_CLASS->all:Z
                    if-eqz v6, :d2_library_notify_done
                    invoke-virtual {v5}, Ljava/util/ArrayList;->size()I
                    move-result v6
                    if-lez v6, :d2_library_notify_done
                    iget-object v9, v1, $LIBRARY_CONTEXT
                    invoke-static {v9, v6}, Lio/github/xxlc/extension/grok/Notify;->queued(Landroid/content/Context;I)V
                    :d2_library_notify_done
                    nop
                """,
            )
            // :cond_35 cũng là đích nhảy; vòng lặp phải chạy trước nhánh một media.
            addInstructionsAtControlFlowLabel(
                singleIndex,
                """
                    check-cast v0, ${GrokNames.LIBRARY_SELECTION_EVENT}
                    iget-object v13, v0, ${GrokNames.SELECTION_ACTION}
                    sget-object v12, ${GrokNames.LIBRARY_DOWNLOAD_SINGLETON}
                    if-ne v13, v12, :single_b1
                    sget-boolean v13, $STATE_CLASS->all:Z
                    if-eqz v13, :single_b1
                    invoke-virtual/range {v32 .. v32}, ${GrokNames.POST_MEDIA}
                    move-result-object v13
                    invoke-interface {v13}, Ljava/util/List;->iterator()Ljava/util/Iterator;
                    move-result-object v13
                    :all_loop
                    invoke-interface {v13}, Ljava/util/Iterator;->hasNext()Z
                    move-result v12
                    if-eqz v12, :next_post
                    invoke-interface {v13}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                    move-result-object v12
                    check-cast v12, ${GrokNames.MEDIA}
                    new-instance v15, ${GrokNames.LIBRARY_DOWNLOAD_REQUEST}
                    invoke-direct {v15, v6, v12}, ${GrokNames.NEW_LIBRARY_DOWNLOAD_REQUEST}
                    invoke-virtual {v5, v15}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                    goto :all_loop
                    :single_b1
                    nop
                """,
                ExternalLabel("next_post", nextPostIndex),
            )
            addInstruction(postIndex + 1, "move-object/from16 v32, v7")
        }
    }
}
