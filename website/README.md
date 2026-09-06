# PHNX Browser Website

This is the static GitHub Pages site for PHNX Browser. It uses the repository's official icon and links only to the actual Phoenix repository, issues, releases, and Actions pages.

The site intentionally does not expose unsigned CI artifacts as public product downloads or claim that a signed release exists. Update `index.html` when a real release is published.

To activate deployment, enable GitHub Pages with `GitHub Actions` as the source and set the repository variable `PAGES_ENABLED` to `true`. The workflow is intentionally dormant until that one-time repository configuration exists.
