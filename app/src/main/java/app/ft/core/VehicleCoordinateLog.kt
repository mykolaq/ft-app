package app.ft.core

import java.util.Locale

/**
 * Only controls diagnostic text; never suppresses live ICCOA/Baidu or AA GPS.
 * No exact position is formatted unless the owner explicitly opts in.
 */
internal object VehicleCoordinateLog {
    private val coordinateValues = Regex("(?i)\\b(latitude|longitude|rawLat|rawLon)=\\S+")

    fun format(
        sample: VehicleLocation,
        decision: VehicleLogSampler.Decision,
        includeCoordinates: Boolean
    ): String = buildString {
        val now = sample.receivedAtMs
        append("reason=${decision.reason} mode=${if (decision.fast) "EVENT_1HZ" else "NORMAL_10S"} ")
        append("protocol=${sample.source} timeMs=$now ")
        if (includeCoordinates) {
            fun dec(value: Double?) = value?.let { String.format(Locale.US, "%.7f", it) } ?: "NA"
            append("latitude=${dec(sample.latitude)} longitude=${dec(sample.longitude)} ")
            append("rawLat=${sample.rawLatitude ?: "NA"} rawLon=${sample.rawLongitude ?: "NA"} ")
        } else {
            append("coordinates=OMITTED ")
        }
        append("speedRaw=${sample.speedRaw ?: "NA"} headingRaw=${sample.headingRaw ?: "NA"} ")
        append("satUsed=${sample.satellitesUsed ?: "NA"} satVisible=${sample.satellitesVisible ?: "NA"} ")
        append("carTimestamp=${sample.deviceTimestampRaw ?: "NA"} ")
        append("positionAgeMs=${if (sample.positionAtMs == 0L) -1 else now - sample.positionAtMs} ")
        append("units=${sample.coordinatesUnit}/${sample.speedUnit}/${sample.headingUnit}")
    }

    /** Scrub old VehicleGPS records when the current setting is off during ZIP export. */
    fun scrubStoredLine(line: String): String {
        if (!line.contains("VehicleGPS:", ignoreCase = true)) return line
        return coordinateValues.replace(line) { match ->
            "${match.groupValues[1]}=<redacted>"
        }
    }
}
