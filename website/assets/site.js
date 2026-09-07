(function () {
  "use strict";

  var api = "https://api.github.com/repos/Phoenix1185/phnx-browser/releases";
  var releaseRoot = "https://github.com/Phoenix1185/phnx-browser/releases";

  function escapeHtml(value) {
    return String(value == null ? "" : value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  function formatDate(value) {
    if (!value) return "Date not published";
    var date = new Date(value);
    return Number.isNaN(date.getTime()) ? "Date not published" : date.toLocaleDateString(undefined, {
      year: "numeric",
      month: "long",
      day: "numeric"
    });
  }

  function apkAsset(release) {
    return (release.assets || []).find(function (asset) {
      return /\.apk$/i.test(asset.name || "") && /^https:\/\//.test(asset.browser_download_url || "");
    });
  }

  function checksum(release) {
    var body = release.body || "";
    var match = body.match(/(?:sha-?256|checksum)\s*[:`-]?\s*([a-f0-9]{64})/i);
    return match ? match[1] : "Not published in this release";
  }

  function releaseLinks(release) {
    var apk = apkAsset(release);
    var links = '<a class="button" href="' + escapeHtml(release.html_url || releaseRoot) + '">Release notes</a>';
    if (apk) links += ' <a class="button primary" href="' + escapeHtml(apk.browser_download_url) + '">Download APK</a>';
    return links;
  }

  function renderPanel(panel, release) {
    var title = panel.querySelector("[data-release-title]");
    var copy = panel.querySelector("[data-release-copy]");
    var meta = panel.querySelector("[data-release-meta]");
    var actions = panel.querySelector("[data-release-actions]");
    if (!release) {
      if (title) title.textContent = "No signed public release is currently available.";
      if (copy) copy.textContent = "The official GitHub Releases feed has no published stable Android release yet. This page does not expose debug, unsigned, or CI artifacts.";
      if (meta) meta.textContent = "Release status: waiting for the first public stable release";
      if (actions) actions.innerHTML = '<a class="button" href="' + releaseRoot + '">Open GitHub Releases</a>';
      return;
    }
    var apk = apkAsset(release);
    if (title) title.textContent = release.name || release.tag_name || "Published PHNX release";
    if (copy) copy.textContent = apk ? "A published APK is available from this official GitHub release." : "This release has no APK asset published yet.";
    if (meta) meta.innerHTML = "Version: " + escapeHtml(release.tag_name || "Unversioned") + " · Published: " + escapeHtml(formatDate(release.published_at)) + " · SHA-256: " + escapeHtml(checksum(release));
    if (actions) actions.innerHTML = releaseLinks(release);
  }

  function renderReleaseList(container, releases) {
    if (!releases.length) {
      container.innerHTML = '<div class="note">No signed public release is currently available. Release notes will appear here after an official GitHub Release is published.</div>';
      return;
    }
    container.innerHTML = releases.map(function (release) {
      var apk = apkAsset(release);
      return '<article class="release-item"><h3>' + escapeHtml(release.name || release.tag_name || "Published PHNX release") + '</h3>' +
        '<p>Version ' + escapeHtml(release.tag_name || "Unversioned") + ' · Published ' + escapeHtml(formatDate(release.published_at)) + (apk ? " · APK available" : " · No APK asset") + '</p>' +
        '<div class="actions">' + releaseLinks(release) + '</div></article>';
    }).join("");
  }

  function loadReleases() {
    var panels = Array.prototype.slice.call(document.querySelectorAll("[data-release-panel]"));
    var lists = Array.prototype.slice.call(document.querySelectorAll("[data-release-list]"));
    if (!panels.length && !lists.length) return;
    fetch(api + "?per_page=20", { headers: { Accept: "application/vnd.github+json" } })
      .then(function (response) {
        if (!response.ok) throw new Error("release service returned " + response.status);
        return response.json();
      })
      .then(function (releases) {
        var published = (releases || []).filter(function (release) { return !release.draft && !release.prerelease; });
        panels.forEach(function (panel) { renderPanel(panel, published[0] || null); });
        lists.forEach(function (list) { renderReleaseList(list, published); });
      })
      .catch(function () {
        panels.forEach(function (panel) { renderPanel(panel, null); });
        lists.forEach(function (list) { renderReleaseList(list, []); });
      });
  }

  var menu = document.querySelector(".menu");
  var navlinks = document.querySelector(".navlinks");
  if (menu && navlinks) {
    menu.addEventListener("click", function () {
      var open = navlinks.classList.toggle("open");
      menu.setAttribute("aria-expanded", String(open));
    });
    navlinks.addEventListener("click", function (event) {
      if (event.target.tagName === "A") navlinks.classList.remove("open");
    });
  }
  loadReleases();
}());
