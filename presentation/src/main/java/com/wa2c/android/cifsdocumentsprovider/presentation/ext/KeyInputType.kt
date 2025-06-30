package com.samsung.cifs.ui.ext

/**
 * Key input type
 */
enum class KeyInputType {
    /** Not used */
    NOT_USED,
    /** External file */
    EXTERNAL_FILE,
    /** Import file */
    IMPORTED_FILE,
    /** Input text */
    INPUT_TEXT,
    ;

    /** Index */
    val index: Int = this.ordinal
}
