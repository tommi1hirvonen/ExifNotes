package com.tommihirvonen.exifnotes.datastructures

/**
 * Helper class to handle management of links between different pieces of gear,
 * e.g. links between cameras and lenses as well as lenses and filters etc.
 *
 * @property gear the piece of gear to which the link is being monitored and managed
 * @property beforeState the link/mountable state before any changes were saved
 * @property afterState the link/mountable state that should exists after saving
 */
class MountableState(
        val gear: Gear,
        val beforeState: Boolean,
        var afterState: Boolean = beforeState
)