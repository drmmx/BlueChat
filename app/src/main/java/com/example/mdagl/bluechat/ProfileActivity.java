package com.example.mdagl.bluechat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView mProfileImage;
    private TextView mProfileName, mProfileStatus, mProfileFriendsCount;
    private Button mProfileSendReqBtn, mDeclineButton;

    private DatabaseReference mUsersDatabase;
    private FirebaseUser mCurrentUser;

    private DatabaseReference mRootRef;

    private String mCurrentState; //request status

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String mUserId = getIntent().getStringExtra("user_id");

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mUserId);
        mUsersDatabase.keepSynced(true);
        mRootRef = FirebaseDatabase.getInstance().getReference();


        mProfileImage = (ImageView) findViewById(R.id.profile_image);
        mProfileName = (TextView) findViewById(R.id.profile_display_name);
        mProfileStatus = (TextView) findViewById(R.id.profile_user_status);
        mProfileFriendsCount = (TextView) findViewById(R.id.profile_total_friends);
        mProfileSendReqBtn = (Button) findViewById(R.id.profile_send_request_btn);
        mDeclineButton = (Button) findViewById(R.id.profile_decline_request_btn);

        mCurrentState = "not_friends";
        mDeclineButton.setVisibility(View.INVISIBLE);
        mDeclineButton.setEnabled(false);

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
                mRootRef.child("Friend_req").child(mCurrentUser.getUid())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                //get request_type value
                                if (dataSnapshot.hasChild(mUserId)) {

                                    String reqType = dataSnapshot.child(mUserId).child("request_type").getValue().toString();
                                    if (reqType.equals("received")) {
                                        mCurrentState = "req_received";
                                        mProfileSendReqBtn.setBackgroundResource(R.drawable.btn_style);
                                        mProfileSendReqBtn.setText(R.string.profile_accept_friend_request);

                                        mDeclineButton.setVisibility(View.VISIBLE);
                                        mDeclineButton.setEnabled(true);
                                    } else if (reqType.equals("sent")) {
                                        mCurrentState = "req_send";
                                        mProfileSendReqBtn.setBackgroundResource(R.drawable.btn_style_dark);
                                        mProfileSendReqBtn.setText(R.string.profile_cancel_friend_request);
                                    }
                                } else {
                                    mRootRef.child("Friends").child(mCurrentUser.getUid())
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {

                                                    if (dataSnapshot.hasChild(mUserId)) {
                                                        mCurrentState = "friends";
                                                        mProfileSendReqBtn.setBackgroundResource(R.drawable.btn_style);
                                                        mProfileSendReqBtn.setText(R.string.profile_unfriend_request);
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {
                                                }
                                            });
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

                    DatabaseReference newNotificationRef = mRootRef.child("Notifications").child(mUserId).push();
                    String newNotificationId = newNotificationRef.getKey();

                    //Create notification
                    HashMap<String, String> notificationData = new HashMap<>();
                    notificationData.put("from", mCurrentUser.getUid());
                    notificationData.put("type", "request");

                    Map<String, Object> requestMap = new HashMap<>();
                    requestMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + mUserId + "/" + "request_type", "sent");
                    requestMap.put("Friend_req/" + mUserId + "/" + mCurrentUser.getUid() + "/" + "request_type", "received");
                    requestMap.put("Notifications/" + mUserId + "/" + newNotificationId, notificationData);

                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if (databaseError == null) {
                            mCurrentState = "req_send";
                            mProfileSendReqBtn.setBackgroundResource(R.drawable.btn_style_dark);
                            mProfileSendReqBtn.setText(R.string.profile_cancel_friend_request);

                            mProfileSendReqBtn.setEnabled(true);
                            } else {
                                String error = databaseError.getMessage();
                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

                //-------------------Cancel Request State---------------------
                if (mCurrentState.equals("req_send")) {
                    Map<String, Object> removeReqMap = new HashMap<>();
                    removeReqMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + mUserId + "/" + "request_type", null);
                    removeReqMap.put("Friend_req/" + mUserId + "/" + mCurrentUser.getUid() + "/" + "request_type", null);

                    mRootRef.updateChildren(removeReqMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError == null) {
                            mProfileSendReqBtn.setEnabled(true);
                            mCurrentState = "not_friends";
                            mProfileSendReqBtn.setBackgroundResource(R.drawable.btn_style);
                            mProfileSendReqBtn.setText(R.string.profile_send_request_btn);
                            } else {
                                String error = databaseError.getMessage();
                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                //-------------------Cancel Received State---------------------
                if (mCurrentState.equals("req_received")) {

                    String currentDate = DateFormat.getDateTimeInstance().format(new Date());

                    Map<String, Object> requestMap = new HashMap<>();
                    requestMap.put("Friends/" + mCurrentUser.getUid() + "/" + mUserId + "/date", currentDate);
                    requestMap.put("Friends/" + mUserId + "/" + mCurrentUser.getUid() + "/date", currentDate);

                    requestMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + mUserId, null);
                    requestMap.put("Friend_req/" + mUserId + "/" + mCurrentUser.getUid(), null);

                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if (databaseError == null) {
                                mProfileSendReqBtn.setEnabled(true);
                                mCurrentState = "friends";
                                mProfileSendReqBtn.setBackgroundResource(R.drawable.btn_style);
                                mProfileSendReqBtn.setText(R.string.profile_unfriend_request);

                                mDeclineButton.setVisibility(View.INVISIBLE);
                                mDeclineButton.setEnabled(false);
                            } else {
                                String error = databaseError.getMessage();
                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

                //-------------------Unfriend State---------------------
                if (mCurrentState.equals("friends")) {

                    Map<String, Object> requestMap = new HashMap<>();
                    requestMap.put("Friends/" + mCurrentUser.getUid() + "/" + mUserId, null);
                    requestMap.put("Friends/" + mUserId + "/" + mCurrentUser.getUid(), null);

                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if (databaseError == null) {
                                mCurrentState = "not_friends";
                                mProfileSendReqBtn.setBackgroundResource(R.drawable.btn_style);
                                mProfileSendReqBtn.setText(R.string.profile_send_request_btn);
                            } else {
                                String error = databaseError.getMessage();
                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                            mProfileSendReqBtn.setEnabled(true);
                        }
                    });
                }
            }
        });

        mDeclineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentState.equals("req_received")) {

                    Map<String, Object> requestMap = new HashMap<>();

                    requestMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + mUserId, null);
                    requestMap.put("Friend_req/" + mUserId + "/" + mCurrentUser.getUid(), null);

                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if (databaseError == null) {
                                mCurrentState = "not_friends";
                                mProfileSendReqBtn.setBackgroundResource(R.drawable.btn_style);
                                mProfileSendReqBtn.setText(R.string.profile_send_request_btn);

                                mDeclineButton.setVisibility(View.INVISIBLE);
                                mDeclineButton.setEnabled(false);
                            } else {
                                String error = databaseError.getMessage();
                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                            mProfileSendReqBtn.setEnabled(true);
                        }
                    });
                }
            }
        });
    }

/*    @Override
    protected void onResume() {
        super.onResume();
        mRootRef.child("Users").child(mCurrentUser.getUid()).child("online").setValue(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRootRef.child("Users").child(mCurrentUser.getUid()).child("online").setValue(false);
    }*/
}
