package io.github.xxlc.patches.grok

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.resource.PublicXmlManager
import org.w3c.dom.Element

private val stringIds = mutableMapOf<String, Int>()

// Chỉ đọc sau khi dependency tài nguyên đã chạy; không trả về ID 0 giả hợp lệ.
internal fun stringId(name: String): Int = stringIds.getValue(name)

// Không có tên để patch dùng chung không xuất hiện trong danh sách của Manager.
internal val downloadResourcesPatch = resourcePatch {
    compatibleWith(grokCompatibility)

    execute {
        stringIds.clear()
        listOf("values", "values-vi").forEach { directory ->
            val path = "res/$directory/strings.xml"
            val file = get(path)
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.writeText("<?xml version=\"1.0\" encoding=\"utf-8\"?><resources/>")
            }
            document(path).use { document ->
                val strings = document.getElementsByTagName("string")
                val existing = (0 until strings.length).map { strings.item(it) as Element }
                xxlcStrings.forEach { string ->
                    val element = existing.singleOrNull { it.getAttribute("name") == string.name }
                        ?: document.createElement("string").also {
                            it.setAttribute("name", string.name)
                            document.documentElement.appendChild(it)
                        }
                    // DOM giữ nguyên %1$d; dấu $ chỉ cần escape trong mã Kotlin.
                    element.textContent = if (directory == "values-vi") string.vi else string.en
                }
            }
        }

        // resourceId() chỉ thấy tài nguyên đầu vào. Cấp ID mới trong public.xml
        // để cả bytecode và bước biên dịch tài nguyên dùng cùng một ID.
        PublicXmlManager(get("res/values/public.xml")).use { publicIds ->
            xxlcStrings.forEach { string ->
                publicIds.createPublicId("string", string.name)
                stringIds[string.name] = publicIds.getDefinedIds().getValue("string" to string.name)
            }
        }
    }
}
