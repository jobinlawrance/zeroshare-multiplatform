package live.jkbx.zeroshare.utils

import io.github.vinceglb.filekit.core.PlatformFile
import live.jkbx.zeroshare.socket.FileTransferMetadata
import kotlin.math.round
import kotlin.math.roundToInt

val compressedFileMimeTypes = listOf(
    "application/zip",                // ZIP Archive
    "application/x-rar-compressed",  // RAR Archive
    "application/gzip",              // GZIP Archive
    "application/x-7z-compressed",   // 7z Archive
    "application/x-tar",             // TAR Archive
    "application/x-gtar-compressed", // TAR.GZ Archive (alternative)
    "application/x-bzip2",           // BZIP2 Archive
    "application/x-xz",              // XZ Archive
    "application/x-compress",        // Z Archive
    "application/x-ms-wim"           // WIM Archive
)

val documentMimeTypes = listOf(
    "application/pdf",                  // PDF Document
    "application/msword",               // Microsoft Word Document (.doc)
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // Microsoft Word Document (.docx)
    "application/vnd.ms-excel",         // Microsoft Excel Document (.xls)
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // Microsoft Excel Document (.xlsx)
    "application/vnd.ms-powerpoint",    // Microsoft PowerPoint Document (.ppt)
    "application/vnd.openxmlformats-officedocument.presentationml.presentation", // Microsoft PowerPoint Document (.pptx)
    "application/rtf",                  // Rich Text Format (.rtf)
    "text/plain",                       // Plain Text (.txt)
    "text/csv",                         // CSV File (.csv)
    "application/vnd.oasis.opendocument.text",        // OpenDocument Text (.odt)
    "application/vnd.oasis.opendocument.spreadsheet", // OpenDocument Spreadsheet (.ods)
    "application/vnd.oasis.opendocument.presentation" // OpenDocument Presentation (.odp)
)

val applicationMimeTypes = listOf(
    "application/x-apple-diskimage",   // DMG (macOS Disk Image)
    "application/octet-stream",        // Binary Files
    "application/vnd.android.package-archive", // APK (Android Package)
    "application/x-msdownload",        // EXE (Windows Executable)
    "application/x-debian-package",    // DEB (Debian Package)
    "application/x-rpm",               // RPM (Red Hat Package Manager)
    "application/x-sharedlib",         // Linux Shared Library
    "application/java-archive",        // JAR (Java Archive)
    "application/x-ms-dos-executable", // DOS Executable
    "application/x-elf",               // ELF (Executable and Linkable Format)
    "application/x-pkcs12",            // PKCS12 File (e.g., PFX)
    "application/vnd.microsoft.portable-executable", // PE (Windows Portable Executable)
    "application/x-apk",               // Alternative for APK
    "application/x-dosexec",           // MS-DOS Executable
    "application/x-appimage",          // AppImage (Linux)
    "application/x-iso9660-image"      // ISO Image
)

fun Long.convertByteSize(): String {
    val units = arrayOf("Bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
    var size = this.toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    return "${size.roundToInt()} ${units[unitIndex]}"
}

val codeRelatedMimeTypes = listOf(
    // Web Development MIME Types
    "text/html",               // HTML files
    "text/css",                // CSS files
    "application/javascript",  // JavaScript files
    "application/json",        // JSON files
    "application/xml",         // XML files
    "text/xml",                // XML files (alternative)
    "text/markdown",           // Markdown files
    "application/x-www-form-urlencoded", // Form-encoded data

    // Programming Code MIME Types
    "text/x-java-source",      // Java source code
    "text/x-c",                // C source code
    "text/x-c++src",           // C++ source code
    "text/x-csharp",           // C# source code
    "application/x-python",    // Python scripts
    "application/x-ruby",      // Ruby scripts
    "text/x-go",               // Go source code
    "text/x-kotlin",           // Kotlin source code
    "text/x-scala",            // Scala source code
    "application/x-php",       // PHP code
    "application/x-shellscript", // Shell scripts
    "text/x-sql",              // SQL scripts
    "application/x-perl",      // Perl scripts
    "application/x-lua",       // Lua scripts
    "text/x-rust",             // Rust source code
    "text/x-swift",            // Swift source code
    "application/x-tcl",       // Tcl scripts
    "application/x-elixir",    // Elixir source code

    // Markup and Configuration MIME Types
    "application/xhtml+xml",   // XHTML files
    "text/x-properties",       // Java properties files
    "text/x-ini",              // INI configuration files
    "text/x-yaml",             // YAML configuration files
    "text/x-toml"              // TOML configuration files
)

val databaseMimeTypes = listOf(
    "application/vnd.sqlite3",      // SQLite database
    "application/x-sqlite3",        // Alternative SQLite database
    "application/x-msaccess",       // Microsoft Access database
    "application/vnd.ms-access",    // Alternative for Microsoft Access database
    "application/vnd.mysql.dump",   // MySQL database dump
    "application/vnd.postgresql",   // PostgreSQL database
    "application/x-sql",            // SQL files
    "application/json",             // JSON-based NoSQL (MongoDB, etc.)
    "application/geo+json",         // GeoJSON for spatial databases
    "application/csv",              // CSV files (commonly used for database import/export)
    "text/csv",                     // CSV text files
    "application/tab-separated-values", // TSV files (Tab-separated values)
    "application/xml",              // XML database files
    "application/x-db",             // Generic database file
    "application/dbase",            // dBase database
    "application/x-dbf"             // dBase database files
)

val excelMimeTypes = listOf(
    "application/vnd.ms-excel",                     // Microsoft Excel (xls)
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // Microsoft Excel (xlsx)
    "application/vnd.oasis.opendocument.spreadsheet", // OpenDocument Spreadsheet (ods)
    "application/x-ms-excel",                       // Legacy Microsoft Excel MIME type
    "application/vnd.apple.numbers",               // Apple Numbers spreadsheet
    "application/x-quattropro",                    // Quattro Pro spreadsheet
    "application/wps-office.xls",                  // WPS Office spreadsheet
    "application/wps-office.xlsx"                  // WPS Office Excel format
)

expect fun PlatformFile.toFileMetaData(): FileTransferMetadata

expect fun getKoinContext(): Any