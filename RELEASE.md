# PHNX Browser Release Setup

The normal `Android` workflow intentionally produces unsigned debug and release artifacts for validation. A public production release must use the tag-triggered `Signed Release` workflow.

Configure these repository secrets before creating a `v*` tag:

- `PHNX_KEYSTORE_BASE64`: base64-encoded Android release keystore.
- `PHNX_KEYSTORE_PASSWORD`: keystore password.
- `PHNX_KEY_ALIAS`: signing key alias.
- `PHNX_KEY_PASSWORD`: signing key password.
- `PHNX_UPDATE_SIGNING_KEY_BASE64`: Base64-wrapped PKCS#8 PEM ECDSA P-256 private key for update manifests.

Private signing material must never be committed. The workflow decodes the Android keystore and the separate update-signing key only inside the runner's temporary directory, verifies the signed APK/AAB, signs the update manifest with ECDSA, computes SHA-256 checksums, and publishes the tagged release. The public update key is embedded in `UpdateSigningKey.kt`; it is not the Android release key.

The signed `phnx-update-manifest.json` is published as a GitHub Release asset beside the APK and AAB. The app rejects unsigned, malformed, or incorrectly signed manifests before use.
