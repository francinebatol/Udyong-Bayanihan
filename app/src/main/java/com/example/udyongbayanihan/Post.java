package com.example.udyongbayanihan;

import com.google.firebase.Timestamp;
import java.util.List;
import java.util.ArrayList;

public class Post {
    private String adminName, position, postContent, amAccountId, postId;
    private String eventId, nameOfEvent, typeOfEvent, organization, barangay, headCoordinator, caption, organizations, status;
    private Timestamp timestamp, date;
    private com.google.firebase.Timestamp postTimestamp;
    private List<String> skills, eventSkills, imageUrls;
    private int volunteerNeeded, participantsJoined, priorityScore;
    private int likesCount = 0;
    private boolean isLikedByCurrentUser = false;
    private boolean isEvent, isCommunityPost, postFeedback;
    private List<PostComment> comments;
    private String joinButtonText = "Join Event"; // Default text for the join button

    public static class PostComment {
        private String commentId;
        private String userId;
        private String fullName;
        private String commentText;
        private Timestamp timestamp;

        public PostComment() {}

        public PostComment(String commentId, String userId, String fullName, String commentText, Timestamp timestamp) {
            this.commentId = commentId;
            this.userId = userId;
            this.fullName = fullName;
            this.commentText = commentText;
            this.timestamp = timestamp;
        }

        // Getters and setters for PostComment
        public String getCommentId() { return commentId; }
        public void setCommentId(String commentId) { this.commentId = commentId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getCommentText() { return commentText; }
        public void setCommentText(String commentText) { this.commentText = commentText; }

        public Timestamp getTimestamp() { return timestamp; }
        public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    }

    // Add getters and setters for comments
    public List<PostComment> getComments() {
        if (comments == null) {
            comments = new ArrayList<>();
        }
        return comments;
    }

    // Getters and Setters
    public void setComments(List<PostComment> comments) {
        this.comments = comments;
    }

    public void addComment(PostComment comment) {
        if (comments == null) {
            comments = new ArrayList<>();
        }
        comments.add(comment);
    }

    public Timestamp getDateForSorting() {
        // For community posts, use postTimestamp
        if (isCommunityPost && postTimestamp != null) {
            return postTimestamp;
        }
        // For events, use the event date
        else if (date != null) {
            return date;
        }
        // If neither is available, return null (this should be handled in the sort comparator)
        return null;
    }

    public void ensureDateIsSet() {
        if (date == null) {
            if (postTimestamp != null) {
                date = postTimestamp;
            } else if (timestamp != null) {
                date = timestamp;
            }
        }
    }


    public String getAmAccountId() {
        return amAccountId;
    }

    public void setAmAccountId(String amAccountId) {
        this.amAccountId = amAccountId;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public Timestamp getPostTimestamp() {
        return postTimestamp;
    }

    public void setPostTimestamp(Timestamp postTimestamp) {
        this.postTimestamp = postTimestamp;
    }

    public boolean isCommunityPost() {
        return isCommunityPost;
    }

    public void setCommunityPost(boolean communityPost) {
        isCommunityPost = communityPost;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getPostContent() {
        return postContent;
    }

    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getEventId() {
        return eventId;
    }

    public void setOrganizations(String organizations) {
        this.organizations = organizations;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setEventSkills(List<String> eventSkills) {
        this.eventSkills = eventSkills;
    }

    public String getOrganizations() {
        return organizations;
    }

    public List<String> getEventSkills() {
        return eventSkills;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getNameOfEvent() {
        return nameOfEvent;
    }

    public void setNameOfEvent(String nameOfEvent) {
        this.nameOfEvent = nameOfEvent;
    }

    public String getTypeOfEvent() {
        return typeOfEvent;
    }

    public void setTypeOfEvent(String typeOfEvent) {
        this.typeOfEvent = typeOfEvent;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getBarangay() {
        return barangay;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public String getHeadCoordinator() {
        return headCoordinator;
    }

    public void setHeadCoordinator(String headCoordinator) {
        this.headCoordinator = headCoordinator;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public int getVolunteerNeeded() {
        return volunteerNeeded;
    }

    public void setVolunteerNeeded(int volunteerNeeded) {
        this.volunteerNeeded = volunteerNeeded;
    }

    public int getParticipantsJoined() {
        return participantsJoined;
    }

    public void setParticipantsJoined(int participantsJoined) {
        this.participantsJoined = participantsJoined;
    }

    public String getJoinButtonText() {
        return joinButtonText;
    }

    public void setJoinButtonText(String joinButtonText) {
        this.joinButtonText = joinButtonText;
    }

    public boolean isEvent() {
        return isEvent;
    }

    public void setEvent(boolean event) {
        isEvent = event;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public boolean isLikedByCurrentUser() {
        return isLikedByCurrentUser;
    }

    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        isLikedByCurrentUser = likedByCurrentUser;
    }

    public void setPriorityScore(int score) {
        this.priorityScore = score;
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    public boolean isPostFeedback() {
        return postFeedback;
    }

    public void setPostFeedback(boolean postFeedback) {
        this.postFeedback = postFeedback;
    }
}

