package org.example.project

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Hilfsklasse zum Formatieren von Datums-/Zeitwerten.
 *
 * Vorgabeformat: "dd.MM.yyyy HH:mm:ss" (z. B. 15.08.2025 10:47:00)
 *
 * Nutzung:
 *  - DateFormatter.format(Date())
 *  - DateFormatter.format(System.currentTimeMillis())
 *  - Date().format("yyyy-MM-dd")
 *  - 1692098820000L.formatAsDate()
 */
object DateFormatter {
    /**
     * Formatiert ein [Date] mit dem angegebenen Muster/Locale/Zeitzone.
     * Gibt "-" zurück, wenn [date] null ist.
     */
    @JvmStatic
    fun format(
        date: Date?,
        pattern: String = "dd.MM.yyyy HH:mm:ss",
        locale: Locale = Locale.getDefault(),
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String {
        if (date == null) return "-"
        val sdf = SimpleDateFormat(pattern, locale)
        sdf.timeZone = timeZone
        return sdf.format(date)
    }

    /**
     * Formatiert Millisekunden seit Epoch mit dem angegebenen Muster/Locale/Zeitzone.
     * Gibt "-" zurück, wenn [epochMillis] null ist.
     */
    @JvmStatic
    fun format(
        epochMillis: Long?,
        pattern: String = "dd.MM.yyyy HH:mm:ss",
        locale: Locale = Locale.getDefault(),
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String {
        if (epochMillis == null) return "-"
        return format(Date(epochMillis), pattern, locale, timeZone)
    }
}

/**
 * Erweiterungsfunktion für bequeme Nutzung auf Date.
 */
fun Date?.format(
    pattern: String = "dd.MM.yyyy HH:mm:ss",
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault(),
): String = DateFormatter.format(this, pattern, locale, timeZone)

/**
 * Erweiterungsfunktion für bequeme Nutzung auf Long (Millis seit Epoch).
 */
fun Long?.formatAsDate(
    pattern: String = "dd.MM.yyyy HH:mm:ss",
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault(),
): String = DateFormatter.format(this, pattern, locale, timeZone)
