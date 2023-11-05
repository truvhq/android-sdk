# TruvSDK

## Requirements

### Installation

**Step 1**. Add the JitPack repository to your project ```build.gradle``` file

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2**. Add the dependency to your ```build.gradle``` file:
  
```
implementation 'com.github.truvhq:android-sdk:1.1.2'
```

The TruvSDK is available via JitPack [![](https://jitpack.io/v/truvhq/android-sdk.svg)](https://jitpack.io/#truvhq/android-sdk)

### TruvBridgeView

The TruvBridgeView is a `View` that you can integrate into your app's flow like so:

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.truv.TruvBridgeView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/bridgeView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

```kotlin
val truvEventsListener = object : TruvEventsListener {

        override fun onClose() {
            Log.d(TAG, "Bridge Closed")
        }

        override fun onError() {
            Log.e(TAG, "Bridge Error")
        }

        override fun onEvent(event: TruvEventPayload.EventType) {
            Log.d(TAG, "Event: $event")
        }

        override fun onLoad() {
            Log.d(TAG, "Bridge Loaded")
        }

        override fun onSuccess(payload: TruvSuccessPayload) {
            Log.d(TAG, "Bridge Success")
            val token = payload.publicToken
            // Do something with your token
        }

    }

binding.bridgeView.addEventListener(truvEventsListener)
```

With the `TruvBridgeView`, end-users can select their employer, authenticate with their payroll platform login credentials, and authorize the direct deposit change. Throughout the process, different events will be emitted to the `TruvEventsListener` interface.

## TruvEventsListener

The `TruvEventsListener` protocol is set up such that every event goes through it.   
See the [events page](https://docs.truv.com/docs/events) of the documentation for more details.
