package net.asksakis.massdroidv2.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomePinTest {

    private fun pin(id: String, kind: HomePin.Kind = HomePin.Kind.PLAYLIST, name: String = "P$id") =
        HomePin(uri = "library://${kind.name.lowercase()}/$id", kind = kind, itemId = id, provider = "library", name = name)

    @Test
    fun `pinning puts the newest first`() {
        val result = HomePin.applyPin(listOf(pin("1"), pin("2")), pin("3"), pinned = true)
        assertThat(result.map { it.itemId }).containsExactly("3", "1", "2").inOrder()
    }

    @Test
    fun `re-pinning moves to front and refreshes the copied name`() {
        val result = HomePin.applyPin(listOf(pin("1"), pin("2")), pin("2", name = "Renamed"), pinned = true)
        assertThat(result.map { it.itemId }).containsExactly("2", "1").inOrder()
        assertThat(result.first().name).isEqualTo("Renamed")
    }

    @Test
    fun `unpinning removes only that uri`() {
        val result = HomePin.applyPin(listOf(pin("1"), pin("2")), pin("1"), pinned = false)
        assertThat(result.map { it.itemId }).containsExactly("2")
    }

    @Test
    fun `list is capped dropping the oldest`() {
        val full = (1..HomePin.MAX_PINS).map { pin("$it") }
        val result = HomePin.applyPin(full, pin("new"), pinned = true)
        assertThat(result).hasSize(HomePin.MAX_PINS)
        assertThat(result.first().itemId).isEqualTo("new")
        assertThat(result.map { it.itemId }).doesNotContain("${HomePin.MAX_PINS}")
    }

    @Test
    fun `round trips albums and playlists`() {
        val pins = listOf(pin("1", HomePin.Kind.ALBUM), pin("2").copy(imageUrl = "http://x/y.jpg"))
        assertThat(HomePin.decode(HomePin.encode(pins))).isEqualTo(pins)
    }

    @Test
    fun `corrupt or empty value reads as no pins`() {
        assertThat(HomePin.decode(null)).isEmpty()
        assertThat(HomePin.decode("")).isEmpty()
        assertThat(HomePin.decode("not json")).isEmpty()
    }

    @Test
    fun `decode drops duplicate and blank uris`() {
        val raw = HomePin.encode(listOf(pin("1"), pin("1", name = "dup"), pin("2").copy(uri = "")))
        assertThat(HomePin.decode(raw).map { it.name }).containsExactly("P1")
    }

    @Test
    fun `only albums and playlists can be pinned`() {
        assertThat(HomePin.kindOf(MediaType.ALBUM)).isEqualTo(HomePin.Kind.ALBUM)
        assertThat(HomePin.kindOf(MediaType.PLAYLIST)).isEqualTo(HomePin.Kind.PLAYLIST)
        assertThat(HomePin.kindOf(MediaType.TRACK)).isNull()
    }
}
