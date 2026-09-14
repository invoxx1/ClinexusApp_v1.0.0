# Retrofit service methods are discovered from runtime annotations.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keep interface com.example.clinexusapp.api.** { *; }

# Gson DTO field names are supplied by @SerializedName. Keep generic signatures and
# model constructors for conservative compatibility with existing API responses.
-keep class com.example.clinexusapp.model.** { *; }

# Keep Firebase messaging service entry points referenced by the manifest.
-keep class com.example.clinexusapp.data.service.MyFirebaseMessagingService { *; }
