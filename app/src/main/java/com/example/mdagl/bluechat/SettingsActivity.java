package com.example.mdagl.bluechat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "My tag";
    private static final int GALLERY_PICK = 1;

    //Firebase
    private DatabaseReference mUserDatabase;
    private FirebaseUser mCurrentUser;
    private StorageReference mImageStorage;

    private Toolbar mToolbar;

    private CircleImageView mDisplayImage;
    private TextView mDisplayName;
    private TextView mDisplayStatus;

    private Button mStatusBtn;
    private Button mImageBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mToolbar = (Toolbar) findViewById(R.id.settings_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDisplayImage = (CircleImageView) findViewById(R.id.settings_image);
        mDisplayName = (TextView) findViewById(R.id.settings_display_name);
        mDisplayStatus = (TextView) findViewById(R.id.settings_display_status);
        mStatusBtn = (Button) findViewById(R.id.change_status_btn);
        mImageBtn = (Button) findViewById(R.id.change_image_btn);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = mCurrentUser.getUid();

        mImageStorage = FirebaseStorage.getInstance().getReference();

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUid);

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String name = dataSnapshot.child("name").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String thumbImage = dataSnapshot.child("thumb_image").getValue().toString();

                mDisplayName.setText(name);
                mDisplayStatus.setText(status);

                if (!image.equals("default")) {
                    Picasso.get().load(image).placeholder(R.drawable.default_user).into(mDisplayImage);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });

        mStatusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String statusValue = mDisplayStatus.getText().toString();
                Intent intent = new Intent(SettingsActivity.this, StatusActivity.class);
                intent.putExtra("status_value", statusValue);
                startActivity(intent);
            }
        });

        mImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "Select Image"), GALLERY_PICK);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {

            Uri imageUri = data.getData();

            // start cropping activity for pre-acquired image saved on the device
            CropImage.activity(imageUri)
                    .setAspectRatio(1, 1)
                    .start(SettingsActivity.this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {

                //crop image uri
                Uri resultUri = result.getUri();

                //for image name
                String currentUserId = mCurrentUser.getUid();

                //Compress bitmap thumb and put to Database
                File thumbFile = new File(resultUri.getPath());
                Bitmap thumbBitmap = null;
                try {
                    thumbBitmap = new Compressor(this)
                            .setMaxWidth(200)
                            .setMaxHeight(200)
                            .setQuality(75)
                            .compressToBitmap(thumbFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                final byte[] thumbByte = baos.toByteArray();


                //save image and thumb in database as currentUserId.jpg
                StorageReference filepath = mImageStorage.child("profile_images").child(currentUserId + ".jpg");
                final StorageReference thumbFilepath = mImageStorage.child("profile_images").child("thumb").child(currentUserId + ".jpg");

                filepath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                        if (task.isSuccessful()) {

                            final String downloadUrl = task.getResult().getDownloadUrl().toString();


                            UploadTask uploadTask = thumbFilepath.putBytes(thumbByte);
                            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumbTask) {

                                    String thumbDownloadUrl = thumbTask.getResult().getDownloadUrl().toString();

                                    if (thumbTask.isSuccessful()) {
                                        Map<String, Object> updateHashMap = new HashMap<>();
                                        updateHashMap.put("image", downloadUrl);
                                        updateHashMap.put("thumb_image", thumbDownloadUrl);

                                        mUserDatabase.updateChildren(updateHashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(SettingsActivity.this, "Success uploading image", Toast.LENGTH_LONG).show();
                                                } else {
                                                    Toast.makeText(SettingsActivity.this, "Error uploading image", Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });

                                    } else {
                                        Toast.makeText(SettingsActivity.this, "Error uploading thumbnail", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });

                        } else {
                            Toast.makeText(SettingsActivity.this, "Error save image in database", Toast.LENGTH_LONG).show();
                        }
                    }
                });

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}
