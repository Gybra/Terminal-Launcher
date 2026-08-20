package com.gybra.terminallauncher.search

import com.gybra.terminallauncher.launcher.AppUsage
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

    @Test
    fun `matches labels whose characters appear in order`() {
        val results = AppSearchEngine.search(query = "mlb", apps = apps)

        assertEquals(listOf(SearchResult(app = mailbox, match = Match.FUZZY)), results)
    }

    @Test
    fun `ignores labels holding the query characters out of order`() {
        assertEquals(
            emptyList<SearchResult>(),
            AppSearchEngine.search(query = "bm", apps = listOf(mailbox)),
        )
    }

    @Test
    fun `ranks fuzzy matches after every literal match`() {
        val results = AppSearchEngine.search(
            query = "mail",
            apps = listOf(mountainTrail, gmail, mailbox, mail),
        )

        assertEquals(
            listOf(
                SearchResult(app = mail, match = Match.EXACT),
                SearchResult(app = mailbox, match = Match.PREFIX),
                SearchResult(app = gmail, match = Match.SUBSTRING),
                SearchResult(app = mountainTrail, match = Match.FUZZY),
            ),
            results,
        )
    }

    @Test
    fun `ranks pinned applications first among equally strong matches`() {
        val results = AppSearchEngine.search(
            query = "mail",
            apps = listOf(mailbox, mailer),
            pinnedPackages = setOf(mailer.packageName),
        )

        assertEquals(listOf(mailer, mailbox), results.map(SearchResult::app))
    }

    @Test
    fun `ranks the most launched application first among equally strong matches`() {
        val results = AppSearchEngine.search(
            query = "mail",
            apps = listOf(mailbox, mailer),
            usage = mapOf(mailer.packageName to AppUsage(launchCount = 3)),
        )

        assertEquals(listOf(mailer, mailbox), results.map(SearchResult::app))
    }

    @Test
    fun `ranks the most recently launched application first when launch counts tie`() {
        val results = AppSearchEngine.search(
            query = "mail",
            apps = listOf(mailbox, mailer),
            usage = mapOf(
                mailbox.packageName to AppUsage(launchCount = 2, lastLaunchedAt = 10L),
                mailer.packageName to AppUsage(launchCount = 2, lastLaunchedAt = 20L),
            ),
        )

        assertEquals(listOf(mailer, mailbox), results.map(SearchResult::app))
    }

    @Test
    fun `stops rewarding launches once the usage score is capped`() {
        val results = AppSearchEngine.search(
            query = "mail",
            apps = listOf(mailbox, mailer),
            usage = mapOf(
                mailbox.packageName to AppUsage(launchCount = 25),
                mailer.packageName to AppUsage(launchCount = 100),
            ),
        )

        assertEquals(listOf(mailbox, mailer), results.map(SearchResult::app))
    }

    @Test
    fun `keeps a pinned application ahead of a far more launched one`() {
        val results = AppSearchEngine.search(
            query = "mail",
            apps = listOf(mailbox, mailer),
            usage = mapOf(mailbox.packageName to AppUsage(launchCount = 100)),
            pinnedPackages = setOf(mailer.packageName),
        )

        assertEquals(listOf(mailer, mailbox), results.map(SearchResult::app))
    }

    private val mail = InstalledApp(packageName = "com.example.mail", label = "Mail")
    private val mailbox = InstalledApp(packageName = "com.example.mailbox", label = "Mailbox")
    private val mailer = InstalledApp(packageName = "com.example.mailer", label = "Mailer")
    private val airMail = InstalledApp(packageName = "com.example.airmail", label = "AirMail")
    private val gmail = InstalledApp(packageName = "com.example.gmail", label = "Gmail")
    private val mountainTrail =
        InstalledApp(packageName = "com.example.trail", label = "Mountain Trail")
    private val apps = listOf(gmail, mailer, mail, airMail, mailbox)
}
