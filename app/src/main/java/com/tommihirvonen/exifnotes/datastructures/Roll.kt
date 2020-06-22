package com.tommihirvonen.exifnotes.datastructures

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Roll(var id: Long = 0,
           var name: String? = null,
           var date: DateTime? = null,
           var unloaded: DateTime? = null,
           var developed: DateTime? = null,
           var note: String? = null,
           var camera: Camera? = null,
           var iso: Int = 0,
           var pushPull: String? = null,
           private var format_: Int = 0,
           var archived: Boolean = false,
           var filmStock: FilmStock? = null
           ) : Parcelable {

    init {
        if (format_ !in 0..3) format_ = 0
    }

    var format: Int
        get() = if (format_ in 0..3) format_ else 0
        set(value) { if (value in 0..3) format_ = value }

    companion object {
        /**
         * Sorts a list of Roll objects based on sortMode in place.
         * Called when the user has selected a sorting criteria.
         *
         * @param sortMode SortMode enum type
         * @param list reference to the List that should be sorted
         */
        fun sortRollList(sortMode: RollSortMode, list: MutableList<Roll>) {
            when (sortMode) {
                RollSortMode.DATE -> list.sortWith(compareByDescending { it.date })
                RollSortMode.NAME -> list.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name ?: "" })
                RollSortMode.CAMERA -> list.sortWith(compareBy { it.camera }) }
        }
    }

}