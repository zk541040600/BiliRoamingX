package app.revanced.bilibili.patches.okhttp.hooks

import app.revanced.bilibili.patches.okhttp.ApiHook
import app.revanced.bilibili.settings.Settings
import app.revanced.bilibili.utils.forEach
import app.revanced.bilibili.utils.orEmpty
import app.revanced.bilibili.utils.removeIf
import app.revanced.bilibili.utils.toJSONObject

object SearchSquare : ApiHook() {
    override fun shouldHook(url: String, status: Int): Boolean {
        return url.contains("/x/v2/search/square") && status.isOk
                && Settings.PurifySearchTypes().isNotEmpty()
    }

    override fun hook(url: String, status: Int, request: String, response: String): String {
        val searchTypes = Settings.PurifySearchTypes()
        return response.toJSONObject().apply {
            val data = optJSONArray("data").orEmpty()
            data.removeIf {
                val type = it.optString("type")
                (type == "trending" && searchTypes.contains("trending"))
                    || (type == "recommend" && searchTypes.contains("recommend"))
            }
            data.forEach {
                val type = it.optString("type")
                if (type == "recommend") {
                    it.optJSONObject("data")?.optJSONArray("list")?.forEach { e ->
                        e.remove("recommend_reason")
                    }
                }
            }
        }.toString()
    }
}
