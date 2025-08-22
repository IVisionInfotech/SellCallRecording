package com.sellcallrecording.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent


class CallRecordingAccessibilityService : AccessibilityService() {


    override fun onAccessibilityEvent(event: AccessibilityEvent) {
    }


    override fun onInterrupt() {
    }

}