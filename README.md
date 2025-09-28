# Telemetry Lab
# Demo video: https://github.com/tushar-nb/Telemetry-Lab-Performance-app/blob/main/telemetryDemo.mp4

A performance monitoring Android app that simulates edge compute workloads while tracking UI jank and system metrics.

## Architecture Overview

### Threading & Backpressure Approach

**Background Processing:**
- Uses `Dispatchers.Default` for CPU-intensive 2D convolution operations
- Maintains 20Hz frame rate (10Hz in power save mode) using coroutine delays
- Each "frame" performs N convolution passes on a 256×256 float array where N = compute load
- Fixed 3×3 edge detection kernel applied repeatedly

**UI Thread Protection:**
- All compute work runs on background threads via `withContext(Dispatchers.Default)`
- UI updates limited to summaries only (frame latency, averages, jank metrics)
- Uses `mutableStateOf` and Compose's recomposition system for efficient UI updates
- Scrolling list updates at controlled rate (20Hz) to show visible UI work

**Backpressure Handling:**
- Frame processing maintains target rate regardless of compute load
- If processing takes longer than frame budget, next frame starts immediately

### Service Choice: Foreground Service vs WorkManager

**Chosen: Foreground Service with `dataSync` type**

**Rationale:**
- **Real-time requirement:** App needs consistent 20Hz frame generation with minimal latency variation
- **User-initiated:** Start/stop controlled directly by user interaction, not scheduled work
- **Long-running:** Potentially runs for extended periods during performance testing
- **Notification required:** User needs clear indication of background processing

**FGS Type Selection - `dataSync`:**
- Chosen over `connectedDevice` because we're processing computational data streams
- Fits the pattern of continuous data processing and telemetry collection
- Aligns with Android 14's stricter FGS policies for performance monitoring use cases

**Why not WorkManager:**
- WorkManager is designed for deferrable, guaranteed work execution
- Our use case requires immediate start/stop control and real-time processing
- Battery optimizations in WorkManager would interfere with precise timing requirements
- Foreground service provides better user control and visibility

### Power Management Integration

**Battery Saver Detection:**
- Registers `PowerManager.ACTION_POWER_SAVE_MODE_CHANGED` receiver
- Automatically reduces frame rate from 20Hz → 10Hz
- Decreases compute load by 1 level (minimum 1)
- Shows orange "Power-save mode" banner in UI

**Adaptive Behavior:**
- Maintains performance targets while respecting system power policies
- Provides user feedback about performance mode changes

### JankStats Integration

**Implementation:**
- Integrated in `MainActivity.onCreate()` with window-level tracking
- Tracks frames >16.67ms as jank (60fps threshold)
- Maintains 30-second rolling window of frame metrics
- Displays jank percentage and count in real-time dashboard

### Key Features

1. **Real-time Dashboard**: Frame latency, moving averages, jank metrics
2. **Adaptive Power Management**: Automatic performance scaling
3. **Background Compute**: Realistic CPU load simulation
4. **System Integration**: Proper foreground service with notifications
5. **Performance Monitoring**: JankStats integration with rolling metrics
