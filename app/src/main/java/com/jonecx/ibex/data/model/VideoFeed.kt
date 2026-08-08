package com.jonecx.ibex.data.model

import com.jonecx.azmaree.player.model.StreamingProtocol
import com.jonecx.azmaree.player.model.VideoSource
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

/**
 * A user-added streaming link, mirroring the Azmaree app's VideoFeed. Stored on device and rendered
 * as a Live tile; [toVideoSource] hands it to the Azmaree player. Protocol defaults to whatever the
 * URL implies (HLS/DASH/progressive) but is persisted so it can be overridden later.
 */
@Serializable
data class VideoFeed(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val thumbnailUrl: String = "",
    @Serializable(with = StreamingProtocolSerializer::class)
    val protocol: StreamingProtocol = StreamingProtocol.fromUrl(url),
    val description: String = "",
) {
    fun toVideoSource(): VideoSource = VideoSource(
        id = id,
        url = url,
        protocol = protocol,
        thumbnailUrl = thumbnailUrl.ifBlank { null },
        title = title,
        description = description.ifBlank { null },
    )
}

// StreamingProtocol is an SDK enum with no serializer; persist it by name.
private object StreamingProtocolSerializer : KSerializer<StreamingProtocol> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StreamingProtocol", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: StreamingProtocol) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): StreamingProtocol =
        runCatching { StreamingProtocol.valueOf(decoder.decodeString()) }
            .getOrDefault(StreamingProtocol.PROGRESSIVE)
}
