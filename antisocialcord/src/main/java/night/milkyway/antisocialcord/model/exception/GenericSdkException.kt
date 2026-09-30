package night.milkyway.antisocialcord.model.exception

/**
 * @param message Reason for the exception
 * @param code Generally any of the error codes specified in the constants
 * @see night.milkyway.antisocialcord.internal.utils.SocialSdkConsts
 */
class GenericSdkException(
    message: String,
    code: Int? = null
) : Exception("$code - $message")