## --------- Preserve SDK Namespace ---------
## Ensure classes remain in com.ptsl.fwa_network_sdk.* namespace
#-keeppackagenames com.ptsl.fwa_network_sdk.**
#-repackageclasses 'com.ptsl.fwa_network_sdk'
## --------- General SDK Protection ---------
#-keep class com.ptsl.fwa_network_sdk.** { *; }
#-dontwarn com.ptsl.fwa_network_sdk.**
#
## --------- Retrofit & OkHttp ---------
#-keepattributes Signature
#-keepattributes *Annotation*
#-keepclasseswithmembers class * {
#    @retrofit2.* <methods>;
#}
#-keep class retrofit2.** { *; }
#-dontwarn retrofit2.**
#-dontwarn okhttp3.**
#-dontwarn okio.**
#
## --------- Gson ---------
#-keep class com.google.gson.** { *; }
#-keep class * {
#  @com.google.gson.annotations.SerializedName <fields>;
#}
#-dontwarn com.google.gson.**
#
## --------- Room ---------
#-keepclassmembers class * {
#    @androidx.room.* <methods>;
#}
#-keep class androidx.room.** { *; }
#-dontwarn androidx.room.**
#
## --------- Hilt / Dagger ---------
#-keep class dagger.** { *; }
#-dontwarn dagger.**
#-keep class javax.inject.** { *; }
#-dontwarn javax.inject.**
#-keep class dagger.hilt.** { *; }
#-dontwarn dagger.hilt.**
#-keep class androidx.hilt.** { *; }
#
## --------- WorkManager ---------
#-keep class androidx.work.** { *; }
#-dontwarn androidx.work.**
#
## --------- Optional TLS Providers ---------
#-dontwarn org.conscrypt.**
#-keep class org.conscrypt.** { *; }
#
#-dontwarn org.bouncycastle.**
#-keep class org.bouncycastle.** { *; }
#
#-dontwarn org.openjsse.**
#-keep class org.openjsse.** { *; }
#
## --------- Android Reflection Safety ---------
#-keepclassmembers class * {
#    public <init>(...);
#}
#-keepclassmembers class * {
#    *** *(...);
#}
#
#
#-dontwarn java.lang.invoke.StringConcatFactory
#
#
##
### Please add these rules to your existing keep rules in order to suppress warnings.
### This is generated automatically by the Android Gradle plugin.
##-dontwarn com.android.Version
##-dontwarn com.android.build.api.instrumentation.AsmClassVisitorFactory
##-dontwarn com.android.build.api.instrumentation.ClassContext
##-dontwarn com.android.build.api.instrumentation.ClassData
##-dontwarn com.android.build.api.instrumentation.FramesComputationMode
##-dontwarn com.android.build.api.instrumentation.InstrumentationContext
##-dontwarn com.android.build.api.instrumentation.InstrumentationParameters$None
##-dontwarn com.android.build.api.instrumentation.InstrumentationScope
##-dontwarn com.android.build.api.transform.Context
##-dontwarn com.android.build.api.transform.DirectoryInput
##-dontwarn com.android.build.api.transform.Format
##-dontwarn com.android.build.api.transform.JarInput
##-dontwarn com.android.build.api.transform.QualifiedContent$DefaultContentType
##-dontwarn com.android.build.api.transform.QualifiedContent$Scope
##-dontwarn com.android.build.api.transform.QualifiedContent
##-dontwarn com.android.build.api.transform.Status
##-dontwarn com.android.build.api.transform.Transform
##-dontwarn com.android.build.api.transform.TransformInput
##-dontwarn com.android.build.api.transform.TransformInvocation
##-dontwarn com.android.build.api.transform.TransformOutputProvider
##-dontwarn com.android.build.api.variant.AndroidComponentsExtension$DefaultImpls
##-dontwarn com.android.build.api.variant.AndroidComponentsExtension
##-dontwarn com.android.build.api.variant.ApplicationAndroidComponentsExtension
##-dontwarn com.android.build.api.variant.ApplicationVariant
##-dontwarn com.android.build.api.variant.DynamicFeatureAndroidComponentsExtension
##-dontwarn com.android.build.api.variant.DynamicFeatureVariant
##-dontwarn com.android.build.api.variant.GeneratesApk
##-dontwarn com.android.build.api.variant.Instrumentation
##-dontwarn com.android.build.api.variant.SourceDirectories$Flat
##-dontwarn com.android.build.api.variant.SourceDirectories$Layered
##-dontwarn com.android.build.api.variant.Sources
##-dontwarn com.android.build.api.variant.Variant
##-dontwarn com.android.build.api.variant.VariantInfo
##-dontwarn com.android.build.api.variant.VariantSelector
##-dontwarn com.android.build.gradle.AppExtension
##-dontwarn com.android.build.gradle.api.ApplicationVariant
##-dontwarn com.android.build.gradle.internal.dsl.BuildType
##-dontwarn com.android.build.gradle.internal.dsl.ProductFlavor
##-dontwarn com.android.builder.model.BuildType
##-dontwarn com.android.builder.model.ProductFlavor
##-dontwarn com.android.ide.common.repository.GradleVersion
##-dontwarn com.android.utils.FileUtils
##-dontwarn com.deenislam.sdk.MainDirections$ActionGlobalKhatamEQuranHomeFragment
##-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
##-dontwarn java.lang.management.ManagementFactory
##-dontwarn java.lang.management.RuntimeMXBean
##-dontwarn org.apache.commons.io.FileUtils
##-dontwarn org.apache.commons.io.IOUtils
##-dontwarn org.gradle.api.Action
##-dontwarn org.gradle.api.DefaultTask
##-dontwarn org.gradle.api.DomainObjectSet
##-dontwarn org.gradle.api.GradleException
##-dontwarn org.gradle.api.NamedDomainObjectContainer
##-dontwarn org.gradle.api.Plugin
##-dontwarn org.gradle.api.Project
##-dontwarn org.gradle.api.artifacts.Configuration
##-dontwarn org.gradle.api.artifacts.ConfigurationContainer
##-dontwarn org.gradle.api.artifacts.DependencyResolutionListener
##-dontwarn org.gradle.api.artifacts.ResolvableDependencies
##-dontwarn org.gradle.api.artifacts.VersionConstraint
##-dontwarn org.gradle.api.artifacts.component.ComponentIdentifier
##-dontwarn org.gradle.api.artifacts.component.ComponentSelector
##-dontwarn org.gradle.api.artifacts.component.ModuleComponentSelector
##-dontwarn org.gradle.api.artifacts.result.DependencyResult
##-dontwarn org.gradle.api.artifacts.result.ResolutionResult
##-dontwarn org.gradle.api.artifacts.result.ResolvedComponentResult
##-dontwarn org.gradle.api.file.Directory
##-dontwarn org.gradle.api.file.DirectoryProperty
##-dontwarn org.gradle.api.file.ProjectLayout
##-dontwarn org.gradle.api.file.RegularFile
##-dontwarn org.gradle.api.file.RegularFileProperty
##-dontwarn org.gradle.api.logging.Logger
##-dontwarn org.gradle.api.plugins.AppliedPlugin
##-dontwarn org.gradle.api.plugins.ExtensionAware
##-dontwarn org.gradle.api.plugins.ExtensionContainer
##-dontwarn org.gradle.api.plugins.PluginManager
##-dontwarn org.gradle.api.provider.Property
##-dontwarn org.gradle.api.provider.Provider
##-dontwarn org.gradle.api.tasks.CacheableTask
##-dontwarn org.gradle.api.tasks.Input
##-dontwarn org.gradle.api.tasks.InputFiles
##-dontwarn org.gradle.api.tasks.Internal
##-dontwarn org.gradle.api.tasks.OutputDirectory
##-dontwarn org.gradle.api.tasks.OutputFile
##-dontwarn org.gradle.api.tasks.PathSensitive
##-dontwarn org.gradle.api.tasks.PathSensitivity
##-dontwarn org.gradle.api.tasks.TaskAction
##-dontwarn org.gradle.api.tasks.TaskContainer
##-dontwarn org.gradle.api.tasks.TaskProvider
##-dontwarn org.gradle.configurationcache.extensions.CharSequenceExtensionsKt
