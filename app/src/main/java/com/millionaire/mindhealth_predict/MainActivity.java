package com.millionaire.mindhealth_predict;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private EditText etAge, etCountry, etScreenTime, etUnlocks, etStudyHours;
    private Spinner spinnerGender, spinnerAcademic, spinnerPlatform;
    private RadioGroup rgStressLevel;
    private Button btnSubmit, btnClear;
    private TextView txtFinalResult;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layouts to XML components
        etAge = findViewById(R.id.etAge);
        etCountry = findViewById(R.id.etCountry);
        etScreenTime = findViewById(R.id.etScreenTime);
        etUnlocks = findViewById(R.id.etUnlocks);
        etStudyHours = findViewById(R.id.etStudyHours);

        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerAcademic = findViewById(R.id.spinnerAcademic);
        spinnerPlatform = findViewById(R.id.spinnerPlatform);

        rgStressLevel = findViewById(R.id.rgStressLevel);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnClear = findViewById(R.id.btnClear);
        txtFinalResult = findViewById(R.id.txtFinalResult);

        setupDropdownMenus();

        // Target your live main domain URL ending with a trailing slash "/"
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://mental-health-ai-api.onrender.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateMentalHealthSignal();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFormFields();
            }
        });
    }

    private void calculateMentalHealthSignal() {
        String ageStr = etAge.getText().toString().trim();
        String countryStr = etCountry.getText().toString().trim();
        String screenTimeStr = etScreenTime.getText().toString().trim();
        String unlocksStr = etUnlocks.getText().toString().trim();
        String studyHoursStr = etStudyHours.getText().toString().trim();

        if (ageStr.isEmpty() || countryStr.isEmpty() || screenTimeStr.isEmpty() || unlocksStr.isEmpty() || studyHoursStr.isEmpty()) {
            txtFinalResult.setText("⚠️ Error: Please fill out all metric fields!");
            return;
        }

        int selectedRadioId = rgStressLevel.getCheckedRadioButtonId();
        if (selectedRadioId == -1) {
            txtFinalResult.setText("⚠️ Error: Please select a perceived stress level!");
            return;
        }
        RadioButton selectedStressButton = findViewById(selectedRadioId);
        String stressTextValue = selectedStressButton.getText().toString().trim();

        // Translate short labels into the exact category text keys expected by model encoders
        if (stressTextValue.equals("Med")) {
            stressTextValue = "Medium";
        } else if (stressTextValue.equals("V.High")) {
            stressTextValue = "Very High";
        }

        int ageValue = Integer.parseInt(ageStr);
        double screenTimeValue = Double.parseDouble(screenTimeStr);
        int unlocksValue = Integer.parseInt(unlocksStr);
        double studyHoursValue = Double.parseDouble(studyHoursStr);

        String genderValue = spinnerGender.getSelectedItem().toString().trim();
        String academicValue = spinnerAcademic.getSelectedItem().toString().trim();
        String platformValue = spinnerPlatform.getSelectedItem().toString().trim();

        // Safeguard fallbacks for menu prompts
        if (genderValue.equals("Select Gender")) genderValue = "Female";
        if (academicValue.equals("Select Academic Level")) academicValue = "Undergraduate";
        if (platformValue.equals("Select Platform")) platformValue = "Instagram";

        txtFinalResult.setText("Analyzing details with cloud model...");

        PredictionRequest datasetPayload = new PredictionRequest(
                ageValue, genderValue, countryStr, academicValue, platformValue,
                screenTimeValue, unlocksValue, studyHoursValue, stressTextValue
        );

        apiService.analyzeData(datasetPayload).enqueue(new Callback<PredictionResponse>() {
            @Override
            public void onResponse(Call<PredictionResponse> call, Response<PredictionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 1. Fetch the raw wellbeing metric float output value from the model
                    double rawWellbeingScore = response.body().getPredictedMentalHealthScore();

                    // 2. Invert the math so the UI presents a logical "Stress / Risk Level" out of 10
                    double stressRiskScore = 10.0 - rawWellbeingScore;
                    if (stressRiskScore < 0) stressRiskScore = 0; // Safeguard bounds
                    if (stressRiskScore > 10) stressRiskScore = 10;

                    // Round the value cleanly to 2 decimal places
                    double roundedStressScore = Math.round(stressRiskScore * 100.0) / 100.0;

                    // 3. Evaluate the output to print clear user action guidelines
                    String displayMessage;
                    if (roundedStressScore >= 7.0) {
                        displayMessage = "Risk Profile: High Stress (" + roundedStressScore + "/10)\nRecommendation: Elevated markers detected. Consider taking a digital screen break!";
                    } else if (roundedStressScore >= 4.0) {
                        displayMessage = "Risk Profile: Moderate Stress (" + roundedStressScore + "/10)\nRecommendation: Minor routine imbalances. Try to dedicate time to offline activities.";
                    } else {
                        displayMessage = "Risk Profile: Stable Balance (" + roundedStressScore + "/10)\nRecommendation: Healthy daily habits. Maintain your current routine profile!";
                    }

                    // Render the clear textual text string into your brown banner card component box
                    txtFinalResult.setText(displayMessage);

                } else {
                    txtFinalResult.setText("Server Error Code: " + response.code() + "\nVerify layout framework parameter properties.");
                }
            }


            @Override
            public void onFailure(Call<PredictionResponse> call, Throwable t) {
                txtFinalResult.setText("Connection failed: " + t.getLocalizedMessage());
            }
        });
    }

    private void setupDropdownMenus() {
        String[] genders = {"Select Gender", "Male", "Female", "Not to be Say"};
        ArrayAdapter<String> adapterGender = new ArrayAdapter<>(this, R.layout.custom_spinner_item, genders);
        adapterGender.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapterGender);

        String[] levels = {"Select Academic Level", "High School", "Undergraduate", "Graduate"};
        ArrayAdapter<String> adapterAcademic = new ArrayAdapter<>(this, R.layout.custom_spinner_item, levels);
        adapterAcademic.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAcademic.setAdapter(adapterAcademic);

        String[] platforms = {"Select Platform", "Facebook", "LinkedIn", "Instagram", "Snapchat", "Twitter", "YouTube", "TikTok", "WhatsApp"};
        ArrayAdapter<String> adapterPlatform = new ArrayAdapter<>(this, R.layout.custom_spinner_item, platforms);
        adapterPlatform.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPlatform.setAdapter(adapterPlatform);
    }

    private void resetFormFields() {
        etAge.setText("");
        etCountry.setText("");
        etScreenTime.setText("");
        etUnlocks.setText("");
        etStudyHours.setText("");
        spinnerGender.setSelection(0);
        spinnerAcademic.setSelection(0);
        spinnerPlatform.setSelection(0);
        rgStressLevel.clearCheck();
        txtFinalResult.setText("Your score will appear here");
    }
}
