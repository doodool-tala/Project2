package com.monarchd.synth.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AppSettings(
    val haptics: Boolean = true,
    val showKeyLabels: Boolean = true
)

/**
 * Small, dependency-free persistence layer. Patches are stored as JSON in
 * SharedPreferences so the app has no database or serialization dependency and
 * user presets survive updates.
 */
class PatchRepository(context: Context) {

    private val prefs = context.getSharedPreferences("monarch_d", Context.MODE_PRIVATE)

    fun isFirstRun(): Boolean = !prefs.getBoolean(KEY_SEEN_INTRO, false)

    fun markIntroSeen() = prefs.edit().putBoolean(KEY_SEEN_INTRO, true).apply()

    fun loadLastPatch(): Patch {
        val raw = prefs.getString(KEY_LAST_PATCH, null) ?: return FactoryPresets.all.first { it.name == "Model One" }.patch
        return runCatching { patchFromJson(JSONObject(raw)) }
            .getOrElse { FactoryPresets.all.first { it.name == "Model One" }.patch }
    }

    fun saveLastPatch(patch: Patch) {
        prefs.edit().putString(KEY_LAST_PATCH, patchToJson(patch).toString()).apply()
    }

    fun loadUserPresets(): List<Preset> {
        val raw = prefs.getString(KEY_USER_PRESETS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val patch = patchFromJson(obj.getJSONObject("patch"))
                    add(
                        Preset(
                            name = obj.optString("name", patch.name),
                            category = PresetCategory.USER,
                            description = obj.optString("description", "User patch"),
                            patch = patch.copy(category = PresetCategory.USER),
                            isUser = true
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun saveUserPreset(name: String, patch: Patch) {
        val cleanName = name.trim().take(40).ifBlank { "Untitled" }
        val existing = loadUserPresets().filterNot { it.name.equals(cleanName, ignoreCase = true) }
        val updated = existing + Preset(
            name = cleanName,
            category = PresetCategory.USER,
            description = "User patch",
            patch = patch.copy(name = cleanName, category = PresetCategory.USER),
            isUser = true
        )
        val array = JSONArray()
        updated.forEach { preset ->
            array.put(
                JSONObject().apply {
                    put("name", preset.name)
                    put("description", preset.description)
                    put("patch", patchToJson(preset.patch))
                }
            )
        }
        prefs.edit().putString(KEY_USER_PRESETS, array.toString()).apply()
    }

    fun deleteUserPreset(name: String) {
        val remaining = loadUserPresets().filterNot { it.name == name }
        val array = JSONArray()
        remaining.forEach { preset ->
            array.put(
                JSONObject().apply {
                    put("name", preset.name)
                    put("description", preset.description)
                    put("patch", patchToJson(preset.patch))
                }
            )
        }
        prefs.edit().putString(KEY_USER_PRESETS, array.toString()).apply()
    }

    fun loadSettings(): AppSettings = AppSettings(
        haptics = prefs.getBoolean(KEY_HAPTICS, true),
        showKeyLabels = prefs.getBoolean(KEY_KEY_LABELS, true)
    )

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_HAPTICS, settings.haptics)
            .putBoolean(KEY_KEY_LABELS, settings.showKeyLabels)
            .apply()
    }

    private fun patchToJson(p: Patch): JSONObject = JSONObject().apply {
        put("name", p.name)
        put("category", p.category.name)
        put("description", p.description)
        put("osc1Range", p.osc1Range.name)
        put("osc1Wave", p.osc1Wave.name)
        put("osc1Level", p.osc1Level.toDouble())
        put("osc2Range", p.osc2Range.name)
        put("osc2Wave", p.osc2Wave.name)
        put("osc2Level", p.osc2Level.toDouble())
        put("osc2Tune", p.osc2Tune.toDouble())
        put("osc3Range", p.osc3Range.name)
        put("osc3Wave", p.osc3Wave.name)
        put("osc3Level", p.osc3Level.toDouble())
        put("osc3Tune", p.osc3Tune.toDouble())
        put("osc3ToFilter", p.osc3ToFilter)
        put("noiseLevel", p.noiseLevel.toDouble())
        put("drive", p.drive.toDouble())
        put("cutoff", p.cutoff.toDouble())
        put("emphasis", p.emphasis.toDouble())
        put("contourAmount", p.contourAmount.toDouble())
        put("keyTrack", p.keyTrack.name)
        put("filterAttack", p.filterAttack.toDouble())
        put("filterDecay", p.filterDecay.toDouble())
        put("filterSustain", p.filterSustain.toDouble())
        put("filterDecayOn", p.filterDecayOn)
        put("ampAttack", p.ampAttack.toDouble())
        put("ampDecay", p.ampDecay.toDouble())
        put("ampSustain", p.ampSustain.toDouble())
        put("ampRelease", p.ampRelease.toDouble())
        put("glide", p.glide.toDouble())
        put("modMix", p.modMix.toDouble())
        put("masterTune", p.masterTune.toDouble())
        put("masterVolume", p.masterVolume.toDouble())
        put("drift", p.drift.toDouble())
    }

    private fun patchFromJson(o: JSONObject): Patch = Patch(
        name = o.optString("name", "Init Patch"),
        category = enumOrDefault(o.optString("category"), PresetCategory.KEYS),
        description = o.optString("description", ""),
        osc1Range = enumOrDefault(o.optString("osc1Range"), OscRange.EIGHT),
        osc1Wave = enumOrDefault(o.optString("osc1Wave"), OscWave.SAWTOOTH),
        osc1Level = o.optDouble("osc1Level", 0.78).toFloat(),
        osc2Range = enumOrDefault(o.optString("osc2Range"), OscRange.EIGHT),
        osc2Wave = enumOrDefault(o.optString("osc2Wave"), OscWave.SAWTOOTH),
        osc2Level = o.optDouble("osc2Level", 0.55).toFloat(),
        osc2Tune = o.optDouble("osc2Tune", 0.02).toFloat(),
        osc3Range = enumOrDefault(o.optString("osc3Range"), OscRange.LO),
        osc3Wave = enumOrDefault(o.optString("osc3Wave"), OscWave.TRIANGLE),
        osc3Level = o.optDouble("osc3Level", 0.0).toFloat(),
        osc3Tune = o.optDouble("osc3Tune", 0.0).toFloat(),
        osc3ToFilter = o.optBoolean("osc3ToFilter", false),
        noiseLevel = o.optDouble("noiseLevel", 0.0).toFloat(),
        drive = o.optDouble("drive", 0.15).toFloat(),
        cutoff = o.optDouble("cutoff", 0.52).toFloat(),
        emphasis = o.optDouble("emphasis", 0.18).toFloat(),
        contourAmount = o.optDouble("contourAmount", 0.45).toFloat(),
        keyTrack = enumOrDefault(o.optString("keyTrack"), KeyTrack.TWO_THIRDS),
        filterAttack = o.optDouble("filterAttack", 0.02).toFloat(),
        filterDecay = o.optDouble("filterDecay", 0.45).toFloat(),
        filterSustain = o.optDouble("filterSustain", 0.2).toFloat(),
        filterDecayOn = o.optBoolean("filterDecayOn", true),
        ampAttack = o.optDouble("ampAttack", 0.01).toFloat(),
        ampDecay = o.optDouble("ampDecay", 0.4).toFloat(),
        ampSustain = o.optDouble("ampSustain", 0.75).toFloat(),
        ampRelease = o.optDouble("ampRelease", 0.18).toFloat(),
        glide = o.optDouble("glide", 0.06).toFloat(),
        modMix = o.optDouble("modMix", 0.0).toFloat(),
        masterTune = o.optDouble("masterTune", 0.0).toFloat(),
        masterVolume = o.optDouble("masterVolume", 0.78).toFloat(),
        drift = o.optDouble("drift", 0.35).toFloat()
    )

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String, fallback: T): T =
        runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)

    private companion object {
        const val KEY_LAST_PATCH = "last_patch"
        const val KEY_USER_PRESETS = "user_presets"
        const val KEY_HAPTICS = "haptics"
        const val KEY_KEY_LABELS = "key_labels"
        const val KEY_SEEN_INTRO = "seen_intro"
    }
}
