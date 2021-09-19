package contacts.entities

import android.content.res.Resources
import android.provider.ContactsContract
import contacts.util.isProfileId

/**
 * [Entity] in the data table that belong to a [RawContact].
 */
interface CommonDataEntity : Entity {

    /**
     * The id of the Data row this represents.
     */
    override val id: Long?

    /**
     * The id of the [RawContact] that this data entity is associated with.
     */
    val rawContactId: Long?

    /**
     * The id of the [Contact] that this data entity is associated with.
     */
    val contactId: Long?

    /**
     * The type of data.
     */
    val mimeType: MimeType

    /**
     * Whether this is the primary entry of its kind for the [RawContact] it belongs to.
     *
     * ## Developer Notes
     *
     * This is immutable to prevent consumers from setting multiple data entities of the same
     * mimetype as primary. Consumers should use the DefaultContactData extension functions to
     * modify these values.
     */
    val isPrimary: Boolean

    /**
     * Whether this is the primary entry of its kind for the aggregate [Contact] it belongs to. Any
     * data record that is "super primary" must also be [isPrimary].
     *
     * ## Developer Notes
     *
     * This is immutable to prevent consumers from setting multiple data entities of the same
     * mimetype as primary. Consumers should use the DefaultContactData extension functions to
     * modify these values.
     */
    val isSuperPrimary: Boolean

    /**
     * True if [isSuperPrimary] is true.
     *
     * "Default" is the terminology used by the native Contacts app. Consumers should use the
     * DefaultContactData extension functions to set a data entity as default or not.
     */
    val isDefault: Boolean
        get() = isSuperPrimary

    /**
     * True if this data belongs to the user's personal profile entry.
     */
    val isProfile: Boolean
        get() = id.isProfileId

    /**
     * A type of data. Used by data that may have several types.
     */
    interface Type {
        /**
         * The actual value that represents the type.
         */
        val value: Int

        /**
         * True if this type is the custom type.
         */
        val isCustomType: Boolean
            get() = value == ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM

        /**
         * The string representation of this type.
         *
         * If this [isCustomType] then the [label] is used (if not null). Otherwise, the
         * Android default string for this type is used.
         */
        fun labelStr(resources: Resources, label: String?): String
    }
}

/**
 * A [CommonDataEntity] that is mutable.
 */
interface MutableCommonDataEntity : CommonDataEntity