-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod

-keep class com.pay2own.Pay2OwnDeviceAdminReceiver { *; }
-keep class com.pay2own.GetProvisioningModeActivity { *; }
-keep class com.pay2own.AdminPolicyComplianceActivity { *; }
-keep class com.pay2own.restrictions.LockHomeActivity { *; }

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}

-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn okhttp3.**
-dontwarn retrofit2.**
