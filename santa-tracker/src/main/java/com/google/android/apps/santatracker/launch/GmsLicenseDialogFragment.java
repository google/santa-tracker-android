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

package com.google.android.apps.santatracker.launch;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.apps.santatracker.R;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;


public class GmsLicenseDialogFragment extends DialogFragment {

    public static GmsLicenseDialogFragment newInstance() {
        return new GmsLicenseDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gms_license_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ListView list = (ListView) view.findViewById(R.id.list);
        list.setAdapter(new LicenseAdapter(view.getContext()));
    }

    private static class ViewHolder {
        TextView text;
    }

    private static class LicenseAdapter extends BaseAdapter {

        private final ArrayList<String> mTexts = new ArrayList<>();

        public LicenseAdapter(Context context) {
            final String info = GoogleApiAvailability.getInstance()
                    .getOpenSourceSoftwareLicenseInfo(context);
            if (info != null) {
                BufferedReader reader = new BufferedReader(new StringReader(info));
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        mTexts.add(line);
                    }
                } catch (IOException e) {
                    // Ignore
                } finally {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }

        @Override
        public int getCount() {
            return mTexts.size();
        }

        @Override
        public Object getItem(int position) {
            return mTexts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup container) {
            ViewHolder holder;
            if (view == null) {
                view = LayoutInflater.from(container.getContext())
                        .inflate(R.layout.item_gms_license_dialog, container, false);
                holder = new ViewHolder();
                holder.text = (TextView) view.findViewById(R.id.text);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            holder.text.setText((String) getItem(position));
            return view;
        }

    }

}
