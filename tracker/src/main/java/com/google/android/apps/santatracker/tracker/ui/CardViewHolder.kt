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

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Build
import android.text.method.LinkMovementMethod
import android.text.method.TransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.bumptech.glide.Glide
import com.google.android.apps.santatracker.tracker.R
import com.google.android.apps.santatracker.tracker.time.Clock
import com.google.android.apps.santatracker.tracker.util.HtmlCompat
import com.google.android.apps.santatracker.tracker.vo.Destination
import com.google.android.apps.santatracker.tracker.vo.StreamEntry
import com.google.android.apps.santatracker.tracker.vo.TrackerCard

/**
 * Holds views for each of the items in the card stream.
 */
abstract class CardViewHolder(inflater: LayoutInflater, parent: ViewGroup, @LayoutRes resId: Int)
    : RecyclerView.ViewHolder(inflater.inflate(resId, parent, false)) {

    val card: View by lazy { itemView.findViewById<View>(R.id.card) }

    abstract fun bind(card: TrackerCard)
}

/**
 * The dashboard.
 */
class DashboardViewHolder(inflater: LayoutInflater, parent: ViewGroup)
    : CardViewHolder(inflater, parent, R.layout.card_dashboard) {

    val presentsContainer: ViewGroup
            by lazy { itemView.findViewById<ViewGroup>(R.id.dashboard_presents_container) }
    val presents: TextView
            by lazy { itemView.findViewById<TextView>(R.id.dashboard_presents) }

    val countdownContainer: ViewGroup
            by lazy { itemView.findViewById<ViewGroup>(R.id.dashboard_countdown_container) }
    val countdownLabel: TextView
            by lazy { itemView.findViewById<TextView>(R.id.dashboard_countdown_label) }
    val countdown: TextView
            by lazy { itemView.findViewById<TextView>(R.id.dashboard_countdown) }

    val locationLabel: TextView
            by lazy { itemView.findViewById<TextView>(R.id.dashboard_location_label) }
    val location: TextView
            by lazy { itemView.findViewById<TextView>(R.id.dashboard_location) }

    override fun bind(card: TrackerCard) {
        // Dashboard is special. The content is directly bound from the adapter
    }
}

/**
 * Stores view references for a destination card.
 */
class DestinationViewHolder(
    inflater: LayoutInflater,
    parent: ViewGroup,
    listener: View.OnClickListener,
    val clock: Clock,
    private val disablePhoto: Boolean,
    val enableStreetView: Boolean
)
    : CardViewHolder(inflater, parent, R.layout.card_destination) {

    companion object {
        var allCaps: AllCaps? = null
        var linkMovementMethod: LinkMovementMethod? = null
    }

    val region: TextView
            by lazy { itemView.findViewById<TextView>(R.id.destination_region) }

    val city: TextView
            by lazy { itemView.findViewById<TextView>(R.id.destination_city) }

    val copyright: TextView
            by lazy { itemView.findViewById<TextView>(R.id.destination_copyright) }

    val arrival: TextView
            by lazy { itemView.findViewById<TextView>(R.id.destination_arrival) }

    val weather: TextView
            by lazy { itemView.findViewById<TextView>(R.id.destination_weather) }

    private val weatherLabel: TextView
            by lazy { itemView.findViewById<TextView>(R.id.destination_weather_label) }

    val image: ImageView
            by lazy { itemView.findViewById<ImageView>(R.id.destination_image) }

    val streetView: Button
            by lazy { itemView.findViewById<Button>(R.id.destination_street_view) }

    init {
        val resources = itemView.resources
        val theme = itemView.context.theme
        image.setColorFilter(ResourcesCompat.getColor(resources,
                com.google.android.apps.santatracker.common.R.color.overlayDestinationCardFilter,
                theme), PorterDuff.Mode.MULTIPLY)
        streetView.setOnClickListener(listener)

        val iconSize = resources.getDimensionPixelSize(R.dimen.destination_icon_size)
        val arrivalIcon = VectorDrawableCompat.create(resources, R.drawable.ic_arrival, theme)
        arrivalIcon?.setBounds(0, 0, iconSize, iconSize)
        val weatherIcon = VectorDrawableCompat.create(resources, R.drawable.ic_weather, theme)
        weatherIcon?.setBounds(0, 0, iconSize, iconSize)
        arrival.setCompoundDrawablesRelative(arrivalIcon, null, null, null)
        weather.setCompoundDrawablesRelative(weatherIcon, null, null, null)

        if (allCaps == null) {
            allCaps = AllCaps(parent.context.applicationContext)
        }
        if (linkMovementMethod == null) {
            linkMovementMethod = LinkMovementMethod()
        }
        region.transformationMethod = allCaps
        copyright.movementMethod = linkMovementMethod
    }

    override fun bind(card: TrackerCard) {
        val destination = card as Destination
        val context = itemView.context
        region.text = destination.region
        city.text = destination.city
        arrival.text = clock.formatTime(destination.arrival)
        destination.weather.let { w ->
            if (w != null) {
                weatherLabel.visibility = View.VISIBLE
                weather.visibility = View.VISIBLE
                weather.text = context.getString(R.string.weather_format,
                        w.tempC.toInt().toString(), w.tempF.toInt().toString())
            } else {
                weatherLabel.visibility = View.INVISIBLE
                weather.visibility = View.INVISIBLE
            }
        }
        destination.photo.let { photo ->
            if (photo != null && !disablePhoto) {
                Glide.with(itemView.context).load(photo.url).into(image)
                copyright.visibility = View.VISIBLE
                copyright.text = HtmlCompat.fromHtml(photo.attribution, 0)
            } else {
                copyright.visibility = View.INVISIBLE
            }
        }
        destination.streetView.let { sv ->
            if (enableStreetView && sv != null) {
                streetView.visibility = View.VISIBLE
                streetView.tag = sv
            } else {
                streetView.visibility = View.INVISIBLE
            }
        }
    }
}

