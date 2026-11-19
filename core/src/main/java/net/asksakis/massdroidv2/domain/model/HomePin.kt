package net.asksakis.massdroidv2.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * An album or playlist the listener pinned to Home.
 *
 * Local only, like the playlist "Pin to top": Music Assistant has nothing to hold it. The
 * name and artwork are copied at pin time so the row draws at once, offline and before the
 * library has loaded; tapping a pin opens the item by its URI, which is always current.
 */
@Serializable
data class HomePin(
    val uri: String,
    val kind: Kind,
    val itemId: String,
    val provider: String,
    val name: String,
    val imageUrl: String? = null,
    val subtitle: String = ""
) {
    @Serializable
    enum class Kind { ALBUM, PLAYLIST }

    fun toAlbum(): Album = Album(
        itemId = itemId,
        provider = provider,
        name = name,
        uri = uri,
        imageUrl = imageUrl,
        artistNames = subtitle
    )

    fun toPlaylist(): Playlist = Playlist(
        itemId = itemId,
        provider = provider,
        name = name,
        uri = uri,
        imageUrl = imageUrl
    )

    companion object {
        /** Home shows one row; past this a pin list is a second library, not a shortcut. */
        const val MAX_PINS = 12

        private val json = Json { ignoreUnknownKeys = true }
        private val listSerializer = ListSerializer(serializer())

        fun kindOf(mediaType: MediaType): Kind? = when (mediaType) {
            MediaType.ALBUM -> Kind.ALBUM
            MediaType.PLAYLIST -> Kind.PLAYLIST
            else -> null
        }

        /** A corrupt or foreign value reads as no pins rather than failing Home. */
        fun decode(raw: String?): List<HomePin> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching { json.decodeFromString(listSerializer, raw) }
                .getOrDefault(emptyList())
                .filter { it.uri.isNotBlank() }
                .distinctBy { it.uri }
        }

        fun encode(pins: List<HomePin>): String = json.encodeToString(listSerializer, pins)

        /**
         * Pinning puts the item first (newest pin leads the row) and refreshes its copied
         * name and art if it was already pinned; unpinning removes it. The list is capped at
         * [MAX_PINS], dropping the oldest.
         */
        fun applyPin(current: List<HomePin>, pin: HomePin, pinned: Boolean): List<HomePin> {
            val without = current.filterNot { it.uri == pin.uri }
            return if (pinned) (listOf(pin) + without).take(MAX_PINS) else without
        }
    }
}
