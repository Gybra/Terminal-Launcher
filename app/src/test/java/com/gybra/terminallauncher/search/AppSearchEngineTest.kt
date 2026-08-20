package com.gybra.terminallauncher.search

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.search.SearchResult.Match
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppSearchEngineTest {
    @Test
    fun `returns no results for a blank query`() {
        assertEquals(emptyList<SearchResult>(), AppSearchEngine.search(query = "   ", apps = apps))
    }

    @Test
    fun `returns no results when no label matches`() {
        assertEquals(
            emptyList<SearchResult>(),
            AppSearchEngine.search(query = "calendar", apps = apps),
        )
    }

    @Test
    fun `ranks exact before prefix before substring matches`() {
        val results = AppSearchEngine.search(query = "mail", apps = apps)

        assertEquals(
            listOf(
                SearchResult(app = mail, match = Match.EXACT),
                SearchResult(app = mailbox, match = Match.PREFIX),
                SearchResult(app = mailer, match = Match.PREFIX),
                SearchResult(app = airMail, match = Match.SUBSTRING),
                SearchResult(app = gmail, match = Match.SUBSTRING),
            ),
            results,
        )
    }

    @Test
    fun `ignores case and surrounding whitespace in the query`() {
        assertEquals(
            AppSearchEngine.search(query = "mail", apps = apps),
            AppSearchEngine.search(query = "  MaIl  ", apps = apps),
        )
    }

    @Test
    fun `orders identical labels by package name`() {
        val firstMail = InstalledApp(packageName = "com.a.mail", label = "Mail")
        val secondMail = InstalledApp(packageName = "com.b.mail", label = "mail")

        val results = AppSearchEngine.search(query = "mail", apps = listOf(secondMail, firstMail))

        assertEquals(listOf(firstMail, secondMail), results.map(SearchResult::app))
    }

    @Test
    fun `limits results to five applications`() {
        val manyApps = (1..7).map { index ->
            InstalledApp(packageName = "com.example.mail$index", label = "Mail $index")
        }

        val results = AppSearchEngine.search(query = "mail", apps = manyApps)

        assertEquals(manyApps.take(5), results.map(SearchResult::app))
    }

    @Test
    fun `resolves a single result as unambiguous`() {
        val results = AppSearchEngine.search(query = "gma", apps = apps)

        assertEquals(gmail, AppSearchEngine.unambiguousMatch(results))
    }

    @Test
    fun `resolves the only exact match among several results`() {
        val results = AppSearchEngine.search(query = "mail", apps = apps)

        assertEquals(mail, AppSearchEngine.unambiguousMatch(results))
    }

    @Test
    fun `keeps several inexact results ambiguous`() {
        val results = AppSearchEngine.search(query = "mail", apps = listOf(mailbox, gmail))

        assertNull(AppSearchEngine.unambiguousMatch(results))
    }

    @Test
    fun `keeps repeated exact matches ambiguous`() {
        val duplicate = InstalledApp(packageName = "com.other.mail", label = "MAIL")

        val results = AppSearchEngine.search(query = "mail", apps = listOf(mail, duplicate))

        assertNull(AppSearchEngine.unambiguousMatch(results))
    }

    @Test
    fun `resolves no application without results`() {
        assertNull(AppSearchEngine.unambiguousMatch(emptyList()))
    }

    private val mail = InstalledApp(packageName = "com.example.mail", label = "Mail")
    private val mailbox = InstalledApp(packageName = "com.example.mailbox", label = "Mailbox")
    private val mailer = InstalledApp(packageName = "com.example.mailer", label = "Mailer")
    private val airMail = InstalledApp(packageName = "com.example.airmail", label = "AirMail")
    private val gmail = InstalledApp(packageName = "com.example.gmail", label = "Gmail")
    private val apps = listOf(gmail, mailer, mail, airMail, mailbox)
}
