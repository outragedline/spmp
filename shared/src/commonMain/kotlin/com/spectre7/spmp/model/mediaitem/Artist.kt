package com.spectre7.spmp.model.mediaitem

import SpMp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.JsonArray
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.api.*
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.ArtistPreviewLong
import com.spectre7.spmp.ui.component.ArtistPreviewSquare
import com.spectre7.spmp.ui.component.MediaItemLayout
import kotlin.concurrent.thread

class ArtistItemData(override val data_item: Artist): MediaItemData(data_item), MediaItemWithLayoutsData {
    var subscribe_channel_id: String? by mutableStateOf(null)
        private set

    fun supplySubscribeChannelId(value: String?, certain: Boolean = false, cached: Boolean = false): Artist {
        if (value != subscribe_channel_id && (subscribe_channel_id == null || certain)) {
            subscribe_channel_id = value
            onChanged(cached)
        }
        return data_item
    }

    var subscriber_count: Int? by mutableStateOf(null)
        private set

    fun supplySubscriberCount(value: Int?, certain: Boolean = false, cached: Boolean = false): Artist {
        if (value != subscriber_count && (subscriber_count == null || certain)) {
            subscriber_count = value
            onChanged(cached)
        }
        return data_item
    }

    var feed_layouts: List<MediaItemLayout>? by mutableStateOf(null)
        private set

    override fun supplyFeedLayouts(value: List<MediaItemLayout>?, certain: Boolean, cached: Boolean) {
        if (value != feed_layouts && (feed_layouts == null || certain)) {
            feed_layouts = value
            onChanged(cached)
        }
    }

    override fun getSerialisedData(klaxon: Klaxon): List<String> {
        return super.getSerialisedData(klaxon) + listOf(klaxon.toJsonString(subscribe_channel_id), klaxon.toJsonString(data_item.is_for_item), klaxon.toJsonString(subscriber_count), klaxon.toJsonString(feed_layouts))
    }

    override fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItemData {
        require(data.size >= 4)
        data.removeLast()?.also { supplyFeedLayouts(klaxon.parseFromJsonArray(it as JsonArray<*>), true, cached = true) }
        data.removeLast()?.also { supplySubscriberCount(it as Int, cached = true) }
        data_item.is_for_item = data.removeLast() as Boolean
        data.removeLast()?.also { supplySubscribeChannelId(it as String, cached = true) }
        return super.supplyFromSerialisedData(data, klaxon)
    }
}

class Artist private constructor (
    id: String,
    is_for_item: Boolean = false
): MediaItem(id), MediaItemWithLayouts {

    override val data: ArtistItemData = ArtistItemData(this)

    var is_for_item: Boolean = is_for_item
    val is_temp: Boolean get() = id.isBlank()

    val subscribe_channel_id: String? get() = data.subscribe_channel_id
    val subscriber_count: Int? get() = data.subscriber_count

    var subscribed: Boolean? by mutableStateOf(null)
    var is_own_channel: Boolean by mutableStateOf(false)

    override val feed_layouts: List<MediaItemLayout>?
        @Composable
        get() = getFeedLayouts()

    override fun getFeedLayouts(): List<MediaItemLayout>? = data.feed_layouts

    fun editArtistData(action: ArtistItemData.() -> Unit): Artist {
        if (is_for_item || is_temp) {
            action(data)
        }
        else {
            editData {
                action(this as ArtistItemData)
            }
        }
        return this
    }

    override fun isFullyLoaded(): Boolean {
        return super.isFullyLoaded() && subscribe_channel_id != null
    }

    companion object {
        private val artists: MutableMap<String, Artist> = mutableMapOf()

        fun fromId(id: String): Artist {
            check(id.isNotBlank())

            synchronized(artists) {
                return artists.getOrPut(id) {
                    val artist = Artist(id)
                    artist.loadFromCache()
                    return@getOrPut artist
                }.getOrReplacedWith() as Artist
            }
        }

        fun clearStoredItems(): Int {
            val amount = artists.size
            artists.clear()
            return amount
        }

        fun createForItem(item: MediaItem): Artist {
            synchronized(artists) {
                val id = "FS" + item.id
                return artists.getOrPut(id) {
                    val artist = Artist(id, true)
                    artist.loadFromCache()
                    return@getOrPut artist
                }.getOrReplacedWith() as Artist
            }
        }

        fun createTemp(id: String = ""): Artist {
            return Artist(id)
        }
    }

    @Composable
    override fun PreviewSquare(params: PreviewParams) {
        ArtistPreviewSquare(this, params)
    }

    @Composable
    override fun PreviewLong(params: PreviewParams) {
        ArtistPreviewLong(this, params)
    }

    override val url: String get() = "https://music.youtube.com/channel/$id"

    fun getReadableSubscriberCount(): String {
        return subscriber_count?.let { subs ->
            getString("artist_x_subscribers").replace("\$x", amountToString(subs, SpMp.ui_language))
        } ?: ""
    }

    fun updateSubscribed() {
        check(!is_for_item)

        if (is_own_channel) {
            return
        }
        subscribed = isSubscribedToArtist(this).getOrNull()
    }

    fun toggleSubscribe(toggle_before_fetch: Boolean = false, onFinished: ((success: Boolean, subscribing: Boolean) -> Unit)? = null) {
        check(!is_for_item)
        check(DataApi.ytm_authenticated)

        thread {
            if (subscribed == null) {
                throw IllegalStateException()
            }

            val target = !subscribed!!

            if (toggle_before_fetch) {
                subscribed = target
            }

            subscribeOrUnsubscribeArtist(this, target).getOrThrowHere()
            updateSubscribed()

            onFinished?.invoke(subscribed == target, target)
        }
    }
}