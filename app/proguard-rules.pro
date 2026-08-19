# R8 keep rules for Ibex release builds.
# Library reflection is covered by consumer rules bundled in the AARs/JARs:
#   jcifs-ng (via azmaree-source-smb), Tink protobuf, WorkManager, Coil, PostHog, Sentry.
# Only rules R8 cannot infer on its own live here. Keep this list tight: broad
# `-keep ... { *; }` rules are exactly what the R8 Configuration Analyzer flags.

# --- kotlinx.serialization -------------------------------------------------
# The library ships no consumer rules, so the generated serializers must be kept
# by hand. These are the official rules from the kotlinx.serialization README,
# scoped to @Serializable types only (not a blanket package keep).
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Keep the Companion of every @Serializable class so serializer() resolves.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
# Keep serializer() on those Companions.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep serializer() on @Serializable objects (INSTANCE-style singletons).
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Room (transitive via WorkManager) -------------------------------------
# Room instantiates its generated *_Impl database via a reflective no-arg call.
# Room 2.6.1 ships `-keep class * extends RoomDatabase` with no member spec, and
# R8 full mode drops the constructor anyway, so WorkManager's WorkDatabase_Impl
# crashes on startup. Keep the no-arg constructor explicitly until Room >= 2.7.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# The fully-merged R8 configuration the analyzer grades is emitted automatically
# by AGP at build/outputs/mapping/<variant>/configuration.txt.
