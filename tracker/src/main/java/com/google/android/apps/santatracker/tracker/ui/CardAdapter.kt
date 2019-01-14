/*
 * Copyright 2019. Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.tracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.apps.santatracker.tracker.R
import com.google.android.apps.santatracker.tracker.time.Clock
import com.google.android.apps.santatracker.tracker.vo.Destination
import com.google.android.apps.santatracker.tracker.vo.DestinationStreetView
import com.google.android.apps.santatracker.tracker.vo.StreamEntry
import com.google.android.apps.santatracker.tracker.vo.TrackerCard

class CardAdapter(
    val clock: Clock,
    private val callback: Callback,
    recyclerView: RecyclerView,
    var disableDestinationPhoto: Boolean,
    private val isTv: Boolean
) : RecyclerView.Adapter<CardViewHolder>() {

    interface Callback {
        fun onPlayMovie(youtubeId: String)
        fun onStreetView(streetView: DestinationStreetView)
    }

    companion object {
        const val TYPE_DASHBOARD = 1
        const val TYPE_DESTINATION = 2
        const val TYPE_FACTOID = 3
        const val TYPE_MOVIE = 4
        const val TYPE_PHOTO = 5
        const val TYPE_UPDATE = 6
    }

    private var cards = emptyList<TrackerCard>()

    val dashboard: DashboardViewHolder =
            DashboardViewHolder(LayoutInflater.from(recyclerView.context), recyclerView)

    private val onClickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.movie_play -> callback.onPlayMovie(view.tag as String)
            R.id.destination_street_view -> callback.onStreetView(view.tag as DestinationStreetView)
        }
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemCount() = 1 + cards.size

    override fun getItemId(position: Int) = when (position) {
        0 -> -1
        else -> cardAt(position).value
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return TYPE_DASHBOARD
        }
        val card = cardAt(position)
        if (card is Destination) {
            return TYPE_DESTINATION
        }
        val streamEntry = card as StreamEntry
        return when (streamEntry.type) {
            StreamEntry.TYPE_DID_YOU_KNOW -> TYPE_FACTOID
            StreamEntry.TYPE_YOUTUBE_ID -> TYPE_MOVIE
            StreamEntry.TYPE_IMAGE_URL -> TYPE_PHOTO
            StreamEntry.TYPE_STATUS -> TYPE_UPDATE
            else -> -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DASHBOARD -> dashboard
            TYPE_DESTINATION -> DestinationViewHolder(
                    inflater, parent, onClickListener, clock, disableDestinationPhoto, !isTv)
            TYPE_FACTOID -> FactoidViewHolder(inflater, parent)
            TYPE_MOVIE -> MovieViewHolder(inflater, parent, onClickListener, !isTv)
            TYPE_PHOTO -> PhotoViewHolder(inflater, parent)
            TYPE_UPDATE -> UpdateViewHolder(inflater, parent)
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        if (position == 0) {
            return
        }
        holder.bind(cardAt(position))
    }

    fun setCards(cards: List<TrackerCard>) {
        this.cards = cards
        notifyDataSetChanged()
    }

    private fun cardAt(position: Int) = cards[position - 1]
}
