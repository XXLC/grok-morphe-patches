package io.github.xxlc.patches.grok

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val NAME_HELPER = "Lio/github/xxlc/extension/grok/DownloadNameHelper;"
private const val GUESS_FILE_NAME = "Landroid/webkit/URLUtil;->guessFileName(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"

private fun MutableMethod.replaceNameString(original: String, replacement: String, register: Int) {
    val index = implementation!!.instructions.withIndex().single { (_, instruction) ->
        instruction.opcode == Opcode.CONST_STRING &&
            ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string == original
    }.index
    requireRegisters(index, register)
    replaceInstruction(index, "const-string v$register, \"$replacement\"")
}

private fun mediaFileMethod(name: String, parameters: List<String>, isStatic: Boolean, returnType: String) =
    Fingerprint(
        definingClass = GrokNames.MEDIA_FILES,
        name = name,
        isStatic = isStatic,
        returnType = returnType,
        parameters = parameters,
    )

@Suppress("unused")
val downloadFileNamesPatch = bytecodePatch(
    name = "Download file names",
    description = "Đặt tên tải ảnh/video theo Grok_yyyyMMdd_HHmmss, thêm mili giây và số thứ tự để tránh trùng trong cùng tiến trình. Giữ phần mở rộng và cơ chế chống trùng sẵn có. Chưa kiểm trên thiết bị.",
    default = true,
) {
    compatibleWith(grokCompatibility)
    extendWith("extensions/extension.mpe")
    val enabled by enabledOption(default = true)

    execute {
        if (enabled != true) return@execute
        Fingerprint(
            definingClass = GrokNames.VIDEO_DATE_FORMAT,
            name = "<clinit>",
            isStatic = true,
            returnType = "V",
            parameters = emptyList(),
        ).method.apply {
            requireRegisterCount(1)
            replaceNameString("yyyy-MM-dd-HH-mm-ss", "yyyyMMdd_HHmmss_SSS", 0)
        }
        Fingerprint(
            definingClass = GrokNames.VIDEO_NAMES,
            name = GrokNames.VIDEO_NAME_METHOD,
            isStatic = false,
            returnType = "Ljava/lang/String;",
            parameters = emptyList(),
        ).method.apply {
            requireRegisterCount(3)
            replaceNameString("grok_video_", "Grok_", 0)
            val formatIndex = indexOfReference(
                Opcode.INVOKE_VIRTUAL,
                "Ljava/time/LocalDateTime;->format(Ljava/time/format/DateTimeFormatter;)Ljava/lang/String;",
            )
            requireRegisters(formatIndex, 2, 0)
            requireRegisters(formatIndex + 1, 2)
            addInstructions(
                formatIndex + 2,
                """
                    invoke-static {p0}, $NAME_HELPER->appendSequence(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object p0
                """,
            )
        }
        listOf(
            Triple(GrokNames.MEDIA_NAME_METHOD, listOf("Ljava/lang/String;", "Landroid/net/Uri;", "Ljava/lang/String;", "Landroid/content/Context;"), 16),
            Triple(GrokNames.LEGACY_MEDIA_NAME_METHOD, listOf("Ljava/io/File;", "Ljava/lang/String;", "Ljava/lang/String;"), 9),
        ).forEach { (name, parameters, registers) ->
            mediaFileMethod(name, parameters, true, "Ljava/lang/String;").method.apply {
                requireRegisterCount(registers)
                val guessIndex = indexOfReference(Opcode.INVOKE_STATIC, GUESS_FILE_NAME)
                val fileRegister = if (name == GrokNames.MEDIA_NAME_METHOD) "p0" else "p1"
                requireRegisters(guessIndex, if (registers == 16) 12 else 7, 0, if (registers == 16) 14 else 8)
                requireRegisters(guessIndex + 1, if (registers == 16) 12 else 7)
                // Ghi đè đúng kết quả guessFileName, giữ URI, Context, MIME và vòng dedupe.
                addInstructions(
                    guessIndex + 2,
                    """
                        invoke-static {$fileRegister, p2}, $NAME_HELPER->renameMedia(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object $fileRegister
                    """,
                )
            }
        }
    }
}

private fun MutableMethod.replaceFolder(field: String, folder: String, register: Int, expected: Int = 1) {
    val indices = implementation!!.instructions.withIndex().filter { (_, instruction) ->
        instruction.opcode == Opcode.SGET_OBJECT &&
            (instruction as? ReferenceInstruction)?.reference?.toString() ==
            "Landroid/os/Environment;->$field:Ljava/lang/String;"
    }.map { it.index }
    check(indices.size == expected) { "Số điểm chọn thư mục $field không khớp: ${indices.size}" }
    indices.forEach { index ->
        requireRegisters(index, register)
        replaceInstruction(index, "const-string v$register, \"$folder/Grok\"")
    }
}

@Suppress("unused")
val downloadFoldersPatch = bytecodePatch(
    name = "Download folders",
    description = "Lưu vào Download/Grok, Pictures/Grok hoặc Movies/Grok theo luồng gốc. Trước Android 10, các nhánh cũ tiếp tục dùng Download/Grok. Bao gồm tệp thường qua bộ lưu dùng chung. Chưa kiểm trên thiết bị.",
    default = true,
) {
    compatibleWith(grokCompatibility)
    val enabled by enabledOption(default = true)

    execute {
        if (enabled != true) return@execute
        Fingerprint(
            definingClass = GrokNames.PUBLIC_VIDEO_FILES,
            name = GrokNames.PUBLIC_VIDEO_SAVE_METHOD,
            isStatic = true,
            returnType = "Ljava/lang/Object;",
            parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/io/File;"),
        ).method.apply {
            requireRegisterCount(6)
            replaceFolder("DIRECTORY_DOWNLOADS", "Download", 0)
        }
        Fingerprint(
            definingClass = GrokNames.MEDIA_SAVE_TASK,
            name = "invokeSuspend",
            isStatic = false,
            returnType = "Ljava/lang/Object;",
            parameters = listOf("Ljava/lang/Object;"),
        ).method.apply {
            requireRegisterCount(15)
            replaceFolder("DIRECTORY_MOVIES", "Movies", 14)
            replaceFolder("DIRECTORY_PICTURES", "Pictures", 14)
            replaceFolder("DIRECTORY_DOWNLOADS", "Download", 14, expected = 3)
        }
        mediaFileMethod(
            GrokNames.GENERIC_MEDIA_SAVE_METHOD,
            listOf("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/lang/String;"),
            false, "V",
        ).method.apply {
            requireRegisterCount(9)
            replaceFolder("DIRECTORY_DOWNLOADS", "Download", 0)
        }
        mediaFileMethod(
            GrokNames.LEGACY_MEDIA_SAVE_METHOD,
            listOf("Ljava/lang/String;", "Ljava/lang/String;", "Landroid/content/Context;", "Ljava/lang/String;"),
            false, "V",
        ).method.apply {
            requireRegisterCount(7)
            val mkdirIndex = indexOfReference(Opcode.INVOKE_VIRTUAL, "Ljava/io/File;->mkdir()Z")
            requireRegisters(mkdirIndex, 6)
            replaceInstruction(mkdirIndex, "invoke-virtual {p4}, Ljava/io/File;->mkdirs()Z")
        }
    }
}
