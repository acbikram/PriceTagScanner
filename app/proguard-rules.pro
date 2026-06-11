# Keep ML Kit barcode classes
-keep class com.google.mlkit.** { *; }
# Keep Room entities
-keep class com.pricetag.scanner.data.db.entity.** { *; }
# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
# Keep CameraX
-keep class androidx.camera.** { *; }
# Keep DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }
