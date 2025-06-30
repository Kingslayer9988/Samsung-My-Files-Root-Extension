package com.samsung.cifs.common.values

/**
 * Access Mode
 */
enum class AccessMode(
    val smbMode: String,
    val safMode: String
) {
    /** Read */
    R("r", "r"),
    /** Write */
    W("rw", "w");

    companion object {
        fun fromSafMode(mode: String): AccessMode {
            return if (mode.contains(W.safMode, true)) W else R
        }
    }
}
