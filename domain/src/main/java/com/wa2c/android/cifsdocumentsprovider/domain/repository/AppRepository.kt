package com.samsung.cifs.domain.repository

import com.samsung.cifs.common.exception.AppException
import com.samsung.cifs.common.utils.generateUUID
import com.samsung.cifs.common.utils.logD
import com.samsung.cifs.common.values.ImportOption
import com.samsung.cifs.common.values.ProtocolType
import com.samsung.cifs.common.values.StorageType
import com.samsung.cifs.common.values.UiTheme
import com.samsung.cifs.data.db.ConnectionIO
import com.samsung.cifs.data.db.ConnectionSettingDao
import com.samsung.cifs.data.preference.AppPreferencesDataStore
import com.samsung.cifs.storage.manager.SshKeyManager
import com.samsung.cifs.domain.IoDispatcher
import com.samsung.cifs.domain.mapper.DomainMapper.toDataModel
import com.samsung.cifs.domain.mapper.DomainMapper.toDomainModel
import com.samsung.cifs.domain.mapper.DomainMapper.toIndexModel
import com.samsung.cifs.domain.model.KnownHost
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App Repository
 */
@Singleton
class AppRepository @Inject internal constructor(
    private val appPreferences: AppPreferencesDataStore,
    private val sshKeyManager: SshKeyManager,
    private val connectionSettingDao: ConnectionSettingDao,
    private val connectionIO: ConnectionIO,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    /** Connection flow */
    val connectionListFlow = connectionSettingDao.getList().map { list ->
        list.map { it.toIndexModel() }
    }

    /**
     * Move connections order
     */
    suspend fun moveConnection(fromPosition: Int, toPosition: Int) {
        logD("moveConnection: fromPosition=$fromPosition, toPosition=$toPosition")
        withContext(dispatcher) {
            connectionSettingDao.move(fromPosition, toPosition)
        }
    }


    /** UI Theme */
    val uiThemeFlow = appPreferences.uiThemeFlow

    /** UI Theme */
    suspend fun setUiTheme(value: UiTheme) = appPreferences.setUiTheme(value)

    /** Open File limit */
    val openFileLimitFlow = appPreferences.openFileLimitFlow

    /** Open File limit */
    suspend fun setOpenFileLimit(value: Int) = appPreferences.setOpenFileLimit(value)

    /** Use as local */
    val useAsLocalFlow = appPreferences.useAsLocalFlow

    /** Use as local */
    suspend fun setUseAsLocal(value: Boolean) = appPreferences.setUseAsLocal(value)

    /** Use foreground to make the app resilient to closing by Android OS */
    val useForegroundFlow = appPreferences.useForegroundFlow

    /** Use foreground to make the app resilient to closing by Android OS */
    suspend fun setUseForeground(value: Boolean) = appPreferences.setUseForeground(value)

    /**
     * Get known hosts
     */
    suspend fun getKnownHosts(): List<KnownHost> {
        val hostList = connectionSettingDao.getTypedList(StorageType.entries.filter { it.protocol == ProtocolType.SFTP }
            .map { it.value })
            .map { it.toDataModel() }
        return sshKeyManager.knownHostList.map { entity ->
            KnownHost(
                host = entity.host,
                type = entity.type,
                key = entity.key,
                connections = hostList.filter { it.host.equals(entity.host, true) }.map { it.toDomainModel() }
            )
        }
    }

    /**
     * Delete known host
     */
    fun deleteKnownHost(knownHost: KnownHost) {
        sshKeyManager.deleteKnownHost(knownHost.host, knownHost.type)
    }

    /**
     * Export settings
     */
    suspend fun exportSettings(
        uriText: String,
        password: String,
        checkedId: Set<String>,
    ): Int {
        return withContext(dispatcher) {
            try {
                val list = connectionSettingDao.getList().first().filter { checkedId.contains(it.id) }
                connectionIO.exportConnections(uriText, password, list)
                list.size
            } catch (e: Exception) {
                connectionIO.deleteConnection(uriText)
                throw AppException.Settings.Export(e)
            }
        }
    }

    /**
     * Import settings
     */
    suspend fun importSettings(
        uriText: String,
        password: String,
        importOption: ImportOption,
    ): Int {
        return withContext(dispatcher) {
            try {
                val list = connectionIO.importConnections(uriText, password)
                when (importOption) {
                    ImportOption.Replace -> {
                        connectionSettingDao.replace(list)
                        list.size
                    }

                    ImportOption.Overwrite -> {
                        connectionSettingDao.insertAll(list)
                        list.size
                    }

                    ImportOption.Ignore -> {
                        val idList = connectionSettingDao.getList().first().map { it.id }
                        var maxId = connectionSettingDao.getMaxSortOrder() + 1
                        val filteredList = list
                            .filter { !idList.contains(it.id) }
                            .map { it.copy(sortOrder = maxId++) }
                        connectionSettingDao.insertAll(filteredList)
                        filteredList.size
                    }

                    ImportOption.Append -> {
                        val idList = connectionSettingDao.getList().first().map { it.id }
                        var maxId = connectionSettingDao.getMaxSortOrder() + 1
                        val newList = list.map {
                            if (!idList.contains(it.id)) it
                            else it.copy(id = generateUUID(), sortOrder = maxId++)
                        }
                        connectionSettingDao.insertAll(newList)
                        newList.size
                    }
                }
            } catch (e: Exception) {
                throw AppException.Settings.Import(e)
            }
        }
    }

    /**
     * Migrate
     */
    suspend fun migrate() {
        appPreferences.migrate()
    }

}
