package com.jonecx.ibex.data.repository

import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.NetworkConnection
import com.jonecx.ibex.data.preferences.NetworkConnectionsPreferencesContract
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.jonecx.ibex.util.FileTypeUtils
import com.jonecx.ibex.util.FileTypeUtils.toFileItem
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Properties

class SmbFileRepository(
    private val connectionId: String,
    private val networkPreferences: NetworkConnectionsPreferencesContract,
    private val settingsPreferences: SettingsPreferencesContract,
    private val ioDispatcher: CoroutineDispatcher,
    private val smbContextProvider: SmbContextProviderContract,
) : FileRepository {

    @Volatile
    private var cachedConnection: NetworkConnection? = null

    @Volatile
    private var cachedContext: CIFSContext? = null

    private suspend fun resolveConnection(): NetworkConnection {
        cachedConnection?.let { return it }
        val connection = networkPreferences.connections.first()
            .firstOrNull { it.id == connectionId }
            ?: throw IllegalStateException("Connection not found")
        cachedConnection = connection
        return connection
    }

    private fun createSmbContext(connection: NetworkConnection): CIFSContext {
        cachedContext?.let { return it }
        // Reuse the host's live context (shared with thumbnails, player, transfers); rebuild only on cred change.
        val context = smbContextProvider.getOrCreate(connection.host, connection.contextSignature()) {
            buildSmbContext(connection)
        }
        cachedContext = context
        return context
    }

    private fun buildSmbContext(connection: NetworkConnection): CIFSContext {
        val properties = Properties().apply {
            setProperty("jcifs.smb.client.responseTimeout", RESPONSE_TIMEOUT_MS.toString())
            setProperty("jcifs.smb.client.soTimeout", SOCKET_TIMEOUT_MS.toString())
            setProperty("jcifs.smb.client.minVersion", SMB_MIN_VERSION)
            setProperty("jcifs.smb.client.maxVersion", SMB_MAX_VERSION)
        }
        val baseContext = BaseContext(PropertyConfiguration(properties))
        return if (connection.anonymous) {
            baseContext.withAnonymousCredentials()
        } else {
            baseContext.withCredentials(
                NtlmPasswordAuthenticator(
                    "",
                    connection.username,
                    connection.password,
                ),
            )
        }
    }

    // Hashed so the raw password is never held in the long-lived context provider; only used to detect cred changes.
    private fun NetworkConnection.contextSignature(): String =
        "$host:$port:$anonymous:$username:$password".hashCode().toString()

    private fun buildRootUrl(connection: NetworkConnection): String {
        return if (connection.port == connection.protocol.defaultPort) {
            "${FileTypeUtils.SMB_SCHEME_PREFIX}${connection.host}/"
        } else {
            "${FileTypeUtils.SMB_SCHEME_PREFIX}${connection.host}:${connection.port}/"
        }
    }

    override fun getFiles(path: String): Flow<List<FileItem>> = flow {
        val connection = resolveConnection()
        val context = createSmbContext(connection)

        val smbUrl = if (path.startsWith(FileTypeUtils.SMB_SCHEME_PREFIX)) {
            FileTypeUtils.smbEnsureTrailingSlash(path)
        } else {
            buildRootUrl(connection)
        }

        // Counting children is one extra SMB round trip per folder, so it is opt-in via settings.
        val includeItemCount = settingsPreferences.networkFolderItemCountEnabled.first()
        // SmbFile is AutoCloseable; close the listing handle and every child once mapped to a plain FileItem.
        val files = SmbFile(smbUrl, context).use { smbFile ->
            smbFile.listFiles().map { child ->
                child.use { it.toFileItem(detailed = includeItemCount) }
            }
        }
        emit(files)
    }.flowOn(ioDispatcher)

    override fun getStorageRoots(): Flow<List<FileItem>> = flow {
        val connection = resolveConnection()
        val context = createSmbContext(connection)
        val rootUrl = buildRootUrl(connection)
        // Close the root handle and every child; keep only the shares mapped to plain FileItems.
        val shares = SmbFile(rootUrl, context).use { root ->
            root.listFiles().mapNotNull { child ->
                child.use { if (it.type == SmbFile.TYPE_SHARE) it.toFileItem(detailed = false) else null }
            }.sortedBy { it.name.lowercase() }
        }
        emit(shares)
    }.flowOn(ioDispatcher)

    override suspend fun getFileDetails(path: String): FileItem? {
        return try {
            val connection = resolveConnection()
            val context = createSmbContext(connection)
            SmbFile(FileTypeUtils.smbEnsureTrailingSlash(path), context).use { smbFile ->
                if (smbFile.exists()) smbFile.toFileItem() else null
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val RESPONSE_TIMEOUT_MS = "30000"
        private const val SOCKET_TIMEOUT_MS = "35000"
        private const val SMB_MIN_VERSION = "SMB202"
        private const val SMB_MAX_VERSION = "SMB311"
    }
}
