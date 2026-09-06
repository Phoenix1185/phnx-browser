package com.phoenix.phnx.update

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun requiresDeltaMetadataOnlyForDeltaUpdates() {
        val full = sampleManifest()
        val delta = full.copy(
            updateType = UpdateType.DELTA,
            deltaUrl = "https://example.com/phnx.patch",
            deltaSha256 = "b".repeat(64),
        )

        assertTrue(full.hasValidArtifactMetadata())
        assertFalse(full.copy(deltaUrl = "https://example.com/phnx.patch").hasValidArtifactMetadata())
        assertFalse(delta.copy(deltaSha256 = null).hasValidArtifactMetadata())
        assertTrue(delta.hasValidArtifactMetadata())
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

    @Test
    fun stagesArtifactsAtomicallyAndRollsBackPreviousArtifact() {
        val root = Files.createTempDirectory("phnx-update").toFile()
        try {
            val first = "first artifact".toByteArray()
            val second = "second artifact".toByteArray()
            val firstHash = "69f6245a92f0c902e45cfd6e99297cad3e536598237b6ef3d04fbb59c8a3b095"
            val secondHash = "60c48ddce35530a43716c40331da2e737fca5f9b2468c01396726ab7d4f351b2"
            val store = AtomicUpdateStore(root)

            assertNotNull(store.stage(ByteArrayInputStream(first), firstHash))
            assertEquals("first artifact", store.currentArtifact()?.readText())
            assertEquals(null, store.stage(ByteArrayInputStream("tampered".toByteArray()), firstHash))
            assertEquals("first artifact", store.currentArtifact()?.readText())
            assertNotNull(store.stage(ByteArrayInputStream(second), secondHash))
            assertEquals("second artifact", store.currentArtifact()?.readText())
            assertTrue(store.rollback())
            assertEquals("first artifact", store.currentArtifact()?.readText())
            assertFalse(store.rollback())
        } finally {
            root.deleteRecursively()
        }
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
