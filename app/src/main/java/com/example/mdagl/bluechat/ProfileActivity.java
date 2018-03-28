package com.example.mdagl.bluechat;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;

public class ProfileActivity extends AppCompatActivity {

    private ImageView mProfileImage;
    private TextView mProfileName, mProfileStatus, mProfileFriendsCount;
    private Button mProfileSendReqBtn;

    private DatabaseReference mUsersDatabase;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mFriendsReqDatabase;
    private DatabaseReference mFriendsDatabase;

    String mUserId;
    private String mCurrentState; //request status

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mUserId = getIntent().getStringExtra("user_id");

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mUserId);
        mFriendsReqDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mFriendsDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");

        mProfileImage = (ImageView) findViewById(R.id.profile_image);
        mProfileName = (TextView) findViewById(R.id.profile_display_name);
        mProfileStatus = (TextView) findViewById(R.id.profile_user_status);
        mProfileFriendsCount = (TextView) findViewById(R.id.profile_total_friends);
        mProfileSendReqBtn = (Button) findViewById(R.id.profile_send_request_btn);

        mCurrentState = "not_friends";

        mUsersDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String displayName = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                final String image = dataSnapshot.child("image").getValue().toString();

                mProfileName.setText(displayName);
                mProfileStatus.setText(status);

                if (!image.equals("default")) {
                    Picasso.get().load(image).placeholder(R.drawable.default_user).into(mProfileImage);
                }

                //----------------------Friends List / Requests Feature-------------------
                mFriendsReqDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        //get request_type value
                        if (dataSnapshot.hasChild(mUserId)) {

                            String reqType = dataSnapshot.child(mUserId).child("request_type").getValue().toString();
                            if (reqType.equals("received")) {
                                mCurrentState = "req_received";
                                mProfileSendReqBtn.setBackgroundResource(R.drawable.btn_style);
                                mProfileSendReqBtn.setText("Accept Friend Request");
                            } else if (reqType.equals("sent")) {
                                mCurrentState = "req_send";
                                mProfileSendReqBtn.setBackgroundResource(R.drawable.btn_style_dark);
                                mProfileSendReqBtn.setText("Cancel Friend Request");
                            }
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mProfileSendReqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mProfileSendReqBtn.setEnabled(false);


                //-------------------Not Friends State---------------------
                if (mCurrentState.equals("not_friends")) {
                    mFriendsReqDatabase.child(mCurrentUser.getUid()).child(mUserId).child("request_type").setValue("sent")
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {

                                        mFriendsReqDatabase.child(mUserId).child(mCurrentUser.getUid()).child("request_type")
                                                .setValue("received").addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {

                                                mCurrentState = "req_send";
                                                mProfileSendReqBtn.setBackgroundResource(R.drawable.btn_style_dark);
                                                mProfileSendReqBtn.setText("Cancel Friend Request");

                                            }
                                        });
                                    } else {
                                        Toast.makeText(ProfileActivity.this, "Failed Sending Request", Toast.LENGTH_SHORT).show();
                                    }
                                    mProfileSendReqBtn.setEnabled(true);
                                }
                            });
                }

                //-------------------Cancel Request State---------------------
                if (mCurrentState.equals("req_send")) {
                    mFriendsReqDatabase.child(mCurrentUser.getUid()).child(mUserId)
                            .removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            mFriendsReqDatabase.child(mUserId).child(mCurrentUser.getUid())
                                    .removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mProfileSendReqBtn.setEnabled(true);
                                    mCurrentState = "not_friends";
                                    mProfileSendReqBtn.setBackgroundResource(R.drawable.btn_style);
                                    mProfileSendReqBtn.setText("Send Request");

                                }
                            });

                        }
                    });
                }
                //-------------------Cancel Received State---------------------
                if (mCurrentState.equals("req_received")) {

                    final String currentDate = DateFormat.getDateTimeInstance().format(new Date());

                    mFriendsDatabase.child(mCurrentUser.getUid()).child(mUserId).setValue(currentDate)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mFriendsDatabase.child(mUserId).child(mCurrentUser.getUid()).setValue(currentDate)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {

                                                    mFriendsReqDatabase.child(mCurrentUser.getUid()).child(mUserId)
                                                            .removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {

                                                            mFriendsReqDatabase.child(mUserId).child(mCurrentUser.getUid())
                                                                    .removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void aVoid) {

                                                                    mProfileSendReqBtn.setEnabled(true);
                                                                    mCurrentState = "friends";
                                                                    mProfileSendReqBtn.setBackgroundResource(R.drawable.btn_style);
                                                                    mProfileSendReqBtn.setText("Unfriend");

                                                                }
                                                            });

                                                        }
                                                    });
                                                }
                                            });
                                }
                            });

                }
            }
        });
    }
}
