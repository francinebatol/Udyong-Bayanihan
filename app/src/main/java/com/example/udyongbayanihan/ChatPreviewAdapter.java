package com.example.udyongbayanihan;

import android.content.Intent;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

import android.content.Context;

public class ChatPreviewAdapter extends RecyclerView.Adapter<ChatPreviewAdapter.ChatPreviewViewHolder> {
    private static final String TAG = "ChatPreviewAdapter";
    private List<ChatPreviewModel> chatPreviews;
    private Context context;
    private String currentUserId;
    private String currentUserType;
    private FirebaseFirestore db;

    public ChatPreviewAdapter(List<ChatPreviewModel> chatPreviews, Context context,
                              String currentUserId, String currentUserType) {
        this.chatPreviews = chatPreviews;
        this.context = context;
        this.currentUserId = currentUserId;
        this.currentUserType = currentUserType;
        this.db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Adapter initialized for user: " + currentUserId);
    }

    @NonNull
    @Override
    public ChatPreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recent_chat_recycler_row, parent, false);
        return new ChatPreviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatPreviewViewHolder holder, int position) {
        ChatPreviewModel preview = chatPreviews.get(position);
        Log.d(TAG, "Binding chat preview for chat: " + preview.getChatId());

        // Check if message is from other user and unread
        Map<String, Object> lastMessageMetadata = preview.getLastMessageMetadata();
        boolean isFromOtherUser = lastMessageMetadata != null &&
                !currentUserId.equals(lastMessageMetadata.get("senderId"));
        boolean isUnread = isMessageUnread(preview);

        // Check if other user is admin and set name accordingly
        if ("admin".equals(preview.getOtherUserType())) {
            fetchAdminDetails(preview, holder);
            // Set default admin profile picture as a circle
            setDefaultCircularProfilePicture(holder.userProfile);
        } else {
            // Regular user, set the name
            holder.userName.setText(preview.getOtherUserName());
            // Fetch and set profile picture for regular users
            fetchUserProfilePicture(preview.getOtherUserId(), holder.userProfile);
        }

        // Format and set time based on message date
        holder.lastMessageTime.setText(getFormattedTime(preview.getTimestamp()));

        // Set last message with prefix if needed and apply profanity filter
        String lastMessagePrefix = getLastMessagePrefix(preview);
        String filteredLastMessage = ProfanityFilter.filterProfanity(preview.getLastMessage());
        holder.lastMessage.setText(lastMessagePrefix + filteredLastMessage);

        // Only apply bold style if message is from other user and unread
        if (isFromOtherUser && isUnread) {
            holder.userName.setTypeface(null, Typeface.BOLD);
            holder.lastMessage.setTypeface(null, Typeface.BOLD);
            holder.lastMessageTime.setTypeface(null, Typeface.BOLD);
        } else {
            holder.userName.setTypeface(null, Typeface.NORMAL);
            holder.lastMessage.setTypeface(null, Typeface.NORMAL);
            holder.lastMessageTime.setTypeface(null, Typeface.NORMAL);
        }

        // Set click listener
        setupClickListener(holder.itemView, preview);
    }

    /**
     * Sets the default profile picture as a circle
     * @param imageView The ImageView to display the circular profile picture
     */
    private void setDefaultCircularProfilePicture(ImageView imageView) {
        RequestOptions requestOptions = new RequestOptions()
                .transform(new CircleCrop())
                .override(150, 150);  // Request a larger image for better quality

        Glide.with(context)
                .load(R.drawable.user2)
                .apply(requestOptions)
                .into(imageView);
    }
    /**
     * Fetches and displays the user's profile picture from usersOtherDetails collection
     * @param userId The user ID to fetch profile picture for
     * @param imageView The ImageView to display the profile picture
     */
    private void fetchUserProfilePicture(String userId, ImageView imageView) {
        db.collection("usersOtherDetails")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String profilePictureUrl = queryDocumentSnapshots.getDocuments()
                                .get(0).getString("profilePictureUrl");

                        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                            // Load profile picture using Glide with CircleCrop transformation
                            RequestOptions requestOptions = new RequestOptions()
                                    .placeholder(R.drawable.user2)
                                    .error(R.drawable.user2)
                                    .transform(new CircleCrop())
                                    .override(170, 170)  // Request a larger image for better quality
                                    .dontAnimate();      // Prevent animation issues with the circle crop

                            Glide.with(context)
                                    .load(profilePictureUrl)
                                    .apply(requestOptions)
                                    .into(imageView);

                            Log.d(TAG, "Loaded circular profile picture for user: " + userId);
                        } else {
                            // No profile picture URL available, use default circular image
                            setDefaultCircularProfilePicture(imageView);
                            Log.d(TAG, "No profile picture URL for user: " + userId);
                        }
                    } else {
                        // No user details found, use default circular image
                        setDefaultCircularProfilePicture(imageView);
                        Log.d(TAG, "No user details found for user: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    // Error fetching user details, use default circular image
                    setDefaultCircularProfilePicture(imageView);
                    Log.e(TAG, "Error fetching user details for profile picture: " + e.getMessage());
                });
    }

    private void fetchAdminDetails(ChatPreviewModel preview, ChatPreviewViewHolder holder) {
        // Temporarily set basic name
        holder.userName.setText(preview.getOtherUserName());

        AtomicReference<String> barangay = new AtomicReference<>("");
        AtomicReference<String> position = new AtomicReference<>("");
        AtomicBoolean hasBarangay = new AtomicBoolean(false);
        AtomicBoolean hasPosition = new AtomicBoolean(false);

        // Fetch barangay from AMAddressDetails
        db.collection("AMAddressDetails")
                .whereEqualTo("amAccountId", preview.getOtherUserId())
                .limit(1)
                .get()
                .addOnSuccessListener(barangayDocs -> {
                    if (!barangayDocs.isEmpty()) {
                        String foundBarangay = barangayDocs.getDocuments().get(0).getString("amBarangay");
                        if (foundBarangay != null && !foundBarangay.isEmpty()) {
                            barangay.set(foundBarangay);
                            hasBarangay.set(true);
                        }
                    }

                    // Fetch position from AMOtherDetails
                    db.collection("AMOtherDetails")
                            .whereEqualTo("amAccountid", preview.getOtherUserId())
                            .limit(1)
                            .get()
                            .addOnSuccessListener(positionDocs -> {
                                if (!positionDocs.isEmpty()) {
                                    String foundPosition = positionDocs.getDocuments().get(0).getString("position");
                                    if (foundPosition != null && !foundPosition.isEmpty()) {
                                        position.set(foundPosition);
                                        hasPosition.set(true);
                                    }
                                }

                                // Now construct the display name with both details
                                updateDisplayName(holder, preview.getOtherUserName(), barangay.get(), position.get(),
                                        hasBarangay.get(), hasPosition.get());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fetching admin position: " + e.getMessage());
                                // Update with just barangay if that was found
                                updateDisplayName(holder, preview.getOtherUserName(), barangay.get(), "",
                                        hasBarangay.get(), false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching admin barangay: " + e.getMessage());

                    // Try to at least get the position
                    db.collection("AMOtherDetails")
                            .whereEqualTo("amAccountid", preview.getOtherUserId())
                            .limit(1)
                            .get()
                            .addOnSuccessListener(positionDocs -> {
                                if (!positionDocs.isEmpty()) {
                                    String foundPosition = positionDocs.getDocuments().get(0).getString("position");
                                    if (foundPosition != null && !foundPosition.isEmpty()) {
                                        position.set(foundPosition);
                                        hasPosition.set(true);
                                    }
                                }

                                // Update with just position
                                updateDisplayName(holder, preview.getOtherUserName(), "", position.get(),
                                        false, hasPosition.get());
                            })
                            .addOnFailureListener(positionError -> {
                                Log.e(TAG, "Error fetching admin position: " + positionError.getMessage());
                                // Fallback to basic admin label
                                holder.userName.setText(preview.getOtherUserName() + " (Admin)");
                            });
                });
    }

    private void updateDisplayName(ChatPreviewViewHolder holder, String fullName, String barangay,
                                   String position, boolean hasBarangay, boolean hasPosition) {
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
        holder.userName.setText(displayName.toString());
    }

    private boolean isMessageUnread(ChatPreviewModel preview) {
        Map<String, Object> lastReadBy = preview.getLastReadBy();
        Date messageTimestamp = preview.getTimestamp();

        if (lastReadBy == null || messageTimestamp == null) {
            Log.d(TAG, "Message marked unread - missing lastReadBy or timestamp");
            return true;
        }

        Object lastReadObj = lastReadBy.get(currentUserId);
        if (!(lastReadObj instanceof Timestamp)) {
            Log.d(TAG, "Message marked unread - no last read timestamp");
            return true;
        }

        Timestamp lastRead = (Timestamp) lastReadObj;
        return lastRead.toDate().before(messageTimestamp);
    }

    private String getLastMessagePrefix(ChatPreviewModel preview) {
        Map<String, Object> lastMessageMetadata = preview.getLastMessageMetadata();
        if (lastMessageMetadata != null) {
            String senderId = (String) lastMessageMetadata.get("senderId");
            if (senderId != null) {
                if (senderId.equals(currentUserId)) {
                    return "You: ";
                } else {
                    // We'll use just the name without admin position in message preview
                    return preview.getOtherUserName() + ": ";
                }
            }
        }
        return "";
    }

    private void setupClickListener(View itemView, ChatPreviewModel preview) {
        itemView.setOnClickListener(v -> {
            Log.d(TAG, "Opening chat with: " + preview.getOtherUserName());

            // Check if other user is admin and include position in chat
            if ("admin".equals(preview.getOtherUserType())) {
                openChatWithAdmin(preview);
            } else {
                // Regular user chat
                openRegularChat(preview);
            }
        });
    }

    private void openChatWithAdmin(ChatPreviewModel preview) {
        AtomicReference<String> barangay = new AtomicReference<>("");
        AtomicReference<String> position = new AtomicReference<>("");
        AtomicBoolean hasBarangay = new AtomicBoolean(false);
        AtomicBoolean hasPosition = new AtomicBoolean(false);

        // Fetch barangay from AMAddressDetails
        db.collection("AMAddressDetails")
                .whereEqualTo("amAccountId", preview.getOtherUserId())
                .limit(1)
                .get()
                .addOnSuccessListener(barangayDocs -> {
                    if (!barangayDocs.isEmpty()) {
                        String foundBarangay = barangayDocs.getDocuments().get(0).getString("amBarangay");
                        if (foundBarangay != null && !foundBarangay.isEmpty()) {
                            barangay.set(foundBarangay);
                            hasBarangay.set(true);
                        }
                    }

                    // Fetch position from AMOtherDetails
                    db.collection("AMOtherDetails")
                            .whereEqualTo("amAccountid", preview.getOtherUserId())
                            .limit(1)
                            .get()
                            .addOnSuccessListener(positionDocs -> {
                                if (!positionDocs.isEmpty()) {
                                    String foundPosition = positionDocs.getDocuments().get(0).getString("position");
                                    if (foundPosition != null && !foundPosition.isEmpty()) {
                                        position.set(foundPosition);
                                        hasPosition.set(true);
                                    }
                                }

                                // Create display name and start chat
                                String displayName = createDisplayName(preview.getOtherUserName(), barangay.get(),
                                        position.get(), hasBarangay.get(), hasPosition.get());

                                Intent intent = new Intent(context, Chat.class);
                                intent.putExtra("userId", currentUserId);
                                intent.putExtra("userType", currentUserType);
                                intent.putExtra("recipientId", preview.getOtherUserId());
                                intent.putExtra("recipientType", preview.getOtherUserType());
                                intent.putExtra("recipientName", displayName);

                                if (hasBarangay.get()) {
                                    intent.putExtra("recipientBarangay", barangay.get());
                                }

                                if (hasPosition.get()) {
                                    intent.putExtra("recipientPosition", position.get());
                                }

                                context.startActivity(intent);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fetching admin position: " + e.getMessage());
                                String displayName = createDisplayName(preview.getOtherUserName(), barangay.get(),
                                        "", hasBarangay.get(), false);
                                Intent intent = new Intent(context, Chat.class);
                                intent.putExtra("userId", currentUserId);
                                intent.putExtra("userType", currentUserType);
                                intent.putExtra("recipientId", preview.getOtherUserId());
                                intent.putExtra("recipientType", preview.getOtherUserType());
                                intent.putExtra("recipientName", displayName);

                                if (hasBarangay.get()) {
                                    intent.putExtra("recipientBarangay", barangay.get());
                                }

                                context.startActivity(intent);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching admin barangay: " + e.getMessage());

                    // Try to at least get the position
                    db.collection("AMOtherDetails")
                            .whereEqualTo("amAccountid", preview.getOtherUserId())
                            .limit(1)
                            .get()
                            .addOnSuccessListener(positionDocs -> {
                                if (!positionDocs.isEmpty()) {
                                    String foundPosition = positionDocs.getDocuments().get(0).getString("position");
                                    if (foundPosition != null && !foundPosition.isEmpty()) {
                                        position.set(foundPosition);
                                        hasPosition.set(true);
                                    }
                                }

                                String displayName = createDisplayName(preview.getOtherUserName(), "",
                                        position.get(), false, hasPosition.get());

                                Intent intent = new Intent(context, Chat.class);
                                intent.putExtra("userId", currentUserId);
                                intent.putExtra("userType", currentUserType);
                                intent.putExtra("recipientId", preview.getOtherUserId());
                                intent.putExtra("recipientType", preview.getOtherUserType());
                                intent.putExtra("recipientName", displayName);

                                if (hasPosition.get()) {
                                    intent.putExtra("recipientPosition", position.get());
                                }

                                context.startActivity(intent);
                            })
                            .addOnFailureListener(positionError -> {
                                Log.e(TAG, "Error fetching admin position: " + positionError.getMessage());

                                // Fallback to basic admin label
                                Intent intent = new Intent(context, Chat.class);
                                intent.putExtra("userId", currentUserId);
                                intent.putExtra("userType", currentUserType);
                                intent.putExtra("recipientId", preview.getOtherUserId());
                                intent.putExtra("recipientType", preview.getOtherUserType());
                                intent.putExtra("recipientName", preview.getOtherUserName() + " (Admin)");
                                context.startActivity(intent);
                            });
                });
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

    private void openRegularChat(ChatPreviewModel preview) {
        Intent intent = new Intent(context, Chat.class);
        intent.putExtra("userId", currentUserId);
        intent.putExtra("userType", currentUserType);
        intent.putExtra("recipientId", preview.getOtherUserId());
        intent.putExtra("recipientType", preview.getOtherUserType());
        intent.putExtra("recipientName", preview.getOtherUserName());
        context.startActivity(intent);
    }

    private String getFormattedTime(Date messageDate) {
        if (messageDate == null) return "";

        Calendar now = Calendar.getInstance();
        Calendar messageTime = Calendar.getInstance();
        messageTime.setTime(messageDate);

        // Check if message is from today
        if (isSameDay(now, messageTime)) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(messageDate);
        }

        // Check if message is from yesterday
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(yesterday, messageTime)) {
            return "Yesterday";
        }

        // Check if message is within last 7 days
        long diffInDays = TimeUnit.MILLISECONDS.toDays(
                now.getTimeInMillis() - messageTime.getTimeInMillis());

        if (diffInDays < 7) {
            return new SimpleDateFormat("EEEE", Locale.getDefault()).format(messageDate);
        }

        // For older messages, show the date
        return new SimpleDateFormat("MMM d", Locale.getDefault()).format(messageDate);
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public int getItemCount() {
        return chatPreviews.size();
    }

    static class ChatPreviewViewHolder extends RecyclerView.ViewHolder {
        TextView userName, lastMessage, lastMessageTime;
        ImageView userProfile;

        ChatPreviewViewHolder(View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.textMessageName);
            lastMessage = itemView.findViewById(R.id.textLastMessage);
            lastMessageTime = itemView.findViewById(R.id.textLastMessageTime);
            userProfile = itemView.findViewById(R.id.userProfile);
        }
    }
}