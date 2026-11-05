package com.mliem.scientific_calculator.data

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkInstallerTest {
    @Test
    fun onlyHttpsDownloadsAreTrusted() {
        assertTrue(ApkInstaller.isTrustedUrl("https://github.com/x/releases/download/a.apk"))
        assertFalse(ApkInstaller.isTrustedUrl("http://github.com/x/a.apk"))
        assertFalse(ApkInstaller.isTrustedUrl("file:///sdcard/a.apk"))
    }

    @Test
    fun checksumMustMatchWhenGithubReportsOne() {
        assertTrue(ApkInstaller.checksumMatches("abc123", "ABC123"))
        assertFalse(ApkInstaller.checksumMatches("abc123", "def456"))
        assertTrue(ApkInstaller.checksumMatches("abc123", null))
    }

    @Test
    fun newestCarriesSizeAndDigestOfTheApk() {
        val releases = JSONArray(
            """[{"tag_name":"nightly-20260612-300","html_url":"https://github.com/r","assets":[
                {"name":"notes.txt","size":5,"digest":"sha256:no","browser_download_url":"https://x/notes.txt"},
                {"name":"calc.apk","size":1234,"digest":"sha256:abcdef","browser_download_url":"https://x/calc.apk"}]}]"""
        )
        val info = Updater.newest(releases, installed = 100)!!
        assertEquals("https://x/calc.apk", info.apkUrl)
        assertEquals(1234L, info.sizeBytes)
        assertEquals("abcdef", info.sha256)
    }

    @Test
    fun newestLeavesDigestEmptyWhenMissing() {
        val releases = JSONArray(
            """[{"tag_name":"nightly-20260612-300","html_url":"https://github.com/r","assets":[
                {"name":"calc.apk","size":9,"browser_download_url":"https://x/calc.apk"}]}]"""
        )
        assertNull(Updater.newest(releases, installed = 100)!!.sha256)
    }
}
