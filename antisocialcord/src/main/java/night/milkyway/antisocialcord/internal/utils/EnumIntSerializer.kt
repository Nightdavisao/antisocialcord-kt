package night.milkyway.antisocialcord.internal.utils

import kotlin.enums.EnumEntries
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal open class EnumIntSerializer<T : Enum<T>>(
    private val entries: EnumEntries<T>
) : KSerializer<T> {

    private val entriesByName = entries.associateBy { it.name }
    private val namesByValue = mutableMapOf<Int, String>()
    private val valuesByName = mutableMapOf<String, Int>()
    private val clazz = entries[0]::class.java

    init {
        for ((i, entry) in entries.withIndex()) {
            namesByValue[i] = entry.name
            valuesByName[entry.name] = i
        }

        for (field in clazz.fields) {
            val fieldType = field.type
            if (fieldType != clazz) continue
            val annotation = field.getAnnotation(SerialName::class.java) ?: continue
            val value = annotation.value.toInt()
            namesByValue[value] = field.name
            valuesByName[field.name] = value
        }
    }

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(clazz.toString(), PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: T) {
        val index = entries.indexOf(value)
        if (index == -1) throw error(value)
        val name = value.name
        val valueInt = valuesByName[name] ?: throw error(value)
        encoder.encodeInt(valueInt)
    }

    override fun deserialize(decoder: Decoder): T {
        val value = decoder.decodeInt()
        val name = namesByValue[value] ?: throw error(value)
        return entriesByName[name] ?: throw error(value)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun error(value: Any) = SerializationException(
        "$value is not a valid enum ${descriptor.serialName}, " +
            "must be one of $entries"
    )
}