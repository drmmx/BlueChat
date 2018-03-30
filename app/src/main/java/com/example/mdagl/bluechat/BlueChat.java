package com.example.mdagl.bluechat;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.OkHttp3Downloader;


public class BlueChat extends Application{

    @Override
    public void onCreate() {
        super.onCreate();

        //Offline using data
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        //Picasso image offline using
        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttp3Downloader(this, Integer.MAX_VALUE));
        Picasso built = builder.build();
        built.setIndicatorsEnabled(true);
        built.setLoggingEnabled(true);
        Picasso.setSingletonInstance(built);
    }
}
