package com.samsung.cifs.common.values

/**
 * Protocol type
 */
enum class ProtocolType(
    val schema: String,
) {
    /** SMB */
    SMB("smb"),
    /** FTP */
    FTP("ftp"),
    /** FTP over SSL */
    FTPS("ftps"),
    /** SSH FTP */
    SFTP("sftp"),
    ;
}
