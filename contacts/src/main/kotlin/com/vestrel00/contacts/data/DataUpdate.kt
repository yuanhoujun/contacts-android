package com.vestrel00.contacts.data

import android.content.ContentResolver
import android.content.Context
import com.vestrel00.contacts.ContactsPermissions
import com.vestrel00.contacts.entities.MutableDataEntity
import com.vestrel00.contacts.entities.operation.updateOperation
import com.vestrel00.contacts.util.applyBatch

/**
 * Updates one or more data rows in the data table.
 *
 * Blank data ([MutableDataEntity.isBlank] will be deleted.
 *
 * Note that in cases where blank data are deleted, existing RawContact instances will still have
 * references to the deleted data instance. The RawContact instances must be refreshed to get the
 * most up-to-date data sets.
 *
 * Updating data that has already been deleted may return a successful result. However, no update
 * actually occurred in the Content Provider Data table because the data row no longer existed.
 * This also applies to data that has not yet been inserted.
 *
 * ## Permissions
 *
 * The [ContactsPermissions.WRITE_PERMISSION] is assumed to have been granted already in these
 * examples for brevity. All updates will do nothing if these permissions are not granted.
 *
 * ## Usage
 *
 * To update a set of [MutableDataEntity];
 *
 * ```kotlin
 * val result = dataUpdate
 *      .data(dataSet)
 *      .commit()
 * ```
 */
interface DataUpdate {

    /**
     * Adds the given [data] to the update queue, which will be updated on [commit].
     *
     * Blank data ([MutableDataEntity.isBlank] will be deleted instead.
     *
     * Only existing [data] that have been retrieved via a query will be added to the update queue.
     * Those that have been manually created via a constructor will be ignored and result in a
     * failed operation.
     */
    fun data(vararg data: MutableDataEntity): DataUpdate

    /**
     * See [DataUpdate.data].
     */
    fun data(data: Collection<MutableDataEntity>): DataUpdate

    /**
     * See [DataUpdate.data].
     */
    fun data(data: Sequence<MutableDataEntity>): DataUpdate

    /**
     * Updates the [MutableDataEntity]s in the queue (added via [data]) and returns the [Result].
     *
     * ## Permissions
     *
     * Requires [ContactsPermissions.WRITE_PERMISSION].
     *
     * ## Thread Safety
     *
     * This should be called in a background thread to avoid blocking the UI thread.
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun commit(): Result

    interface Result {

        /**
         * True if all data have successfully been updated. False if even one update failed.
         */
        val isSuccessful: Boolean

        /**
         * True if the [data] has been successfully updated. False otherwise.
         */
        fun isSuccessful(data: MutableDataEntity): Boolean
    }
}

@Suppress("FunctionName")
internal fun DataUpdate(context: Context): DataUpdate = DataUpdateImpl(
    context.contentResolver,
    ContactsPermissions(context)
)

private class DataUpdateImpl(
    private val contentResolver: ContentResolver,
    private val permissions: ContactsPermissions,
    private val data: MutableSet<MutableDataEntity> = mutableSetOf()
) : DataUpdate {

    override fun data(vararg data: MutableDataEntity): DataUpdate = data(data.asSequence())

    override fun data(data: Collection<MutableDataEntity>): DataUpdate = data(data.asSequence())

    override fun data(data: Sequence<MutableDataEntity>): DataUpdate = apply {
        this.data.addAll(data)
    }

    override fun commit(): DataUpdate.Result {
        if (data.isEmpty() || !permissions.canInsertUpdateDelete()) {
            return DataUpdateFailed
        }

        val results = mutableMapOf<Long, Boolean>()
        for (data in data) {
            val dataId = data.id
            if (dataId != null) {
                results[dataId] = contentResolver.updateData(data)
            } else {
                results[INVALID_ID] = false
            }
        }
        return DataUpdateResult(results)
    }

    private companion object {
        // A failed entry in the results so that Result.isSuccessful returns false.
        const val INVALID_ID = -1L
    }
}

private fun ContentResolver.updateData(data: MutableDataEntity): Boolean =
    data.updateOperation()?.let { applyBatch(it) } != null

private class DataUpdateResult(private val dataIdsResultMao: Map<Long, Boolean>) :
    DataUpdate.Result {

    override val isSuccessful: Boolean by lazy { dataIdsResultMao.all { it.value } }

    override fun isSuccessful(data: MutableDataEntity): Boolean {
        val dataId = data.id
        return dataId != null && dataIdsResultMao.getOrElse(dataId) { false }
    }
}

private object DataUpdateFailed : DataUpdate.Result {

    override val isSuccessful: Boolean = false

    override fun isSuccessful(data: MutableDataEntity): Boolean = false
}
