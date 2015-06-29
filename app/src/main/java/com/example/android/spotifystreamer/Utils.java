package com.example.android.spotifystreamer;

import android.content.Context;
import android.widget.Toast;

import java.util.List;

import kaaes.spotify.webapi.android.models.Image;

/**
 * Created by achiang on 6/29/15.
 */
public final class Utils {
    private Utils(){}

    public static void displayToast(Context context, int resId, int duration, int gravity){
        Toast toast = Toast.makeText(context,resId,duration);
        toast.setGravity(gravity,0,0);
        toast.show();
    }

    public static String findImageForListItem(List<Image> images){
        Image thumbnail = images.get(0);
        for (int i = 1; i < images.size(); ++i) {
            Image tmp = images.get(i);
            if (tmp.width < thumbnail.width && tmp.width >= 200) {
                thumbnail = tmp;
            }
        }
        return thumbnail.url;
    }
}
