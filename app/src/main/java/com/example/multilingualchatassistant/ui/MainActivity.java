package com.example.multilingualchatassistant.ui;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.multilingualchatassistant.R;
import com.example.multilingualchatassistant.data.AppDatabase;
import com.example.multilingualchatassistant.data.MessageDao;
import com.example.multilingualchatassistant.data.MessageEntity;
import com.example.multilingualchatassistant.network.GiphyApi;
import com.example.multilingualchatassistant.network.GiphyResponse;
import com.example.multilingualchatassistant.network.RetrofitClient;
import com.example.multilingualchatassistant.nlp.IntentDetector;
import com.example.multilingualchatassistant.nlp.LanguageDetector;
import com.example.multilingualchatassistant.nlp.ReplyEngine;
import com.example.multilingualchatassistant.nlp.SlangProcessor;
import com.example.multilingualchatassistant.nlp.ToneDetector;
import com.example.multilingualchatassistant.nlp.TranslatorHelper;
import com.example.multilingualchatassistant.overlay.FloatingBubbleService;
import com.example.multilingualchatassistant.util.GifQueryBuilder;
import com.example.multilingualchatassistant.util.KeywordExtractor;
import com.example.multilingualchatassistant.util.LanguageUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    // UI
    private EditText etIncoming, etReply;
    private TextView tvTranslatedForUser, tvReplyPreviewUser, tvReplyPreviewSend;
    private TextView tvDetectedLanguage, tvDetectedToneIntent;
    private Button btnAnalyzeTranslate, btnGenerateReply, btnClearHistory, btnGenerateGif;
    private Button btnSpeakIncoming, btnSpeakReply;
    private Button btnCopyReply, btnCopyGif;
    private RecyclerView rvHistory;
    private Spinner spTone;
    private ImageView ivGifPreview;
    private Switch switchGifOnly;

    // Database
    private HistoryAdapter adapter;
    private MessageDao messageDao;

    // Language & tone
    private String detectedOriginalLang = "unknown";
    private String selectedToneCode = "auto";

    // Copy buffer
    private String lastReplyToSend = "";
    private String lastGifUrl = null;

    // Giphy
    private GiphyApi giphyApi;
    private static final String GIPHY_API_KEY = "guZjkLGwI9IGzQSQqiZ0NOdR07hkwBbz";

    // Speech
    private SpeechRecognizer speechRecognizerIncoming;
    private SpeechRecognizer speechRecognizerReply;
    private Intent speechIntentIncoming;
    private Intent speechIntentReply;

    private static final int REQ_RECORD_AUDIO = 1001;
    private static final int REQ_OVERLAY_PERMISSION = 2002;

    // ✅ NEW: notification permission request code (Android 13+)
    private static final int REQ_POST_NOTIFICATIONS = 3001;

    // ✅ NEW: prevents bubble starting while overlay permission screen is open
    private boolean overlayPermissionRequestInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageDao = AppDatabase.getInstance(this).messageDao();
        giphyApi = RetrofitClient.getClient().create(GiphyApi.class);

        bindViews();
        setupToneSpinner();
        setupSpeechRecognizers();

        adapter = new HistoryAdapter();
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        loadHistory();

        switchGifOnly.setOnCheckedChangeListener(
                (buttonView, isChecked) -> adapter.setGifOnlyMode(isChecked)
        );

        setupAnalyzeButton();
        setupGenerateReplyButton();
        setupGifButton();
        setupClearHistoryButton();
        setupSpeechButtons();
        setupCopyButtons();

        // ✅ NEW: required on Android 13+ so the foreground service notification can appear
        ensureNotificationPermission();
    }

    // ✅ NEW
    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_POST_NOTIFICATIONS
                );
            }
        }
    }

    private void bindViews() {
        etIncoming = findViewById(R.id.etIncoming);
        etReply = findViewById(R.id.etReply);

        tvTranslatedForUser = findViewById(R.id.tvTranslatedForUser);
        tvReplyPreviewUser = findViewById(R.id.tvReplyPreviewUser);
        tvReplyPreviewSend = findViewById(R.id.tvReplyPreviewSend);
        tvDetectedLanguage = findViewById(R.id.tvDetectedLanguage);
        tvDetectedToneIntent = findViewById(R.id.tvDetectedToneIntent);

        btnAnalyzeTranslate = findViewById(R.id.btnAnalyzeTranslate);
        btnGenerateReply = findViewById(R.id.btnGenerateReply);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        btnGenerateGif = findViewById(R.id.btnGenerateGif);

        btnSpeakIncoming = findViewById(R.id.btnSpeakIncoming);
        btnSpeakReply = findViewById(R.id.btnSpeakReply);

        btnCopyReply = findViewById(R.id.btnCopyReply);
        btnCopyGif = findViewById(R.id.btnCopyGif);

        rvHistory = findViewById(R.id.rvHistory);
        spTone = findViewById(R.id.spTone);

        ivGifPreview = findViewById(R.id.ivGifPreview);
        switchGifOnly = findViewById(R.id.switchGifOnly);
    }

    // ---------------- TONE SPINNER ----------------
    private void setupToneSpinner() {
        ArrayAdapter<CharSequence> adapterTone =
                ArrayAdapter.createFromResource(
                        this,
                        R.array.tone_display,
                        android.R.layout.simple_spinner_item
                );

        adapterTone.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTone.setAdapter(adapterTone);
        spTone.setSelection(0);

        spTone.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String label = parent.getItemAtPosition(position).toString().toLowerCase();

                if (label.contains("friendly")) selectedToneCode = "friendly";
                else if (label.contains("formal")) selectedToneCode = "formal";
                else if (label.contains("casual")) selectedToneCode = "casual";
                else if (label.contains("humorous")) selectedToneCode = "humorous";
                else if (label.contains("empathetic")) selectedToneCode = "empathetic";
                else selectedToneCode = "auto";
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedToneCode = "auto";
            }
        });
    }

    // --------------- ANALYSE + TRANSLATE ----------------
    private void setupAnalyzeButton() {
        btnAnalyzeTranslate.setOnClickListener(v -> {

            String incoming = etIncoming.getText().toString().trim();

            if (incoming.isEmpty()) {
                Toast.makeText(this, "Enter a message first", Toast.LENGTH_SHORT).show();
                return;
            }

            tvDetectedLanguage.setText("Detecting language...");
            tvTranslatedForUser.setText("...");

            LanguageDetector.detectLanguage(incoming, new LanguageDetector.Callback() {
                @Override
                public void onResult(@NonNull String langCode) {

                    detectedOriginalLang = langCode;
                    String langName = LanguageUtils.codeToName(langCode);

                    runOnUiThread(() ->
                            tvDetectedLanguage.setText("Detected: " + langName + " (" + langCode + ")")
                    );

                    String normalized = SlangProcessor.normalizeInput(langCode, incoming);

                    TranslatorHelper.translate(
                            langCode,
                            "en",
                            normalized,
                            new TranslatorHelper.TranslateCallback() {
                                @Override
                                public void onTranslated(@NonNull String translated) {
                                    runOnUiThread(() -> tvTranslatedForUser.setText(translated));
                                }

                                @Override
                                public void onError(@NonNull Exception e) {
                                    runOnUiThread(() -> tvTranslatedForUser.setText(incoming));
                                }
                            });
                }

                @Override
                public void onError(@NonNull Exception e) {
                    detectedOriginalLang = "unknown";
                    runOnUiThread(() -> {
                        tvDetectedLanguage.setText("Detected language: unknown");
                        tvTranslatedForUser.setText(incoming);
                    });
                }
            });
        });
    }

    // --------------- GENERATE REPLY ----------------
    private void setupGenerateReplyButton() {
        btnGenerateReply.setOnClickListener(v -> {

            String incoming = etIncoming.getText().toString().trim();
            String userReply = etReply.getText().toString().trim();

            if (incoming.isEmpty() || userReply.isEmpty()) {
                Toast.makeText(this, "Enter incoming message and reply", Toast.LENGTH_SHORT).show();
                return;
            }

            btnGenerateReply.setEnabled(false);
            btnGenerateReply.setText("Generating...");

            ReplyEngine.generateReplyAsync(
                    incoming,
                    userReply,
                    "en",
                    detectedOriginalLang,
                    selectedToneCode,
                    new ReplyEngine.Callback() {
                        @Override
                        public void onReplyReady(@NonNull ReplyEngine.ReplyResult result) {

                            tvDetectedToneIntent.setText(
                                    "Intent: " + result.intent + " • Tone: " + result.tone);

                            tvReplyPreviewUser.setText(
                                    "Modified reply (en): " + result.replyUserMeaning);

                            tvReplyPreviewSend.setText(
                                    "Reply to send (" +
                                            LanguageUtils.codeToName(result.sendLangCode) +
                                            "): " +
                                            result.replyToSend
                            );

                            lastReplyToSend = result.replyToSend;

                            saveMessage(
                                    incoming,
                                    result.replyUserMeaning,
                                    result.replyToSend,
                                    result.tone,
                                    result.intent,
                                    result.sendLangCode,
                                    null
                            );

                            btnGenerateReply.setEnabled(true);
                            btnGenerateReply.setText("Generate Reply");
                        }

                        @Override
                        public void onError(@NonNull Exception e) {
                            btnGenerateReply.setEnabled(true);
                            btnGenerateReply.setText("Generate Reply");
                            Toast.makeText(MainActivity.this,
                                    "Failed to generate reply", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });
    }

    private void saveMessage(String incoming, String replyStyled, String replyToSend,
                             String tone, String intent, String sendLang, String gifUrl) {

        MessageEntity msg = new MessageEntity();
        msg.originalText = incoming;
        msg.originalLang = detectedOriginalLang;
        msg.translatedForUserText = tvTranslatedForUser.getText().toString();

        msg.replyUserInput = etReply.getText().toString().trim();
        msg.replyStyledUserLang = replyStyled;
        msg.replySendLang = sendLang;
        msg.replySendText = replyToSend;

        msg.detectedTone = tone;
        msg.detectedIntent = intent;
        msg.gifUrl = gifUrl;
        msg.timestamp = System.currentTimeMillis();

        messageDao.insert(msg);
        runOnUiThread(this::loadHistory);
    }

    private void loadHistory() {
        List<MessageEntity> list = messageDao.getAllMessages();
        adapter.setItems(list != null ? list : new ArrayList<>());
    }

    private void setupClearHistoryButton() {
        btnClearHistory.setOnClickListener(v -> {
            messageDao.clearAll();
            loadHistory();
            Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
        });
    }

    // --------------- GIF ----------------
    private void setupGifButton() {
        btnGenerateGif.setOnClickListener(v -> {

            String incoming = etIncoming.getText().toString().trim();
            String reply = etReply.getText().toString().trim();

            if (incoming.isEmpty() && reply.isEmpty()) {
                Toast.makeText(this,
                        "Enter text before generating GIF",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String incomingNorm =
                    SlangProcessor.normalizeInput(detectedOriginalLang, incoming);
            String replyNorm =
                    SlangProcessor.normalizeInput("en", reply);

            String intentFromReply = IntentDetector.detectIntent(replyNorm);
            String intentFromIncoming = IntentDetector.detectIntent(incomingNorm);
            String intent = !"unknown".equals(intentFromReply)
                    ? intentFromReply
                    : intentFromIncoming;

            String tone = ToneDetector.detectTone(
                    replyNorm.isEmpty() ? incomingNorm : replyNorm
            );

            String keywordSourceRaw = reply.isEmpty() ? incoming : reply;
            String keywordSourceNorm =
                    SlangProcessor.normalizeInput(
                            reply.isEmpty() ? detectedOriginalLang : "en",
                            keywordSourceRaw);
            String keywords = KeywordExtractor.extractKeywords(keywordSourceNorm, 2);

            String primaryQuery = GifQueryBuilder.buildQuery(intent, tone, keywords);

            requestGifWithFallback(primaryQuery, intent, tone, incoming, reply);
        });
    }

    private void requestGifWithFallback(String primaryQuery,
                                        String intent,
                                        String tone,
                                        String incoming,
                                        String reply) {

        btnGenerateGif.setEnabled(false);
        btnGenerateGif.setText("Loading GIF...");

        Call<GiphyResponse> call =
                giphyApi.searchGifs(GIPHY_API_KEY, primaryQuery, 1, "g");

        call.enqueue(new Callback<GiphyResponse>() {
            @Override
            public void onResponse(@NonNull Call<GiphyResponse> call,
                                   @NonNull Response<GiphyResponse> response) {

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().data != null
                        && !response.body().data.isEmpty()) {

                    handleGifSuccess(response.body(), primaryQuery,
                            intent, tone, incoming, reply);

                } else {
                    String fallback = GifQueryBuilder.buildFallbackQuery(intent, tone);

                    if (!fallback.equalsIgnoreCase(primaryQuery)) {
                        giphyApi.searchGifs(GIPHY_API_KEY, fallback, 1, "g")
                                .enqueue(new Callback<GiphyResponse>() {
                                    @Override
                                    public void onResponse(@NonNull Call<GiphyResponse> call,
                                                           @NonNull Response<GiphyResponse> response2) {

                                        if (response2.isSuccessful()
                                                && response2.body() != null
                                                && response2.body().data != null
                                                && !response2.body().data.isEmpty()) {

                                            handleGifSuccess(response2.body(), fallback,
                                                    intent, tone, incoming, reply);
                                        } else {
                                            onGifNoneFound(primaryQuery);
                                        }
                                    }

                                    @Override
                                    public void onFailure(@NonNull Call<GiphyResponse> call,
                                                          @NonNull Throwable t) {
                                        onGifFailure();
                                    }
                                });
                    } else {
                        onGifNoneFound(primaryQuery);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<GiphyResponse> call,
                                  @NonNull Throwable t) {
                onGifFailure();
            }
        });
    }

    private void handleGifSuccess(GiphyResponse body,
                                  String usedQuery,
                                  String intent,
                                  String tone,
                                  String incoming,
                                  String reply) {

        btnGenerateGif.setEnabled(true);
        btnGenerateGif.setText("Generate GIF");

        if (body.data == null || body.data.isEmpty()) {
            onGifNoneFound(usedQuery);
            return;
        }

        GiphyResponse.GifObject gif = body.data.get(0);

        String url = null;
        if (gif.images != null) {
            if (gif.images.downsizedMedium != null &&
                    gif.images.downsizedMedium.url != null) {
                url = gif.images.downsizedMedium.url;
            } else if (gif.images.original != null &&
                    gif.images.original.url != null) {
                url = gif.images.original.url;
            }
        }

        if (url == null) {
            onGifNoneFound(usedQuery);
            return;
        }

        lastGifUrl = url;

        Glide.with(this)
                .asGif()
                .load(url)
                .into(ivGifPreview);

        MessageEntity msg = new MessageEntity();
        msg.originalText = incoming.isEmpty() ? "[GIF only]" : incoming;
        msg.originalLang = detectedOriginalLang;
        msg.translatedForUserText = tvTranslatedForUser.getText().toString();

        msg.replyUserInput = reply;
        msg.replyStyledUserLang = reply;
        msg.replySendLang = detectedOriginalLang;
        msg.replySendText = reply;

        msg.detectedTone = tone;
        msg.detectedIntent = intent;
        msg.gifUrl = url;
        msg.timestamp = System.currentTimeMillis();

        messageDao.insert(msg);
        runOnUiThread(this::loadHistory);

        Toast.makeText(this,
                "GIF loaded for: " + usedQuery,
                Toast.LENGTH_SHORT).show();
    }

    private void onGifNoneFound(String query) {
        btnGenerateGif.setEnabled(true);
        btnGenerateGif.setText("Generate GIF");
        Toast.makeText(this,
                "No GIF found for: " + query,
                Toast.LENGTH_SHORT).show();
    }

    private void onGifFailure() {
        btnGenerateGif.setEnabled(true);
        btnGenerateGif.setText("Generate GIF");
        Toast.makeText(this,
                "GIF request failed (check internet)",
                Toast.LENGTH_SHORT).show();
    }

    // --------------- COPY BUTTONS ----------------
    private void setupCopyButtons() {
        btnCopyReply.setOnClickListener(v -> {
            if (lastReplyToSend == null || lastReplyToSend.isEmpty()) {
                Toast.makeText(this, "No reply to copy yet", Toast.LENGTH_SHORT).show();
            } else {
                copyToClipboard("Reply", lastReplyToSend);
                Toast.makeText(this, "Reply copied", Toast.LENGTH_SHORT).show();
            }
        });

        btnCopyGif.setOnClickListener(v -> {
            if (lastGifUrl == null || lastGifUrl.isEmpty()) {
                Toast.makeText(this, "No GIF to copy yet", Toast.LENGTH_SHORT).show();
            } else {
                copyToClipboard("GIF link", lastGifUrl);
                Toast.makeText(this, "GIF link copied", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --------------- SPEECH ----------------
    private void setupSpeechButtons() {
        btnSpeakIncoming.setOnClickListener(v -> {
            if (ensureAudioPermission()) startListeningIncoming();
        });

        btnSpeakReply.setOnClickListener(v -> {
            if (ensureAudioPermission()) startListeningReply();
        });
    }

    private void setupSpeechRecognizers() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            btnSpeakIncoming.setEnabled(false);
            btnSpeakReply.setEnabled(false);
            Toast.makeText(this,
                    "Speech recognition not available",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Incoming speech
        speechRecognizerIncoming = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntentIncoming = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntentIncoming.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        speechRecognizerIncoming.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
            @Override public void onError(int error) { }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    etIncoming.setText(matches.get(0));
                    etIncoming.setSelection(etIncoming.getText().length());
                }
            }
        });

        // Reply speech
        speechRecognizerReply = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntentReply = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntentReply.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        speechRecognizerReply.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
            @Override public void onError(int error) { }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    etReply.setText(matches.get(0));
                    etReply.setSelection(etReply.getText().length());
                }
            }
        });
    }

    private void startListeningIncoming() {
        if (speechRecognizerIncoming != null) {
            speechRecognizerIncoming.startListening(speechIntentIncoming);
        }
    }

    private void startListeningReply() {
        if (speechRecognizerReply != null) {
            speechRecognizerReply.startListening(speechIntentReply);
        }
    }

    private boolean ensureAudioPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQ_RECORD_AUDIO
        );
        return false;
    }

    // --------------- CLIPBOARD HELPER ----------------
    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
        }
    }

    // ✅ FIXED FLOATING BUBBLE (reliable)
    @Override
    protected void onPause() {
        super.onPause();

        if (isChangingConfigurations()) return;
        if (isFinishing()) return;
        if (overlayPermissionRequestInProgress) return;

        if (Settings.canDrawOverlays(this)) {
            Intent i = new Intent(this, FloatingBubbleService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(i);
            } else {
                startService(i);
            }
        } else {
            overlayPermissionRequestInProgress = true;

            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivityForResult(intent, REQ_OVERLAY_PERMISSION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        overlayPermissionRequestInProgress = false;

        // Stop bubble when returning to app
        stopService(new Intent(this, FloatingBubbleService.class));
    }

    // --------------- MENU ----------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --------------- CLEANUP ----------------
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (speechRecognizerIncoming != null) {
            speechRecognizerIncoming.destroy();
        }
        if (speechRecognizerReply != null) {
            speechRecognizerReply.destroy();
        }
    }
}