/**
 * Stores view references for a factoid card.
 */
class FactoidViewHolder(inflater: LayoutInflater, parent: ViewGroup)
    : CardViewHolder(inflater, parent, R.layout.card_factoid) {

    private val body: TextView
            by lazy { itemView.findViewById<TextView>(R.id.factoid_text) }

    override fun bind(card: TrackerCard) {
        val entry = card as StreamEntry
        body.text = entry.content
    }
}

/**
 * Stores view references for a movie card.
 */
class MovieViewHolder(
    inflater: LayoutInflater,
    parent: ViewGroup,
    listener: View.OnClickListener,
    enablePlay: Boolean
)
    : CardViewHolder(inflater, parent, R.layout.card_movie) {

    private val thumbnail: ImageView
            by lazy { itemView.findViewById<ImageView>(R.id.movie_thumbnail) }
    val play: Button
            by lazy { itemView.findViewById<Button>(R.id.movie_play) }

    init {
        play.setOnClickListener(listener)
        play.visibility = if (enablePlay) View.VISIBLE else View.INVISIBLE
    }

    override fun bind(card: TrackerCard) {
        val youtubeId = (card as StreamEntry).content
        play.tag = youtubeId
        val url = "https://img.youtube.com/vi/$youtubeId/0.jpg"
        Glide.with(itemView.context).load(url).into(thumbnail)
    }
}

/**
 * Stores view references for a photo card.
 */
class PhotoViewHolder(inflater: LayoutInflater, parent: ViewGroup)
    : CardViewHolder(inflater, parent, R.layout.card_photo) {

    val image: ImageView
            by lazy { itemView.findViewById<ImageView>(R.id.photo_image) }

    override fun bind(card: TrackerCard) {
        val url = (card as StreamEntry).content
        Glide.with(itemView.context).load(url).into(image)
    }
}

/**
 * Stores view references for a update card.
 */
class UpdateViewHolder(inflater: LayoutInflater, parent: ViewGroup)
    : CardViewHolder(inflater, parent, R.layout.card_update) {

    val content: TextView
            by lazy { itemView.findViewById<TextView>(R.id.update_text) }

    override fun bind(card: TrackerCard) {
        val entry = card as StreamEntry
        content.text = entry.content
    }
}

class AllCaps(context: Context) : TransformationMethod {

    private val locale = if (Build.VERSION.SDK_INT >= 24) {
        val locales = context.resources.configuration.locales
        locales.get(0)
    } else {
        @Suppress("DEPRECATION")
        context.resources.configuration.locale
    }

    override fun getTransformation(source: CharSequence?, view: View?): CharSequence? {
        return source?.toString()?.toUpperCase(locale)
    }

    override fun onFocusChanged(
        view: View?,
        sourceText: CharSequence?,
        focused: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        // Do nothing
    }
}
