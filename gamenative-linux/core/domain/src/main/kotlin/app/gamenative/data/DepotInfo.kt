package app.gamenative.data

import app.gamenative.db.serializers.OsEnumSetSerializer
import app.gamenative.enums.OS
import app.gamenative.enums.OSArch
import java.util.EnumSet
import kotlinx.serialization.Serializable

private const val INVALID_APP_ID: Int = Int.MAX_VALUE

@Serializable
data class DepotInfo(
    val depotId: Int,
    val dlcAppId: Int,
    val optionalDlcId: Int = INVALID_APP_ID,
    val depotFromApp: Int,
    val sharedInstall: Boolean,
    @Serializable(with = OsEnumSetSerializer::class)
    val osList: EnumSet<OS>,
    val osArch: OSArch,
    val manifests: Map<String, ManifestInfo>,
    val encryptedManifests: Map<String, ManifestInfo>,
    val language: String = "",
    val realm: String = "",
)
