package com.jonecx.ibex.data.repository

import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.NetworkConnection
import com.jonecx.ibex.data.preferences.NetworkConnectionsPreferencesContract
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
        val properties = Properties().apply {
            setProperty("jcifs.smb.client.responseTimeout", RESPONSE_TIMEOUT_MS.toString())
            setProperty("jcifs.smb.client.soTimeout", SOCKET_TIMEOUT_MS.toString())
            setProperty("jcifs.smb.client.minVersion", SMB_MIN_VERSION)
            setProperty("jcifs.smb.client.maxVersion", SMB_MAX_VERSION)
        }
        val baseContext = BaseContext(PropertyConfiguration(properties))
        val context = if (connection.anonymous) {
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
        cachedContext = context
        smbContextProvider.register(connection.host, context)
        return context
    }

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

        val smbFile = SmbFile(smbUrl, context)
        val files = smbFile.listFiles()
            .map { it.toFileItem() }
        emit(files)
    }.flowOn(ioDispatcher)

    override fun getStorageRoots(): Flow<List<FileItem>> = flow {
        val connection = resolveConnection()
        val context = createSmbContext(connection)
        val rootUrl = buildRootUrl(connection)
        val root = SmbFile(rootUrl, context)
        val shares = root.listFiles()
            .filter { it.type == SmbFile.TYPE_SHARE }
            .map { it.toFileItem() }
            .sortedBy { it.name.lowercase() }
        emit(shares)
    }.flowOn(ioDispatcher)

    override suspend fun getFileDetails(path: String): FileItem? {
        return try {
            val connection = resolveConnection()
            val context = createSmbContext(connection)
            val smbFile = SmbFile(FileTypeUtils.smbEnsureTrailingSlash(path), context)
            if (smbFile.exists()) smbFile.toFileItem() else null
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
