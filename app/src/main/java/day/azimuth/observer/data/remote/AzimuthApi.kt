package day.azimuth.observer.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ─── Auth ────────────────────────────────────────────────────────────

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
)

data class RegisterResponse(
    @SerializedName("user_id") val userId: String,
    @SerializedName("api_key") val apiKey: String,
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
)

data class LoginResponse(
    @SerializedName("user_id") val userId: String,
    @SerializedName("api_key") val apiKey: String,
)

data class GoogleSignInRequest(
    @SerializedName("id_token") val idToken: String,
)

data class GoogleSignInResponse(
    @SerializedName("user_id") val userId: String,
    @SerializedName("api_key") val apiKey: String,
)

data class RotateKeyResponse(
    @SerializedName("api_key") val apiKey: String,
)

// ─── Nodes ───────────────────────────────────────────────────────────

data class RegisterNodeRequest(
    @SerializedName("hardware_type") val hardwareType: String,
    val label: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerializedName("altitude_m") val altitudeM: Double? = null,
)

data class RegisterNodeResponse(
    @SerializedName("node_id") val nodeId: String,
)

data class UpdateNodeRequest(
    val status: String? = null,
    val label: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerializedName("altitude_m") val altitudeM: Double? = null,
)

data class NodeInfo(
    val id: String,
    @SerializedName("hardware_type") val hardwareType: String,
    val label: String?,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("altitude_m") val altitudeM: Double?,
    val status: String,
    @SerializedName("registered_at") val registeredAt: String,
    @SerializedName("last_seen_at") val lastSeenAt: String?,
    @SerializedName("animal_name") val animalName: String? = null,
)

data class ListNodesResponse(
    val nodes: List<NodeInfo>,
)

// ─── Observations ────────────────────────────────────────────────────

data class ObservationPayload(
    @SerializedName("signal_type") val signalType: String,
    @SerializedName("observed_at") val observedAt: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val altitude: Double? = null,
    @SerializedName("frequency_hz") val frequencyHz: Long? = null,
    @SerializedName("timestamp_ns") val timestampNs: Long? = null,
    @SerializedName("signal_strength_dbm") val signalStrengthDbm: Double? = null,
    @SerializedName("snr_db") val snrDb: Double? = null,
    @SerializedName("source_id") val sourceId: String? = null,
    @SerializedName("raw_data") val rawData: Map<String, Any?>? = null,
    @SerializedName("app_version") val appVersion: String? = null,
    @SerializedName("build_number") val buildNumber: String? = null,
    @SerializedName("device_model") val deviceModel: String? = null,
    @SerializedName("android_api_level") val androidApiLevel: Int? = null,
    @SerializedName("validation_status") val validationStatus: String? = "raw",
    @SerializedName("client_dedupe_key") val clientDedupeKey: String? = null,
    @SerializedName("rtk_enabled") val rtkEnabled: Boolean = false,
)

data class SubmitObservationsRequest(
    @SerializedName("node_id") val nodeId: String,
    val observations: List<ObservationPayload>,
)

data class SubmitObservationsResponse(
    val accepted: Int,
    @SerializedName("points_earned") val pointsEarned: Int = 0,
)

// ─── Rewards ─────────────────────────────────────────────────────────

data class RewardInfo(
    val id: String,
    @SerializedName("node_id") val nodeId: String?,
    val epoch: Int,
    val amount: String,
    val reason: String,
    val status: String,
    @SerializedName("tx_hash") val txHash: String?,
    @SerializedName("created_at") val createdAt: String,
)

data class ListRewardsResponse(
    val rewards: List<RewardInfo>,
    @SerializedName("total_earned") val totalEarned: String,
)

// ─── Stats ───────────────────────────────────────────────────────────

data class NetworkStats(
    @SerializedName("total_users") val totalUsers: Int,
    @SerializedName("total_nodes") val totalNodes: Int,
    @SerializedName("active_nodes") val activeNodes: Int,
    @SerializedName("total_observations") val totalObservations: Long,
    @SerializedName("observations_24h") val observations24h: Long,
    @SerializedName("total_rewards_distributed") val totalRewardsDistributed: String,
)

// ─── Coverage Sync ──────────────────────────────────────────────────

