# PHNX Browser Website

This is the static GitHub Pages site for PHNX Browser. It uses the repository's official icon and links to the actual Phoenix repository, issues, releases, and product documentation. The root `index.html` redirects legacy root-based Pages configurations into this maintained site directory.

The Download section resolves the latest stable GitHub Release through the public API and links directly to its APK asset. Until a signed stable release with an APK asset exists, it shows `Android download coming soon` and exposes no CI artifacts, debug builds, unsigned packages, or workflow links as product downloads.

To activate deployment, enable GitHub Pages with `GitHub Actions` as the source and set the repository variable `PAGES_ENABLED` to `true`. The workflow is intentionally dormant until that one-time repository configuration exists. Until then, legacy root-based Pages serves the root redirect.
