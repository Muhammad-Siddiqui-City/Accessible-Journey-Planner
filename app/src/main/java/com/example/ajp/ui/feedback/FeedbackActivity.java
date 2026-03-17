package com.example.ajp.ui.feedback;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ajp.R;
import com.example.ajp.databinding.ActivityFeedbackBinding;
import com.example.ajp.ui.main.MainActivity;
import com.example.ajp.utils.LocaleHelper;
import com.example.ajp.utils.SettingsPrefs;

/**
 * Feedback screen (rating, comment). Add in Commit 16.
 * PURPOSE: Placeholder feedback form; apply locale and high-contrast theme; back to MainActivity.
 * WHY: Linked from Settings; no backend – local only per project rules.
 * ISSUES: None.
 */
// AI Generated
public class FeedbackActivity extends AppCompatActivity {

    private ActivityFeedbackBinding binding;
    private int rating = 0;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyFull(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (SettingsPrefs.get(getApplicationContext()).isHighContrast()) {
            setTheme(R.style.Theme_AJP_HighContrast);
        }
        super.onCreate(savedInstanceState);
        binding = ActivityFeedbackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String[] issueTypes = {
                getString(R.string.crowding_information),
                getString(R.string.accessibility),
                getString(R.string.route_suggestions),
                getString(R.string.app_issues),
                getString(R.string.other)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, issueTypes);
        binding.issueTypeSpinner.setAdapter(adapter);

        binding.star1.setOnClickListener(v -> setRating(1));
        binding.star2.setOnClickListener(v -> setRating(2));
        binding.star3.setOnClickListener(v -> setRating(3));
        binding.star4.setOnClickListener(v -> setRating(4));
        binding.star5.setOnClickListener(v -> setRating(5));

        String[] tags = {"Easy to use", "Accurate times", "Good accessibility", "Helpful routes", "Nice design", "Fast app"};
        for (String tag : tags) {
            TextView chip = new TextView(this);
            chip.setText(tag);
            int pad = (int) (24 * getResources().getDisplayMetrics().density);
            chip.setPadding(pad, pad / 2, pad, pad / 2);
            chip.setBackgroundResource(R.drawable.glass_card);
            chip.setTextSize(14);
            chip.setOnClickListener(v -> {
                String current = binding.comments.getText().toString();
                binding.comments.setText(current.isEmpty() ? tag + "." : current + " " + tag + ".");
            });
            binding.quickTagsContainer.addView(chip);
        }

        binding.submit.setOnClickListener(v -> {
            if (rating > 0) {
                sendFeedbackEmail();
                binding.formContent.setVisibility(View.GONE);
                binding.thankYouContent.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, R.string.rate_experience, Toast.LENGTH_SHORT).show();
            }
        });

        binding.returnHome.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
    }

    private void setRating(int r) {
        rating = r;
        binding.star1.setAlpha(r >= 1 ? 1f : 0.3f);
        binding.star2.setAlpha(r >= 2 ? 1f : 0.3f);
        binding.star3.setAlpha(r >= 3 ? 1f : 0.3f);
        binding.star4.setAlpha(r >= 4 ? 1f : 0.3f);
        binding.star5.setAlpha(r >= 5 ? 1f : 0.3f);
    }

    /** Opens email chooser with feedback pre-filled to muhammad.siddiqui.2@city.ac.uk */
    private void sendFeedbackEmail() {
        String issueType = "";
        Object selected = binding.issueTypeSpinner.getSelectedItem();
        if (selected != null) issueType = selected.toString();
        String comments = binding.comments.getText() != null ? binding.comments.getText().toString().trim() : "";
        String body = "Rating: " + rating + "/5\n"
                + "Issue type: " + issueType + "\n\n"
                + "Comments:\n" + comments;
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"muhammad.siddiqui.2@city.ac.uk"});
        i.putExtra(Intent.EXTRA_SUBJECT, "AJP Feedback");
        i.putExtra(Intent.EXTRA_TEXT, body);
        Intent chooser = Intent.createChooser(i, getString(R.string.send_feedback));
        if (getPackageManager().resolveActivity(chooser, 0) != null) {
            startActivity(chooser);
        } else {
            Toast.makeText(this, R.string.no_email_app, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
