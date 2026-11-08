# Compose, Material 3 and AndroidX ship their own consumer rules.
# The synth engine is pure Kotlin and referenced directly, so no reflection rules
# are required. Keep the demo/test entry points free of obfuscation issues.
-dontwarn org.jetbrains.annotations.**
