package com.example.udyongbayanihan;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Chat extends AppCompatActivity {
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUserType;
    private String recipientId;
    private String recipientType;
    private String recipientName;
    private String recipientBarangay;
    private String recipientPosition;
    private RecyclerView chatRecyclerView;
    private MessageAdapter messageAdapter;
    private List<MessageModel> messages;
    private EditText inputChat;
    private ImageButton btnSendChat;
    private TextView recipientNameText;
    private KeyboardVisibilityUtil keyboardVisibilityUtil;

    private static final String TAG = "Chat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get and validate intent extras
        if (getIntent().getExtras() != null) {
            currentUserId = getIntent().getStringExtra("userId");
            currentUserType = getIntent().getStringExtra("userType");
            recipientId = getIntent().getStringExtra("recipientId");
            recipientType = getIntent().getStringExtra("recipientType");
            recipientName = getIntent().getStringExtra("recipientName");
            recipientBarangay = getIntent().getStringExtra("recipientBarangay");
            recipientPosition = getIntent().getStringExtra("recipientPosition");

            // Validate required data
            if (currentUserId == null || recipientId == null) {
                Toast.makeText(this, "Error: Missing user information", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        } else {
            Toast.makeText(this, "Error: Missing required data", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize views
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        inputChat = findViewById(R.id.inputChat);
        btnSendChat = findViewById(R.id.imgbtnSendChat);
        recipientNameText = findViewById(R.id.recipientName);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // Set recipient name
        setupRecipientName();

        // Initialize RecyclerView
        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(messages, currentUserId);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(messageAdapter);

        // Load messages
        loadMessages();

        // Send message button click listener
        btnSendChat.setOnClickListener(v -> sendMessage());

        // Back button click listener
        btnBack.setOnClickListener(v -> finish());

        inputChat.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && messages.size() > 0) {
                // When EditText gains focus, scroll to bottom of chat
                chatRecyclerView.postDelayed(() ->
                        chatRecyclerView.smoothScrollToPosition(messages.size() - 1), 200);
            }
        });

        setupKeyboardVisibility();
    }

    private void setupRecipientName() {
        // If the recipientName is already formatted with Admin info, use it directly
        if (recipientName != null && recipientName.contains("Admin")) {
            recipientNameText.setText(recipientName);
            return;
        }

        // If the recipientType is admin but we don't have their position/barangay yet, fetch it
        if ("admin".equals(recipientType)) {
            if ((recipientBarangay == null || recipientBarangay.isEmpty()) &&
                    (recipientPosition == null || recipientPosition.isEmpty())) {
                fetchAdminDetails();
            } else {
                // We have at least one detail, use what we have
                updateDisplayName();
            }
        } else {
            // Regular user, just use the name
            recipientNameText.setText(recipientName != null ? recipientName : "Chat");
        }
    }

    private void fetchAdminDetails() {
        // Set a temporary name while we fetch the details
        recipientNameText.setText(recipientName);

        AtomicReference<String> barangay = new AtomicReference<>("");
        AtomicReference<String> position = new AtomicReference<>("");
        AtomicBoolean hasBarangay = new AtomicBoolean(false);
        AtomicBoolean hasPosition = new AtomicBoolean(false);

        // Fetch barangay from AMAddressDetails
        db.collection("AMAddressDetails")
                .whereEqualTo("amAccountId", recipientId)
                .limit(1)
                .get()
                .addOnSuccessListener(barangayDocs -> {
                    if (!barangayDocs.isEmpty()) {
                        String foundBarangay = barangayDocs.getDocuments().get(0).getString("amBarangay");
                        if (foundBarangay != null && !foundBarangay.isEmpty()) {
                            barangay.set(foundBarangay);
                            hasBarangay.set(true);
                            recipientBarangay = foundBarangay;
                        }
                    }

                    // Fetch position from AMOtherDetails
                    db.collection("AMOtherDetails")
                            .whereEqualTo("amAccountid", recipientId)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(positionDocs -> {
                                if (!positionDocs.isEmpty()) {
                                    String foundPosition = positionDocs.getDocuments().get(0).getString("position");
                                    if (foundPosition != null && !foundPosition.isEmpty()) {
                                        position.set(foundPosition);
                                        hasPosition.set(true);
                                        recipientPosition = foundPosition;
                                    }
                                }

                                // Now construct the display name with both details
                                String displayName = createDisplayName(recipientName, barangay.get(), position.get(),
                                        hasBarangay.get(), hasPosition.get());
                                recipientNameText.setText(displayName);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fetching admin position: " + e.getMessage());
                                // Update with just barangay if that was found
                                String displayName = createDisplayName(recipientName, barangay.get(), "",
                                        hasBarangay.get(), false);
                                recipientNameText.setText(displayName);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching admin barangay: " + e.getMessage());

                    // Try to at least get the position
                    db.collection("AMOtherDetails")
                            .whereEqualTo("amAccountid", recipientId)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(positionDocs -> {
                                if (!positionDocs.isEmpty()) {
                                    String foundPosition = positionDocs.getDocuments().get(0).getString("position");
                                    if (foundPosition != null && !foundPosition.isEmpty()) {
                                        position.set(foundPosition);
                                        hasPosition.set(true);
                                        recipientPosition = foundPosition;
                                    }
                                }

                                // Update with just position
                                String displayName = createDisplayName(recipientName, "", position.get(),
                                        false, hasPosition.get());
                                recipientNameText.setText(displayName);
                            })
                            .addOnFailureListener(positionError -> {
                                Log.e(TAG, "Error fetching admin position: " + positionError.getMessage());
                                // Fallback to basic admin label
                                recipientNameText.setText(recipientName + " (Admin)");
                            });
                });
    }

    private void updateDisplayName() {
        boolean hasBarangay = recipientBarangay != null && !recipientBarangay.isEmpty();
        boolean hasPosition = recipientPosition != null && !recipientPosition.isEmpty();

        String displayName = createDisplayName(recipientName,
                hasBarangay ? recipientBarangay : "",
                hasPosition ? recipientPosition : "",
                hasBarangay, hasPosition);

        recipientNameText.setText(displayName);
    }

    private String createDisplayName(String fullName, String barangay, String position,
                                     boolean hasBarangay, boolean hasPosition) {
        StringBuilder displayName = new StringBuilder(fullName);
        displayName.append(" (Admin");

        if (hasBarangay || hasPosition) {
            displayName.append(" - ");

            if (hasBarangay) {
                displayName.append(barangay);
                if (hasPosition) {
                    displayName.append(" ");
                }
            }

            if (hasPosition) {
                displayName.append(position);
            }
        }

        displayName.append(")");
        return displayName.toString();
    }

    private void loadMessages() {
        String chatId = getChatId(currentUserId, recipientId);
        if (chatId == null) {
            Toast.makeText(this, "Error: Unable to create chat", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(Chat.this, "Error loading messages: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        messages.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            MessageModel message = doc.toObject(MessageModel.class);
                            if (message != null) {
                                messages.add(message);
                            }
                        }
                        messageAdapter.notifyDataSetChanged();
                        if (!messages.isEmpty()) {
                            chatRecyclerView.scrollToPosition(messages.size() - 1);
                        }

                        // Mark messages as read
                        markMessagesAsRead(chatId);
                    }
                });
    }

    private void markMessagesAsRead(String chatId) {
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("recipientId", currentUserId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().update("read", true);
                    }
                });
    }

    private void markChatAsRead() {
        String chatId = getChatId(currentUserId, recipientId);
        if (chatId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastReadBy." + currentUserId, Timestamp.now());

        db.collection("chats").document(chatId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d("Chat", "Chat marked as read"))
                .addOnFailureListener(e -> Log.e("Chat", "Error marking chat as read", e));
    }

    private String getChatId(String userId1, String userId2) {
        // Validate input parameters
        if (userId1 == null || userId2 == null) {
            return null;
        }

        // Create a consistent chat ID regardless of who initiated the chat
        return userId1.compareTo(userId2) < 0
                ? userId1 + "_" + userId2
                : userId2 + "_" + userId1;
    }

    private void sendMessage() {
        String messageText = inputChat.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        // Apply profanity filter before sending
        messageText = ProfanityFilter.filterProfanity(messageText);

        String chatId = getChatId(currentUserId, recipientId);
        if (chatId == null) {
            Toast.makeText(this, "Error: Unable to send message", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageId = db.collection("chats").document(chatId)
                .collection("messages").document().getId();

        MessageModel message = new MessageModel(
                messageId,
                currentUserId,
                recipientId,
                currentUserType,
                recipientType,
                messageText,  // Using filtered message
                new Date(),
                false
        );

        // Save message to Firestore
        String finalMessageText = messageText;
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .set(message)
                .addOnSuccessListener(aVoid -> {
                    inputChat.setText("");
                    updateLastMessage(chatId, finalMessageText);

                    // Add this to scroll to bottom after sending
                    if (messages.size() > 0) {
                        chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(Chat.this, "Failed to send message: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void updateLastMessage(String chatId, String lastMessage) {
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("lastMessage", lastMessage);
        chatData.put("timestamp", new Date());
        chatData.put("lastMessageMetadata", new HashMap<String, String>() {{
            put("senderId", currentUserId);
            put("senderType", currentUserType);
        }});
        chatData.put("participants", Arrays.asList(
                new HashMap<String, String>() {{
                    put("id", currentUserId);
                    put("type", currentUserType != null ? currentUserType : "user");
                }},
                new HashMap<String, String>() {{
                    put("id", recipientId);
                    put("type", recipientType != null ? recipientType : "user");
                }}
        ));

        db.collection("chats")
                .document(chatId)
                .set(chatData, SetOptions.merge())
                .addOnFailureListener(e ->
                        Toast.makeText(Chat.this, "Failed to update chat: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void setupKeyboardVisibility() {
        keyboardVisibilityUtil = new KeyboardVisibilityUtil(this);
        keyboardVisibilityUtil.setKeyboardVisibilityListener(isVisible -> {
            if (isVisible) {
                // Keyboard is visible, scroll to bottom of chat with a slight delay
                chatRecyclerView.postDelayed(() -> {
                    if (messages.size() > 0) {
                        chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                    }
                }, 200);
            }
        });
    }



    @Override
    protected void onResume() {
        super.onResume();
        markChatAsRead();
    }
}