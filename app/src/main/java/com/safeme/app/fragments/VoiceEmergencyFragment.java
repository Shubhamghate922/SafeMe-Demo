package com.safeme.app.fragments;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.R;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.safeme.app.utils.PermissionHelper;
import com.safeme.app.utils.SecureTokenManager;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceEmergencyFragment extends Fragment {

    private SecureTokenManager tokenManager;
    private SpeechRecognizer speechRecognizer;
    private TextInputEditText etKeyword;
    private TextView tvStatus;
    private MaterialSwitch switchEnabled;

    private final ActivityResultLauncher<String> micPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startExplicitListening();
                } else {
                    tvStatus.setText("Microphone permission denied. Voice recognition cannot start.");
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_voice_emergency, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tokenManager = SecureTokenManager.getInstance(requireContext());

        switchEnabled = view.findViewById(R.id.switchVoiceTrigger);
        etKeyword = view.findViewById(R.id.etVoiceKeyword);
        tvStatus = view.findViewById(R.id.tvVoiceStatus);

        switchEnabled.setChecked(tokenManager.isVoiceTriggerEnabled());
        etKeyword.setText(tokenManager.getVoiceKeyword());

        switchEnabled.setOnCheckedChangeListener((btn, checked) -> {
            tokenManager.setVoiceTriggerEnabled(checked);
            Toast.makeText(requireContext(), checked ? "Voice trigger enabled." : "Voice trigger disabled.", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btnSaveVoiceKeyword).setOnClickListener(v -> {
            String kw = etKeyword.getText() != null ? etKeyword.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
            if (kw.length() < 3) {
                etKeyword.setError("Enter at least 3 characters");
                return;
            }
            tokenManager.setVoiceKeyword(kw);
            Toast.makeText(requireContext(), "Emergency keyword saved.", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btnTestVoiceTrigger).setOnClickListener(v -> {
            if (!PermissionHelper.hasMicrophonePermission(requireContext())) {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            } else {
                startExplicitListening();
            }
        });
    }

    private void startExplicitListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            tvStatus.setText("Speech recognition service is not available on this device.");
            return;
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                tvStatus.setText("Listening… Speak your emergency phrase clearly.");
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {
                tvStatus.setText("Processing speech…");
            }
            @Override public void onError(int error) {
                tvStatus.setText("Speech recognition ended or timed out (code " + error + "). Tap to try again.");
            }
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String heard = matches.get(0).toLowerCase(Locale.ROOT);
                    String keyword = tokenManager.getVoiceKeyword().toLowerCase(Locale.ROOT);
                    tvStatus.setText("Heard: \"" + heard + "\"");
                    if (heard.contains(keyword) && tokenManager.isVoiceTriggerEnabled()) {
                        Bundle args = new Bundle();
                        args.putBoolean("autoTrigger", true);
                        NavHostFragment.findNavController(VoiceEmergencyFragment.this)
                                .navigate(R.id.emergencySosFragment, args);
                    }
                }
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizer.startListening(intent);
    }

    @Override
    public void onDestroyView() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        super.onDestroyView();
    }
}
