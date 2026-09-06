package com.phoenix.phnx.update

import java.io.ByteArrayInputStream
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCoreTest {
    @Test
    fun comparesNumericVersionsWithoutAllowingDowngrades() {
        assertTrue(VersionComparator.isNewer("1.10.0", "1.9.9"))
        assertFalse(VersionComparator.isNewer("1.0.0", "1.0.0"))
        assertFalse(VersionComparator.isNewer("v1.0.0-beta", "1.0.0"))
        assertEquals(0, VersionComparator.compare("1.0", "1.0.0"))
    }

    @Test
    fun verifiesSha256AndRejectsInvalidChecksums() {
        val payload = "PHNX update artifact"
        val checksum = "ea2859cdca33d2ed412d98b7e0856503560f45f0311549cc8fa4aa5505795066"

        assertTrue(ChecksumVerifier.verify(ByteArrayInputStream(payload.toByteArray()), checksum))
        assertFalse(ChecksumVerifier.verify(ByteArrayInputStream(payload.toByteArray()), "not-a-checksum"))
    }

    @Test
    fun verifiesSignedManifestAndRejectsTampering() {
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val unsigned = sampleManifest()
        val signer = Signature.getInstance("SHA256withECDSA").apply {
            initSign(keyPair.private)
            update(unsigned.canonicalPayload())
        }
        val signature = Base64.getEncoder().encodeToString(signer.sign())
        val signed = unsigned.copy(signature = signature)

        assertTrue(SignatureVerifier.verify(signed, keyPair.public))
        assertFalse(SignatureVerifier.verify(signed.copy(latestVersionCode = 2), keyPair.public))
        assertFalse(SignatureVerifier.verify(signed.copy(signature = "not-base64"), keyPair.public))
    }

    @Test
    fun verifiesSignedArtifactChecksumAndRejectsChangedBytes() {
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val payload = "signed PHNX artifact".toByteArray()
        val checksum = "4991d3e8df8e3262600232a5d20e9b25f50b660abce1bd88e31f15f21c7b1764"
        val signer = Signature.getInstance("SHA256withECDSA").apply {
            initSign(keyPair.private)
            update(payload)
        }
        val signature = Base64.getEncoder().encodeToString(signer.sign())

        assertTrue(ArtifactVerifier.verify(ByteArrayInputStream(payload), checksum, signature, keyPair.public))
        assertFalse(ArtifactVerifier.verify(ByteArrayInputStream("changed".toByteArray()), checksum, signature, keyPair.public))
    }

    @Test
    fun parsesEncodedEcPublicKey() {
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val encoded = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        assertEquals(keyPair.public, SignatureVerifier.parseEcPublicKey(encoded))
        assertEquals(null, SignatureVerifier.parseEcPublicKey("not-base64"))
    }

    @Test
    fun validatesManifestShapeBeforeUse() {
        val valid = "{" +
            "\"product\":\"phnx-browser\"," +
            "\"platform\":\"android\"," +
            "\"architecture\":\"arm64-v8a\"," +
            "\"channel\":\"stable\"," +
            "\"latestVersion\":\"1.1.0\"," +
            "\"latestVersionCode\":2," +
            "\"minimumSupportedVersionCode\":1," +
            "\"updateType\":\"full\"," +
            "\"mandatory\":false," +
            "\"fullApkUrl\":\"https://example.com/phnx.apk\"," +
            "\"fullApkSha256\":\"${"a".repeat(64)}\"}"

        assertNotNull(UpdateManifestParser.parseOrThrow(valid))
        assertNull(UpdateManifestParser.parse(valid.replace("\"minimumSupportedVersionCode\":1", "\"minimumSupportedVersionCode\":3")))
        assertNull(UpdateManifestParser.parse(valid.replace("https://example.com", "http://example.com")))
    }

    @Test
    fun enforcesSafeUpdateStateTransitions() {
        assertEquals(UpdateState.CHECKING, UpdateStateMachine.transition(UpdateState.IDLE, UpdateState.CHECKING))
        assertEquals(UpdateState.VERIFIED, UpdateStateMachine.transition(UpdateState.VERIFYING, UpdateState.VERIFIED))
        assertTrue(UpdateStateMachine.canTransition(UpdateState.RESTARTING, UpdateState.ROLLBACK_REQUIRED))
        assertFalse(UpdateStateMachine.canTransition(UpdateState.IDLE, UpdateState.APPLYING))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsafeUpdateStateTransitions() {
        UpdateStateMachine.transition(UpdateState.IDLE, UpdateState.APPLYING)
    }

    private fun sampleManifest() = UpdateManifest(
        product = "phnx-browser",
        platform = "android",
        architecture = "arm64-v8a",
        channel = UpdateChannel.STABLE,
        latestVersion = "1.1.0",
        latestVersionCode = 1,
        minimumSupportedVersionCode = 1,
        updateType = UpdateType.FULL,
        mandatory = false,
        fullApkUrl = "https://example.com/phnx.apk",
        fullApkSha256 = "a".repeat(64),
    )

}
