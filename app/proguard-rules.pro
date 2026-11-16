# kotlinx.serialization ships its own consumer rules; keep the model classes' serializers.
-keep,includedescriptorclasses class com.robert.courtcompanion.model.**$$serializer { *; }
-keepclassmembers class com.robert.courtcompanion.model.** {
    *** Companion;
}
