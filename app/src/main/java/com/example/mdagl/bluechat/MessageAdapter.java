package com.example.mdagl.bluechat;


import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>{

    private List<Messages> mMessagesList;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private DatabaseReference mUserDadabase;

    public MessageAdapter(List<Messages> messagesList) {
        mMessagesList = messagesList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_single_layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, int position) {

        Messages c = mMessagesList.get(position);

        String currentUserId = mAuth.getCurrentUser().getUid();

        String fromUser = c.getFrom();
        String messageType = c.getType();

        if (fromUser.equals(currentUserId)) {
            holder.messageText.setBackgroundResource(R.drawable.message_text_background_white);
            holder.messageText.setTextColor(Color.BLACK);
        } else {
            holder.messageText.setBackgroundResource(R.drawable.message_text_background_blue);
            holder.messageText.setTextColor(Color.WHITE);
        }

        mUserDadabase = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUser);
        mUserDadabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String image = dataSnapshot.child("thumbImage").getValue().toString();
                Picasso.get().load(image).placeholder(R.drawable.default_user)
                        .into(holder.profileImage);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        if (messageType.equals("text")) {
            holder.messageText.setText(c.getMessage());
            holder.messageImage.setVisibility(View.INVISIBLE);
        } else {
            holder.messageText.setVisibility(View.INVISIBLE);
            Picasso.get().load(c.getMessage()).placeholder(R.drawable.default_user).into(holder.messageImage);
            holder.messageImage.refreshDrawableState();
        }
    }

    @Override
    public int getItemCount() {
        return mMessagesList.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        public TextView messageText;
        public CircleImageView profileImage;
        public ImageView messageImage;

        public MessageViewHolder(View itemView) {
            super(itemView);
            messageText = (TextView) itemView.findViewById(R.id.message_text_layout);
            profileImage = (CircleImageView) itemView.findViewById(R.id.message_profile_layout);
            messageImage = (ImageView) itemView.findViewById(R.id.message_image_layout);
        }

    }
}
