package com.controlcom.app

object AppConfig {
    // 사용자-facing URL (GitHub Pages /docs)
    private const val GITHUB_OWNER = "suikim135"
    private const val GITHUB_REPO = "controlcom"
    private const val PAGES_BASE = "https://${GITHUB_OWNER}.github.io/${GITHUB_REPO}"

    const val WEBSITE_URL = "$PAGES_BASE/"
    const val PC_DOWNLOAD_URL = "$PAGES_BASE/download.html"
    const val PRIVACY_POLICY_URL = "$PAGES_BASE/privacy-policy.html"
    const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.controlcom.app"

    const val DEFAULT_PORT = 7847
}