data class CoverageHexUpload(
    @SerializedName("h3_index") val h3Index: String,
    @SerializedName("observation_count") val observationCount: Int,
    @SerializedName("signal_types") val signalTypes: List<String>,
    @SerializedName("first_seen") val firstSeen: String,
    @SerializedName("last_seen") val lastSeen: String,
)

data class PostCoverageRequest(
    val hexes: List<CoverageHexUpload>,
)

data class PostCoverageResponse(
    val synced: Int,
)

data class MyCoverageHex(
    @SerializedName("h3_index") val h3Index: String,
    @SerializedName("observation_count") val observationCount: Int,
    @SerializedName("signal_types") val signalTypes: List<String>,
    @SerializedName("first_seen") val firstSeen: String,
    @SerializedName("last_seen") val lastSeen: String,
)

data class GetMyCoverageResponse(
    val hexes: List<MyCoverageHex>,
)

data class GlobalCoverageHex(
    @SerializedName("h3_index") val h3Index: String,
    @SerializedName("contributor_count") val contributorCount: Int,
    @SerializedName("total_observations") val totalObservations: Long,
)

data class GetGlobalCoverageResponse(
    val hexes: List<GlobalCoverageHex>,
)

// ─── Points ──────────────────────────────────────────────────────────

data class PointEntry(
    val id: String,
    val amount: Int,
    val reason: String,
    @SerializedName("reference_id") val referenceId: String?,
    @SerializedName("created_at") val createdAt: String,
)

data class PointsResponse(
    val balance: Int,
    val history: List<PointEntry>,
)

// ─── Referral ────────────────────────────────────────────────────────

data class ReferralEntry(
    @SerializedName("referee_id") val refereeId: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("bonus_awarded") val bonusAwarded: Boolean,
)

data class ReferralResponse(
    @SerializedName("referral_code") val referralCode: String,
    @SerializedName("referral_count") val referralCount: Int,
    @SerializedName("total_bonus_points") val totalBonusPoints: Int,
    val referrals: List<ReferralEntry>,
)

// ─── Leaderboard ─────────────────────────────────────────────────────

data class LeaderboardEntry(
    val rank: Int,
    @SerializedName("animal_name") val animalName: String,
    val points: Int,
    @SerializedName("observation_count") val observationCount: Int,
)

data class LeaderboardResponse(
    val period: String,
    val entries: List<LeaderboardEntry>,
    @SerializedName("total_participants") val totalParticipants: Int,
)

// ─── API Interface ───────────────────────────────────────────────────

interface AzimuthApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/google")
    suspend fun googleSignIn(@Body request: GoogleSignInRequest): GoogleSignInResponse

    @POST("api/auth/rotate-key")
    suspend fun rotateKey(): RotateKeyResponse

    @POST("api/nodes/register")
    suspend fun registerNode(@Body request: RegisterNodeRequest): RegisterNodeResponse

    @PATCH("api/nodes/{nodeId}")
    suspend fun updateNode(
        @Path("nodeId") nodeId: String,
        @Body request: UpdateNodeRequest,
    )

    @POST("api/nodes/{nodeId}/heartbeat")
    suspend fun heartbeat(@Path("nodeId") nodeId: String)

    @GET("api/nodes/mine")
    suspend fun getMyNodes(): ListNodesResponse

    @POST("api/observations")
    suspend fun submitObservations(@Body request: SubmitObservationsRequest): SubmitObservationsResponse

    @GET("api/rewards/mine")
    suspend fun getMyRewards(): ListRewardsResponse

    @GET("api/stats")
    suspend fun getStats(): NetworkStats

    @POST("api/coverage")
    suspend fun postCoverage(@Body request: PostCoverageRequest): PostCoverageResponse

    @GET("api/coverage/mine")
    suspend fun getMyCoverage(): GetMyCoverageResponse

    @GET("api/coverage")
    suspend fun getGlobalCoverage(): GetGlobalCoverageResponse

    @GET("api/points/mine")
    suspend fun getMyPoints(@Query("limit") limit: Int = 50): PointsResponse

    @GET("api/referral/mine")
    suspend fun getMyReferral(): ReferralResponse

    @GET("api/leaderboard")
    suspend fun getLeaderboard(
        @Query("period") period: String = "alltime",
        @Query("limit") limit: Int = 50,
    ): LeaderboardResponse

    @GET("api/version")
    suspend fun getLatestVersion(): VersionInfo
}
