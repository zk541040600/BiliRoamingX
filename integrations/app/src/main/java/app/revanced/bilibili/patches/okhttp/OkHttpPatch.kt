package app.revanced.bilibili.patches.okhttp

import android.util.Pair
import androidx.annotation.Keep
import app.revanced.bilibili.api.BrotliInputStream
import app.revanced.bilibili.patches.main.ApplicationDelegate
import app.revanced.bilibili.patches.okhttp.hooks.*
import app.revanced.bilibili.settings.Settings
import app.revanced.bilibili.utils.Logger
import app.revanced.bilibili.utils.safeContent
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

@Suppress("unused")
object OkHttpPatch {
    @JvmStatic
    private val hooks = arrayOf(
        BangumiCards,
        BangumiMaterial,
        DisableAvif,
        DmAd,
        DmQoeShow,
        Eps,
        FeedIndex,
        GarbSuitDetail,
        GrpcPlayViewUnite,
        GrpcUnlockAreaLimitForPlay,
        HistoryReport,
        Media,
        ReplyAdd,
        RoomPlayInfo,
        SearchAll,
        SearchByType,
        SearchDefaultWords,
        SearchRecommend,
        SearchSquare,
        Season,
        SeasonRecommend,
        ShareChannels,
        ShareClick,
        Skin,
        Space,
        Subtitle,
        UnlockEpisodesForPlay,
        Upgrade,
        ViewLikeTriple,
        VipAds,
        VipPrivilegeInfo,
    )

    @Keep
    @JvmStatic
    fun shouldHook(url: String, code: Int): Boolean {
        if (!ApplicationDelegate.attached())
            return false
        Logger.debug { "OkHttpPatch.shouldHook, code: $code, url: ${url.safeContent}" }
        return try {
            (code == 200 && Settings.Debug()) || hooks.any { hook ->
                try {
                    hook.shouldHook(url, code)
                } catch (t: Throwable) {
                    Logger.error(t) {
                        "OkHttpPatch.shouldHook failed, code: $code, url: ${url.safeContent}, hook: ${hook.javaClass.name}"
                    }
                    false
                }
            }
        } catch (t: Throwable) {
            Logger.error(t) { "OkHttpPatch.shouldHook fallback, code: $code, url: ${url.safeContent}" }
            false
        }
    }

    @JvmStatic
    fun hook(url: String, code: Int, request: String, response: String): String {
        val hook = hooks.firstOrNull {
            try {
                it.shouldHook(url, code)
            } catch (t: Throwable) {
                Logger.error(t) {
                    "OkHttpPatch.hook shouldHook failed, code: $code, url: ${url.safeContent}, hook: ${it.javaClass.name}"
                }
                false
            }
        } ?: return response
        return try {
            hook.hook(url, code, request, response)
        } catch (t: Throwable) {
            Logger.error(t) {
                "OkHttpPatch.hook fallback, code: $code, url: ${url.safeContent}, hook: ${hook.javaClass.name}"
            }
            response
        }
    }

    @Keep
    @JvmStatic
    fun hook(
        url: String,
        code: Int,
        reqEncoding: String?,
        reqStream: InputStream,
        respEncoding: String?,
        respStream: InputStream
    ): String {
        return try {
            val request = if (reqStream.available() == 0) {
                ""
            } else (when (reqEncoding) {
                "gzip" -> GZIPInputStream(reqStream)
                "deflate" -> InflaterInputStream(reqStream)
                "br" -> BrotliInputStream(reqStream)
                else -> reqStream
            }).bufferedReader().use { it.readText() }
            val response = (when (respEncoding) {
                "gzip" -> GZIPInputStream(respStream)
                "deflate" -> InflaterInputStream(respStream)
                "br" -> BrotliInputStream(respStream)
                else -> respStream
            }).bufferedReader().use { it.readText() }
            Logger.debug { "OkHttpPatch.hook, code: $code, url: ${url.safeContent}" }
            Logger.debug { "OkHttpPatch.hook, request, encoding: $reqEncoding, content: ${request.safeContent}" }
            Logger.debug { "OkHttpPatch.hook, response, encoding: $respEncoding, content: ${response.safeContent}" }
            hook(url, code, request, response)
        } catch (t: Throwable) {
            Logger.error(t) {
                "OkHttpPatch.hook stream fallback, code: $code, url: ${url.safeContent}, respEncoding: $respEncoding"
            }
            ""
        }
    }

    @Keep
    @JvmStatic
    fun hookBefore(url: String, headers: Array<String>): Pair<String, Array<String>> {
        if (!ApplicationDelegate.attached())
            return Pair.create(url, headers)
        val hook = hooks.find {
            try {
                it.shouldHookBefore(url, headers)
            } catch (t: Throwable) {
                Logger.error(t) {
                    "OkHttpPatch.hookBefore shouldHook failed, url: ${url.safeContent}, hook: ${it.javaClass.name}"
                }
                false
            }
        } ?: return Pair.create(url, headers)
        return try {
            hook.hookBefore(url, headers)
        } catch (t: Throwable) {
            Logger.error(t) {
                "OkHttpPatch.hookBefore fallback, url: ${url.safeContent}, hook: ${hook.javaClass.name}"
            }
            Pair.create(url, headers)
        }
    }
}
