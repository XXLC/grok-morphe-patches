package io.github.xxlc.patches.grok

import app.morphe.patcher.patch.PatchBuilder
import app.morphe.patcher.patch.booleanOption

// Chọn patch là áp dụng; option cho phép tắt mà vẫn giữ lựa chọn trong Manager.
// Extension không lưu cài đặt lúc chạy.
internal fun PatchBuilder<*>.enabledOption(default: Boolean = true) = booleanOption(
    key = "enabled",
    default = default,
    title = "Bật bản vá",
    description = "Mặc định áp dụng khi chọn bản vá; tắt để bỏ qua thay đổi mà vẫn giữ lựa chọn.",
    required = true,
)
