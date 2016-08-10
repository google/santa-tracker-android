package com.github.amlcurran.showcaseview.targets;

import android.app.Activity;
import android.view.View;
import android.view.ViewParent;
import android.view.Window;

/**
 * Created by Alex on 27/10/13.
 */
class AppCompatReflector implements Reflector {

    private Activity mActivity;

    public AppCompatReflector(Activity activity) {
        mActivity = activity;
    }

    @Override
    public ViewParent getActionBarView() {
        Window window = mActivity.getWindow();
        return getHomeButton().getParent().getParent();
    }

    @Override
    public View getHomeButton() {
        View homeButton = mActivity.findViewById(android.R.id.home);
        if (homeButton == null) {
            throw new RuntimeException(
                    "insertShowcaseViewWithType cannot be used when the theme " +
                            "has no ActionBar");
        }
        return homeButton;
    }

    @Override
    public void showcaseActionItem(int itemId) {

    }
}
