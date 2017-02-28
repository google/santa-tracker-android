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
package com.google.android.apps.santatracker.presentquest.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.apps.santatracker.presentquest.R;
import com.google.android.apps.santatracker.util.FontHelper;

/**
 * Base class for game launching Fragments.
 */
public abstract class PlayGameDialog extends DialogFragment implements View.OnClickListener {

    /**
     * Listener to be notified when the dialog appears and disappears.
     */
    public interface GameDialogListener {

        void onDialogShow();

        void onDialogDismiss();

    }

    private GameDialogListener mListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AppTheme_PresentQuest_DialogFragment);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.dialog_play_minigame, container, false);

        // Play text
        FontHelper.makeLobster((TextView) rootView.findViewById(R.id.minigame_text));

        // Game image
        ImageView imageView = (ImageView) rootView.findViewById(R.id.minigame_image);
        imageView.setImageResource(getImageResourceId());

        // Click listener
        rootView.findViewById(R.id.dialog_root).setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.dialog_root) {
            launchGame();
            dismiss();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (mListener != null) {
            mListener.onDialogDismiss();
        }
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        super.show(manager, tag);

        if (mListener != null) {
            mListener.onDialogShow();
        }
    }

    public void setListener(GameDialogListener listener) {
        mListener = listener;
    }

    public abstract int getImageResourceId();

    public abstract void launchGame();
}

