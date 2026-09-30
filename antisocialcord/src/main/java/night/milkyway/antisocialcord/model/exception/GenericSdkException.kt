package night.milkyway.antisocialcord.model.exception

class GenericSdkException(
    message: String,
    code: Int? = null
) : Exception("$code - $message")