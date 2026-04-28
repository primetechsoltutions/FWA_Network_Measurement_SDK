# Network Measurement SDK — Integration & Best Practices Guide

This document provides a comprehensive guide for integrating the Network Measurement SDK into host applications (such as MyBL). It covers setup, initialization, manifest configurations, and common troubleshooting steps to ensure a crash-free experience.

---

## 1. Installation & Dependency

Ensure the SDK module is added to your project's `settings.gradle` and your app's `build.gradle` file:

```gradle
// app/build.gradle
implementation project(':network-sdk')
```

---

## 2. Manifest Configuration & WorkManager

The SDK uses Android's modern `androidx.startup.InitializationProvider` to automatically configure `WorkManager`.

### ⚠️ Critical Warning: Custom WorkManager Init
If your host application **customizes** WorkManager initialization by removing the default initializer like this:

```xml
<!-- In your host app's AndroidManifest.xml -->
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    tools:node="remove" />  <!-- REMOVES AUTO INIT -->
```

**What happens?** The SDK is designed to be safe! If the host app removes auto-initialization, the SDK will automatically fallback to initializing `WorkManager` with a default configuration to prevent `IllegalStateException` crashes.

**Best Practice:** If you use `tools:node="remove"`, ensure your host app initializes `WorkManager` in your `Application` class by implementing `Configuration.Provider`:

```kotlin
class MyHostApplication : Application(), Configuration.Provider {
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }
}
```
*(If you do this, the SDK will seamlessly use your custom configuration.)*

---

## 3. SDK Initialization

The SDK **must** be initialized before triggering any network captures. Because the SDK manages permissions safely, it must be bound to the lifecycle of an Activity or Fragment.

### Initialization Rules:
1. **Where to call:** You MUST call `NetworkDataUploader().init(...)` inside `onCreate()` of your Activity or Fragment.
2. **Why?** The SDK uses `ActivityResultContracts` for permissions, which the Android framework requires to be registered *before* the component reaches the `STARTED` state.

### ✅ Correct Usage (Fragment Example)
```kotlin
class HomeFragment : Fragment() {
    
    // 1. Declare the uploader globally
    private val networkDataUploader = NetworkDataUploader()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 2. Initialize in onCreate()
        networkDataUploader.init(
            fragment = this, 
            applicationName = "MyBL_App"
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        button.setOnClickListener {
            // 3. Trigger capture later
            startCapture()
        }
    }
}
```

### ❌ Incorrect Usage (Will cause crashes)
```kotlin
// BAD: Initializing inside a click listener will crash!
button.setOnClickListener {
    val uploader = NetworkDataUploader()
    uploader.init(this, "App") // CRASH: IllegalStateException (registered after STARTED)
    uploader.startUploading(...)
}
```

---

## 4. Triggering Network Captures

The SDK supports two types of captures:
1. `UploadType.NetworkDataCapture` (Standard background capture, runs via WorkManager)
2. `UploadType.FTPNetworkDataCapture` (Deep diagnostic with FTP speed test, runs immediately)

```kotlin
private fun startCapture() {
    networkDataUploader.startUploading(
        msisdn = "01912345678",
        integratedAppVersion = "1.0.0",
        sdkInitiateTimeStamp = System.currentTimeMillis().toString(),
        integratedAppEventName = "Home_Screen_Load",
        uploadType = UploadType.FTPNetworkDataCapture
    ) { success: Boolean, status: UploadStatus ->
        
        if (success) {
            Log.d("SDK", "Capture successful: ${status.response}")
        } else {
            Log.e("SDK", "Capture failed: ${status.response}")
        }
    }
}
```

### 📋 Callback Response Formats

The `status.response` string is a serialized JSON object of the `NetworkDataResponse` class. The format differs depending on the `UploadType` you requested and whether it succeeded or failed.

#### 1. Standard Capture (`UploadType.NetworkDataCapture`)
Standard captures run asynchronously in the background via WorkManager. The callback only indicates whether the task was successfully **enqueued**, it does not wait for the actual network test to finish.

**✅ Success Response:**
```json
{
  "message": "SDK Task enqueued"
}
```

**❌ Error Response (e.g., SDK not initialized):**
```json
{
  "status": "Failed",
  "statusCode": 500,
  "message": "SDK not initialized"
}
```

#### 2. Deep Diagnostic Capture (`UploadType.FTPNetworkDataCapture`)
FTP captures run immediately and the callback waits up to 60 seconds for the entire diagnostic test to complete.

**✅ Success Response:**
```json
{
  "status": "Success",
  "testResult": "Green", 
  "statusCode": 200,
  "message": "Assessment completed successfully",
  "data": {
    "assessmentId": 123456789,
    "networkData": {
      "RSRP": -85,
      "SNR": 15,
      "RSRQ": -12
    },
    "cellInfo": {
      "cellName": "Dhaka_North_Cell_A",
      "eNodeBName": "eNodeB_Banani",
      "nbhDlThroughputMbps": 45.5,
      "nbhTrafficGB": 12.3
    },
    "speedPair": {
      "ulSpeedKbps": 15000.0,
      "dlSpeedKbps": 45000.0
    },
    "userInfo": {
      "deviceManufacture": "Samsung",
      "deviceModel": "SM-G998B",
      "deviceOsVersion": "33",
      "latitude": 23.8103,
      "longitude": 90.4125,
      "msisdn": "01912345678"
    }
  }
}
```

**❌ Error Response (e.g., test timeout or no cell data found):**
```json
{
  "status": "Failed",
  "statusCode": 500,
  "message": "Assessment Failed"
}
```

---

## 5. Troubleshooting & Crash Prevention

### "SDK not initialized" Error in Callback
If your callback immediately receives `status = "Failed", message = "SDK not initialized"`, it means:
- You called `startUploading` but forgot to call `init()` first.
- The internal `SdkContainer` failed to create the local database (often due to out-of-memory or corrupt storage).

### "Fragment not attached" Warnings
The SDK internally uses `WeakReference` to hold the Fragment/Activity. If the user navigates away from the screen while a 60-second FTP test is running, the SDK will safely detect the detached state and cancel UI updates to prevent `IllegalStateException`. You do not need to manually cancel the SDK on navigation.

### Permission Dialog Not Showing
If the user previously clicked "Don't ask again", the SDK will not be able to show the system dialog. The SDK's `CheckPermissionHandler` handles this gracefully and will proceed (without location data) or fail the FTP test cleanly based on the requested `UploadType`.

---

## 📞 Support & Integration Sync

If you encounter any architectural conflicts, unexpected crashes, or have questions about how the SDK handles WorkManager queues and threading:

> **Note:** Please feel free to arrange a sync meeting with the SDK Development Team. We are happy to walk through the integration via screen share or review your host app's implementation PR to ensure perfect stability.
