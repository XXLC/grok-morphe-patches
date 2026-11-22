package io.github.xxlc.patches.grok

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.immutable.ImmutableField

// Hạ tầng không tên, chỉ thêm một lần dù nhiều patch cùng phụ thuộc.
internal val postActionFlagsPatch = bytecodePatch {
    compatibleWith(grokCompatibility)

    execute {
        val actionClass = mutableClassDefBy(GrokNames.DOWNLOAD_ACTION)
        listOf(
            GrokNames.DOWNLOAD_ALL_FLAG_NAME,
            GrokNames.COPY_PROMPT_FLAG_NAME,
        ).forEach { name ->
            check(actionClass.fields.none { it.name == name }) {
                "Trường cờ hành động đã tồn tại: $name"
            }
            actionClass.fields.add(
                ImmutableField(
                    GrokNames.DOWNLOAD_ACTION, name, "Z", AccessFlags.PUBLIC.value,
                    null, emptySet(), emptySet(),
                ).toMutable(),
            )
        }

        DownloadActionEqualsFingerprint.method.apply {
            requireRegisterCount(5)
            // v0..v2 chưa sống ở đầu; giữ p0/p1 để phần gốc so trạng thái a.
            // hashCode được phép trùng khi equals khác nhau, không đổi giao ước.
            addInstructionsWithLabels(
                0,
                """
                    instance-of v0, p1, ${GrokNames.DOWNLOAD_ACTION}
                    if-eqz v0, :d2_flags_original_equals
                    move-object v0, p1
                    check-cast v0, ${GrokNames.DOWNLOAD_ACTION}
                    iget-boolean v1, p0, ${GrokNames.DOWNLOAD_ALL_FLAG}
                    iget-boolean v2, v0, ${GrokNames.DOWNLOAD_ALL_FLAG}
                    if-ne v1, v2, :d2_flags_different
                    iget-boolean v1, p0, ${GrokNames.COPY_PROMPT_FLAG}
                    iget-boolean v2, v0, ${GrokNames.COPY_PROMPT_FLAG}
                    if-eq v1, v2, :d2_flags_original_equals
                    :d2_flags_different
                    const/4 v0, 0x0
                    return v0
                    :d2_flags_original_equals
                    nop
                """,
            )
        }
    }
}
