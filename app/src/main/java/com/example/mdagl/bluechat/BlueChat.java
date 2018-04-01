package com.example.mdagl.bluechat;

import android.app.Application;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.OkHttp3Downloader;


public class BlueChat extends Application{

    private DatabaseReference mUserDatabase;
    private FirebaseAuth mAuth;

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

        mAuth = FirebaseAuth.getInstance();
        mUserDatabase = FirebaseDatabase.getInstance()
                .getReference().child("Users").child(mAuth.getCurrentUser().getUid());

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                mUserDatabase.child("online").onDisconnect().setValue(false);
                mUserDatabase.child("online").setValue(true);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
