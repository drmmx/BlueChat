package com.example.mdagl.bluechat;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


public class FriendsFragment extends Fragment {

    private RecyclerView mFriendsList;

    private DatabaseReference mFriendsDatabase;
    private DatabaseReference mUsersDatabase;
    private FirebaseAuth mAuth;

    private String mCurrentUserId;
    private View mMainView;

    public FriendsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mMainView = inflater.inflate(R.layout.fragment_friends, container, false);

        mFriendsList = (RecyclerView) mMainView.findViewById(R.id.friends_list);
        mAuth = FirebaseAuth.getInstance();

        mCurrentUserId = mAuth.getCurrentUser().getUid();
        mFriendsDatabase = FirebaseDatabase.getInstance().getReference().child("Friends").child(mCurrentUserId);
        mFriendsDatabase.keepSynced(true);
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mUsersDatabase.keepSynced(true);

        mFriendsList.setHasFixedSize(true);
        mFriendsList.setLayoutManager(new LinearLayoutManager(getContext()));

        return mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();


        FirebaseRecyclerOptions<Friends> options = new FirebaseRecyclerOptions.Builder<Friends>()
                .setQuery(mFriendsDatabase, Friends.class)
                .build();

        FirebaseRecyclerAdapter firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Friends, FriendsFragment.FriendsViewHolder>(options) {

            @Override
            public FriendsFragment.FriendsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_single_layout, parent, false);

                return new FriendsFragment.FriendsViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull final FriendsFragment.FriendsViewHolder friendsViewHolder, int position, @NonNull Friends friends) {
                friendsViewHolder.setDate(friends.getDate());
/*                friendsViewHolder.setStatus(friends.getStatus());
                friendsViewHolder.setUserImage(friends.getThumbImage());*/

                final String userId = getRef(position).getKey();

                mUsersDatabase.child(userId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        String userName = dataSnapshot.child("name").getValue().toString();
                        String userThumb = dataSnapshot.child("thumbImage").getValue().toString();
                        String userOnline = dataSnapshot.child("online").getValue().toString();

                        friendsViewHolder.setName(userName);
                        friendsViewHolder.setUserImage(userThumb);
                        friendsViewHolder.setUserOnlineStatus(userOnline);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                friendsViewHolder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

/*                        Intent profileIntent = new Intent(FriendsFragment.this, ProfileActivity.class);
                        profileIntent.putExtra("user_id", userId);
                        startActivity(profileIntent);*/
                    }
                });
            }
        };
        mFriendsList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }

    public class FriendsViewHolder extends RecyclerView.ViewHolder {

        View mView;
        Context mContext;

        FriendsViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setDate(String date) {
            TextView friendDateView = (TextView) mView.findViewById(R.id.user_single_status);
            friendDateView.setText(date);
        }

        public void setName(String name) {
            TextView userNameView = (TextView) mView.findViewById(R.id.user_single_name);
            userNameView.setText(name);
        }

        public void setUserImage(String thumbImage) {

            CircleImageView userImageView = (CircleImageView) mView.findViewById(R.id.user_single_circle_image_view);
            Picasso.get().load(thumbImage).placeholder(R.drawable.default_user).into(userImageView);
        }

        public void setUserOnlineStatus(String onlineStatus) {
            TextView userOnlineView = (TextView) mView.findViewById(R.id.user_online_status);

            if (onlineStatus.equals("false")) {
                userOnlineView.setText(R.string.offline_status);
                userOnlineView.setTextColor(getResources().getColor(R.color.secondaryTextColor));
            } else {
                userOnlineView.setText(R.string.online_status);
                userOnlineView.setTextColor(getResources().getColor(R.color.colorPrimary));
            }
        }
    }

}
