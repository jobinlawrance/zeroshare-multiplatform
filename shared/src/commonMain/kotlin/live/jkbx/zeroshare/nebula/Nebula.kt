package live.jkbx.zeroshare.nebula

interface Nebula {
    fun generateKeyPair(): Key
    fun parseCert(cert: String): Result<String>
    fun verifyCertAndKey(cert: String, key: String): Result<Boolean>
}