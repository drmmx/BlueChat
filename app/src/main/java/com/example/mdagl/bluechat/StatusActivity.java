package com.example.mdagl.bluechat;

import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class StatusActivity extends AppCompatActivity {

    private static final String TAG = "My tag";

    //Firebase
    private DatabaseReference mStatusDatabase;
    private FirebaseUser mCurrentUser;

    private Toolbar mToolbar;
    private TextInputLayout mStatus;
    private Button mSaveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        //Firebase
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = mCurrentUser.getUid();
        mStatusDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUid);

        mToolbar = (Toolbar) findViewById(R.id.status_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Account Status");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String statusValue = getIntent().getStringExtra("status_value");
        mStatus = (TextInputLayout) findViewById(R.id.status_input);
        mStatus.getEditText().setText(statusValue);
        mSaveBtn = (Button) findViewById(R.id.save_btn);

        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String status = mStatus.getEditText().getText().toString();
                mStatusDatabase.child("status").setValue(status).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(StatusActivity.this, "New status saved", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "Problem with saving changes", task.getException());
                            Toast.makeText(StatusActivity.this, "There was some error in saving Changes", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        });
    }

/*    @Override
    protected void onResume() {
        super.onResume();
        mStatusDatabase.child("online").setValue(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mStatusDatabase.child("online").setValue(false);
    }*/
}
