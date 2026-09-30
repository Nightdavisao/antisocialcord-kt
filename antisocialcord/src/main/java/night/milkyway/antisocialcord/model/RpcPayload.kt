package night.milkyway.antisocialcord.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import night.milkyway.antisocialcord.internal.utils.EnumIntSerializer

@Serializable
data class RpcPayload(
    @SerialName("cmd")
    val command: RpcCommand,
    val nonce: String? = null,
    @SerialName("evt")
    val event: RpcEvent? = null,
    val args: PayloadArguments? = null
)

@Serializable
enum class RpcEvent {
    READY
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("#type")
sealed class PayloadArguments

@Serializable
object EmptyPayload: PayloadArguments()

@Serializable
data class ActivityArguments(
    val pid: Int = 1,
    val activity: Activity
): PayloadArguments()

@Serializable
data class Activity(
    @SerialName("application_id")
    @EncodeDefault(Mode.NEVER)
    val applicationId: String? = null,
    val name: String,
    @EncodeDefault(Mode.NEVER)
    val state: String? = null,
    @EncodeDefault(Mode.NEVER)
    @SerialName("state_url")
    val stateUrl: String? = null,
    @EncodeDefault(Mode.NEVER)
    val details: String? = null,
    @EncodeDefault(Mode.NEVER)
    @SerialName("session_id")
    val sessionId: String? = null,
    @EncodeDefault(Mode.NEVER)
    @SerialName("details_url")
    val detailsUrl: String? = null,
    @SerialName("type")
    val activityType: ActivityType,
    @EncodeDefault(Mode.NEVER)
    @SerialName("status_display_type")
    val statusDisplayType: StatusDisplayType? = null,
    @EncodeDefault(Mode.NEVER)
    val timestamps: ActivityTimestamps? = null,
    @EncodeDefault(Mode.NEVER)
    val assets: ActivityAssets? = null
)

internal class StatusDisplayTypeSerializer : EnumIntSerializer<StatusDisplayType>(StatusDisplayType.entries)

@Serializable(with = StatusDisplayTypeSerializer::class)
enum class StatusDisplayType(val value: Int) {
    NAME(0),
    STATE(1),
    DETAILS(2)
}

@Serializable
data class ActivityAssets(
    @SerialName("large_image")
    val largeImage: String?,
    @SerialName("large_text")
    val largeText: String?,
    @SerialName("large_url")
    val largeUrl: String?,
    @SerialName("small_image")
    val smallImage: String?,
    @SerialName("small_text")
    val smallText: String?,
    @SerialName("small_url")
    val smallUrl: String?
)

@Serializable
data class ActivityTimestamps(
    val start: Long,
    val end: Long
)

internal class ActivityTypeSerializer : EnumIntSerializer<ActivityType>(ActivityType.entries)


@Serializable(with = ActivityTypeSerializer::class)
enum class ActivityType(val value: Int) {
    PLAYING(0),
    STREAMING(1),
    LISTENING(2),
    WATCHING(3),
    CUSTOM_STATUS(4),
    COMPETING(5),
    HANG_STATUS(6)

}

internal class ActivityGamePlatformsSerializer : EnumIntSerializer<ActivityGamePlatforms>(ActivityGamePlatforms.entries)


@Serializable(with = ActivityGamePlatformsSerializer::class)
enum class ActivityGamePlatforms(val value: Byte) {
    DESKTOP(0x1),
    XBOX(0X2),
    SAMSUNG(0X4),
    IOS(0x8),
    ANDROID(0x10),
    EMBEDDED(0x20),
    PS4(0x40),
    PS5(0x80.toByte())
}

@Serializable
enum class RpcCommand {
    DISPATCH,
    SET_ACTIVITY
}