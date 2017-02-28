/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.map.cardstream;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.data.Destination;
import com.google.android.apps.santatracker.data.StreamEntry;
import com.google.android.apps.santatracker.util.SantaLog;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubeThumbnailView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CardAdapter extends RecyclerView.Adapter<CardViewHolder> {

    private final String mYouTubeApiDeveloperKey;
    private final Context mContext;

    private boolean mDisableDestinationPhoto = true;

    private final Typeface mTypefaceLabel;
    private final Typeface mTypefaceBody;

    private final CardAdapterListener mListener;
    private final DestinationCardKeyListener mDestinationCardListener;

    private final long mDashboardId;

    private TvFocusAnimator mFocusHighlight;

    public static final int DASHBOARD_POSITION = 0;

    /**
     * The list of cards. Note that the cards always need to be sorted by timestamps in the
     * descending order.
     */
    private final ArrayList<TrackerCard> mCards = new ArrayList<>();
    private ThumbnailListener mThumbnailListener;
    private Map<YouTubeThumbnailView, YouTubeThumbnailLoader> mThumbnailViewToLoader =
            new HashMap<>();

    private static final String TAG = "CardAdaptor";
    private final boolean mIsTv;

    public CardAdapter(Context context, CardAdapterListener listener) {
        this(context, listener, null, false);
    }

    public CardAdapter(Context context, CardAdapterListener listener,
                       DestinationCardKeyListener destCardListener, boolean isTv) {
        mContext = context;
        mTypefaceLabel = Typeface.createFromAsset(context.getAssets(),
                context.getResources().getString(R.string.typeface_roboto_black));
        mTypefaceBody = Typeface.createFromAsset(context.getAssets(),
                context.getResources().getString(R.string.typeface_roboto_light));
        mListener = listener;
        mDestinationCardListener = destCardListener;

        TrackerCard.Dashboard dashboard = TrackerCard.Dashboard.getInstance();
        mDashboardId = dashboard.id;
        mCards.add(DASHBOARD_POSITION, dashboard);
        mThumbnailListener = new ThumbnailListener();
        mYouTubeApiDeveloperKey = context.getString(R.string.config_maps_api_key);
        mIsTv = isTv;

        setHasStableIds(true);
    }

    @Override
    public CardViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        CardViewHolder holder;
        switch (type) {
            case TrackerCard.TYPE_DASHBOARD: {
                View view = LayoutInflater.from(mContext)
                        .inflate(R.layout.item_card_dashboard, parent, false);
                holder = new DashboardViewHolder(view);
                break;
            }
            case TrackerCard.TYPE_FACTOID: {
                View view = LayoutInflater.from(mContext)
                        .inflate(R.layout.item_card_factoid, parent, false);
                holder = new FactoidViewHolder(view);
                break;
            }
            case TrackerCard.TYPE_DESTINATION: {
                View view = LayoutInflater.from(mContext)
                        .inflate(R.layout.item_card_destination, parent, false);
                holder = new DestinationViewHolder(view);
                break;
            }
            case TrackerCard.TYPE_PHOTO: {
                View view = LayoutInflater.from(mContext)
                        .inflate(R.layout.item_card_photo, parent, false);
                holder = new PhotoViewHolder(view);
                break;
            }
            case TrackerCard.TYPE_MOVIE: {
                View view = LayoutInflater.from(mContext)
                        .inflate(R.layout.item_card_movie, parent, false);
                holder = new MovieViewHolder(view);
                break;
            }
            case TrackerCard.TYPE_STATUS: {
                View view = LayoutInflater.from(mContext)
                        .inflate(R.layout.item_card_update, parent, false);
                holder = new UpdateViewHolder(view);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unexpected type of card.");
            }
        }
        holder.setTypefaces(mTypefaceLabel, mTypefaceBody);
        setupTvCardIfNecessary(holder);
        return holder;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setupTvCardIfNecessary(CardViewHolder holder) {
        if (mIsTv) {

            if (mFocusHighlight == null) {
                mFocusHighlight = new TvFocusAnimator();
            }

            final Resources res = holder.itemView.getResources();

            TvFocusAnimator.FocusChangeListener listener
                    = new TvFocusAnimator.FocusChangeListener(mFocusHighlight);

            holder.itemView.setFocusable(true);
            holder.itemView.setFocusableInTouchMode(false);
            holder.itemView.setOnFocusChangeListener(listener);
            holder.itemView.setElevation(res.getDimensionPixelOffset(R.dimen.toolbar_elevation));

            mFocusHighlight.onInitializeView(holder.itemView);

            if (holder.itemView.getBackground() == null) {
                holder.itemView.setBackground(ResourcesCompat.getDrawable(res,
                        R.drawable.tv_tracker_card_selector,
                        holder.itemView.getContext().getTheme()));
            }
        }
    }

    @Override
    public void onBindViewHolder(CardViewHolder holder, int position) {
        TrackerCard card = mCards.get(position);
        if (card instanceof TrackerCard.Dashboard) {
            ((DashboardViewHolder) holder).location.setSelected(true);
        } else if (card instanceof TrackerCard.DestinationCard) {
            TrackerCard.DestinationCard destination = (TrackerCard.DestinationCard) card;
            DestinationViewHolder h = (DestinationViewHolder) holder;
            Context context = h.itemView.getContext();
            h.city.setText(destination.city);
            if (destination.region == null) {
                h.region.setVisibility(View.GONE);
            } else {
                h.region.setText(destination.region);
                h.region.setVisibility(View.VISIBLE);
            }
            if (destination.url != null && destination.attributionHtml != null) {
                // Image
                Glide.with(context).load(destination.url).into(h.image);
                // Attribution
                Spanned attribution;
                if (Build.VERSION.SDK_INT >= 24) {
                    attribution = Html.fromHtml(destination.attributionHtml, 0);
                } else {
                    //noinspection deprecation
                    attribution = Html.fromHtml(destination.attributionHtml);
                }
                h.copyright.setText(attribution);
            }
            h.arrival.setText(DashboardFormats.formatTime(context, destination.timestamp));
            if (destination.hasWeather) {
                h.weatherLabel.setVisibility(View.VISIBLE);
                h.weather.setVisibility(View.VISIBLE);
                h.weather.setText(context.getString(R.string.weather_format,
                        String.valueOf((int) destination.tempC),
                        String.valueOf((int) destination.tempF)));
            } else {
                h.weatherLabel.setVisibility(View.GONE);
                h.weather.setVisibility(View.GONE);
            }

            if (mIsTv) {
                h.streetView.setVisibility(View.GONE);

                if (destination.position != null) {
                    h.card.setTag(destination.position);
                    h.card.setOnKeyListener(mMoveToDestinationListener);
                    h.card.setClickable(true);
                } else {
                    h.card.setTag(null);
                    h.card.setOnKeyListener(null);
                    h.card.setClickable(false);
                }
            } else {
                if (destination.streetView != null) {
                    h.streetView.setVisibility(View.VISIBLE);
                    h.streetView.setOnClickListener(mShowStreetViewListener);
                    h.streetView.setTag(destination.streetView);
                } else if (h.streetView != null) {
                    h.streetView.setVisibility(View.GONE);
                }

            }
        } else if (card instanceof TrackerCard.FactoidCard) {
            TrackerCard.FactoidCard factoid = (TrackerCard.FactoidCard) card;
            FactoidViewHolder h = (FactoidViewHolder) holder;
            h.body.setText(factoid.factoid);
        } else if (card instanceof TrackerCard.PhotoCard) {
            TrackerCard.PhotoCard photo = (TrackerCard.PhotoCard) card;
            PhotoViewHolder h = (PhotoViewHolder) holder;
            Glide.with(h.image.getContext()).load(photo.imageUrl).into(h.image);
        } else if (card instanceof TrackerCard.MovieCard) {
            if (mThumbnailViewToLoader == null || mThumbnailListener == null) {
                return;
            }
            TrackerCard.MovieCard movie = (TrackerCard.MovieCard) card;
            MovieViewHolder h = (MovieViewHolder) holder;
            h.thumbnail.setTag(movie.youtubeId);
            if (mThumbnailViewToLoader.containsKey(h.thumbnail)) {
                final YouTubeThumbnailLoader loader = mThumbnailViewToLoader.get(h.thumbnail);
                if (loader != null) {
                    loader.setVideo(movie.youtubeId);
                }
            } else {
                h.thumbnail.setImageDrawable(null);
                h.thumbnail.initialize(mYouTubeApiDeveloperKey, mThumbnailListener);
                mThumbnailViewToLoader.put(h.thumbnail, null);
            }
            h.play.setTag(movie.youtubeId);
            h.play.setOnClickListener(mPlayVideoListener);
            if (mIsTv) {
                h.card.setTag(movie.youtubeId);
                h.card.setOnClickListener(mPlayVideoListener);
            }
        } else if (card instanceof TrackerCard.StatusCard) {
            TrackerCard.StatusCard status = (TrackerCard.StatusCard) card;
            UpdateViewHolder h = (UpdateViewHolder) holder;
            h.content.setText(status.status);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        release();
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public long getItemId(int position) {
        return mCards.get(position).id;
    }

    @Override
    public int getItemCount() {
        return mCards.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mCards.get(position).getType();
    }

    public long getDashboardId() {
        return mDashboardId;
    }

    /**
     * Find the right index to insert a new card with the specified {@code timestamp}. Note that
     * this method assumes that {@link #mCards} are sorted by timestamps in the descending order.
     *
     * @param timestamp The unix time.
     * @return The index to insert a new card in {@link #mCards}.
     */
    private int findCardIndexByTimestamp(long timestamp) {
        // This is basically a binary search that doesn't search for a specific value but an index.
        int head = 0;
        int tail = mCards.size();
        while (head < tail) {
            int needle = (head + tail) / 2;
            long needleTimestamp = mCards.get(needle).timestamp;
            if (timestamp < needleTimestamp) {
                head = needle + 1;
            } else {
                tail = needle;
            }
        }
        return head;
    }

    private int addCard(TrackerCard card) {
        final int index = findCardIndexByTimestamp(card.timestamp);
        // Replace a old duplicated card if it exists.
        if (index < mCards.size() && mCards.get(index).equals(card)) {
            mCards.remove(index);
            mCards.add(index, card);
            notifyItemChanged(index);
        } else {
            mCards.add(index, card);
            notifyItemInserted(index);
        }
        return index;
    }

    public int addDestination(boolean fromUser, Destination destination, boolean showStreetView) {
        String url = null;
        String copyright = null;
        if (!mDisableDestinationPhoto && destination.photos != null
                && destination.photos.length > 0) {
            url = destination.photos[0].url;
            copyright = destination.photos[0].attributionHTML;
        }
        Destination.StreetView streetView = null;
        if (showStreetView && destination.streetView != null && destination.streetView.id != null
                && !destination.streetView.id.isEmpty()) {
            streetView = destination.streetView;
        }
        boolean hasWeather = destination.weather != null;
        double tempC = 0, tempF = 0;
        if (hasWeather) {
            tempC = destination.weather.tempC;
            tempF = destination.weather.tempF;
        }
        return addCard(new TrackerCard.DestinationCard(destination.arrival, destination.position,
                fromUser, destination.city, destination.region,
                url, copyright, streetView, hasWeather, tempC, tempF));
    }

    public TrackerCard addStreamEntry(StreamEntry entry) {
        TrackerCard card = null;
        if (entry.didYouKnow != null) {
            // Did you know card
            card = new TrackerCard.FactoidCard(entry.timestamp, entry.didYouKnow);
            addCard(card);
        } else if (entry.santaStatus != null) {
            // Status card
            card = new TrackerCard.StatusCard(entry.timestamp, entry.santaStatus);
            addCard(card);
        } else if (entry.image != null) {
            // Image card
            card = new TrackerCard.PhotoCard(entry.timestamp, entry.image, entry.caption);
            addCard(card);
        } else if (entry.video != null) {
            // Video card
            card = new TrackerCard.MovieCard(entry.timestamp, entry.video);
            addCard(card);
        }
        return card;
    }

    public void setNextLocation(String nextLocation) {
        TrackerCard.Dashboard.getInstance().nextDestination = nextLocation;
    }

    public void setDestinationPhotoDisabled(boolean disablePhoto) {
        mDisableDestinationPhoto = disablePhoto;
    }

    private final class ThumbnailListener implements
            YouTubeThumbnailView.OnInitializedListener,
            YouTubeThumbnailLoader.OnThumbnailLoadedListener {

        @Override
        public void onInitializationSuccess(
                YouTubeThumbnailView view, YouTubeThumbnailLoader loader) {
            loader.setOnThumbnailLoadedListener(this);
            view.setScaleType(ImageView.ScaleType.CENTER_CROP);
            loader.setVideo((String) view.getTag());
            mThumbnailViewToLoader.put(view, loader);
        }

        @Override
        public void onInitializationFailure(
                YouTubeThumbnailView view, YouTubeInitializationResult loader) {
            SantaLog.e(TAG, "Failed to initialize YouTubeThumbnailView.");
            view.setImageResource(R.drawable.big_play_button);
            view.setScaleType(ImageView.ScaleType.CENTER);
        }

        @Override
        public void onThumbnailLoaded(YouTubeThumbnailView view, String videoId) {
        }

        @Override
        public void onThumbnailError(YouTubeThumbnailView view, YouTubeThumbnailLoader.ErrorReason errorReason) {
            Log.e(TAG, "Failed to load YouTubThumbnail");
            SantaLog.e(TAG, errorReason.toString());
            view.setImageResource(R.drawable.big_play_button);
        }
    }

    public interface DestinationCardKeyListener {

        void onJumpToDestination(LatLng destination);

        void onFinish();

        boolean onMoveBy(KeyEvent event);
    }

    public interface CardAdapterListener {

        void onOpenStreetView(Destination.StreetView streetView);

        void onPlayVideo(String youtubeId);
    }

    private View.OnClickListener mShowStreetViewListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            mListener.onOpenStreetView((Destination.StreetView) v.getTag());
        }

    };

    private View.OnClickListener mPlayVideoListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            mListener.onPlayVideo((String) v.getTag());
        }

    };

    private View.OnKeyListener mMoveToDestinationListener = new View.OnKeyListener() {

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {

            if (mDestinationCardListener == null) {
                return false;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                    || keyCode == KeyEvent.KEYCODE_BUTTON_A) {

                switch (event.getAction()) {
                    case KeyEvent.ACTION_DOWN:
                        mDestinationCardListener.onJumpToDestination((LatLng) v.getTag());
                        break;
                    case KeyEvent.ACTION_UP:
                        mDestinationCardListener.onFinish();
                        break;
                }
                return false;
            }

            // When a DPAD is pressed, fire an 'onMove' event.
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    //fall through
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    //fall through
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    //fall through
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return mDestinationCardListener.onMoveBy(event);
            }

            return false;
        }
    };

    public void release() {
        if (mThumbnailViewToLoader != null) {
            for (YouTubeThumbnailLoader loader : mThumbnailViewToLoader.values()) {
                if (loader != null) {
                    loader.release();
                }
            }
            mThumbnailViewToLoader.clear();
            mThumbnailViewToLoader = null;
        }
        mThumbnailListener = null;
    }

}