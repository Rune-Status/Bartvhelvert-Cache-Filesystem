package io.github.runedata.cache.filesystem.crypto

import org.bouncycastle.util.encoders.Hex
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WhirlPoolTest {
    @Test
    fun `Hash "The quick brown fox jumps over the lazy dog"`() {
        hashTest(
            string = "The quick brown fox jumps over the lazy dog",
            expededHash = "B97DE512E91E3828B40D2B0FDCE9CEB3C4A71F9BEA8D88E75C4FA854DF36725FD2B52EB6544EDCACD6F8BEDDFE" +
                    "A403CB55AE31F03AD62A5EF54E42EE82C3FB35"
        )
    }

    @Test
    fun `Hash "The quick brown fox jumps over the lazy eog"`() {
        hashTest(
            string = "The quick brown fox jumps over the lazy eog",
            expededHash = "C27BA124205F72E6847F3E19834F925CC666D0974167AF915BB462420ED40CC50900D85A1F923219D832357750" +
                    "492D5C143011A76988344C2635E69D06F2D38C"
        )
    }

    @Test
    fun `Hash ""`() {
        hashTest(
            string = "",
            expededHash = "19FA61D75522A4669B44E39C1D2E1726C530232130D407F89AFEE0964997F7A73E83BE698B288FEBCF88E3E03C" +
                    "4F0757EA8964E59B63D93708B138CC42A66EB3"
        )
    }

    private fun hashTest(string: String, expededHash: String) {
        val hash1 = whirlPoolHash(string.toByteArray())
        val expecedHash =  Hex.decode(expededHash)
        assert(hash1.contentEquals(expecedHash))
    }
}