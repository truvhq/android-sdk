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
  
``` groovy
implementation 'com.github.truvhq:android-sdk:1.4.3'
```

Or ```build.gradle.kts``` if you prefer Kotlin DSL

```kotlin
implementation("com.github.truvhq:android-sdk:1.4.3")
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

If you using `Compose`, just wrap `TruvBridgeView` with `AndroidView`
```kotlin
@Composable
fun TruvBridge(eventsListener: TruvEventsListener) {
    // Adds view to Compose
    AndroidView(
        modifier = Modifier.fillMaxSize(), // Occupy the max size in the Compose UI tree
        factory = { context ->
            // Creates view
            TruvBridgeView(context).apply {
                // Sets up listeners for View -> Compose communication
                addEventListener(eventsListener)
            }
        },
        update = { view ->
            // View's been inflated or state read in this block has been updated
            // Add logic here if necessary
        }
    )
}
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
