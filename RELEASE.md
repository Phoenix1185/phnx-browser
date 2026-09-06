# PHNX Browser Release Setup

The normal `Android` workflow intentionally produces unsigned debug and release artifacts for validation. A public production release must use the tag-triggered `Signed Release` workflow.

Configure these repository or protected-environment secrets before creating a `v*` tag:

- `PHNX_KEYSTORE_BASE64`: base64-encoded Android release keystore.
- `PHNX_KEYSTORE_PASSWORD`: keystore password.
- `PHNX_KEY_ALIAS`: signing key alias.
- `PHNX_KEY_PASSWORD`: signing key password.

Private signing material must never be committed. The workflow decodes the keystore only inside the runner's temporary directory, verifies the signed APK, packages the AAB, computes SHA-256 checksums, and publishes the tagged release.

The self-update manifest still requires a separately managed update-signing key and release endpoint. It is intentionally not generated from an unsigned build or from a private key stored in this repository.
