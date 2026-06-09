package day.azimuth.observer.data.local

enum class NtripConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    STREAMING,
    ERROR,
}

data class NtripStatus(
    val state: NtripConnectionState = NtripConnectionState.DISCONNECTED,
    val bytesReceived: Long = 0,
    val messagesDecoded: Int = 0,
    val lastMessageTime: Long? = null,
    val errorMessage: String? = null,
)
