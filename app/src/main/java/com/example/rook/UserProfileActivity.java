package com.example.rook;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvProfileName, tvProfileEmail, tvProfileProjects, tvProfileTests, tvProfileSuccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        dbHelper = new DatabaseHelper(this);
        NavigationUtils.setupAppChrome(this, "Profile", false);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileProjects = findViewById(R.id.tvProfileProjects);
        tvProfileTests = findViewById(R.id.tvProfileTests);
        tvProfileSuccess = findViewById(R.id.tvProfileSuccess);

        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                // Log out from Firebase Authentication
                FirebaseAuth.getInstance().signOut();

                // Return to login and clear task stack
                Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindProfileData();
    }

    private void bindProfileData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            tvProfileName.setText(name != null && !name.trim().isEmpty() ? name : "Signed-in user");
            tvProfileEmail.setText(user.getEmail() != null ? user.getEmail() : "No email available");
        } else {
            tvProfileName.setText("Guest user");
            tvProfileEmail.setText("Not signed in");
        }

        int testCount = dbHelper.getTestResultCount();
        if (testCount == 0) {
            testCount = dbHelper.getHistoryCount();
        }
        tvProfileProjects.setText(String.valueOf(dbHelper.getProjectCount()));
        tvProfileTests.setText(String.valueOf(testCount));
        tvProfileSuccess.setText(String.format(Locale.US, "%.0f%%", dbHelper.getSuccessRate()));
    }
}
