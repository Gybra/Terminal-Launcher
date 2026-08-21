package com.gybra.terminallauncher.ui.home

/**
 * Every row Home keeps, named once, so the screen and a test never spell the same key twice.
 * A row listing something the device carries names it through [rowKey].
 */
public enum class HomeItem(public val key: String) {
    SHORTCUT("shortcut"),
    SEARCH("search"),
    HISTORY("history"),
    ;

    /** Names the row listing what [id] identifies, such as one shortcut or one search result. */
    public fun rowKey(id: String): String = "$key-$id"
}
