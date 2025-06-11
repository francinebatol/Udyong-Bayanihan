package com.example.udyongbayanihan;

public class UserModel {
    private String id;
    private String firstName;
    private String lastName;
    private String userType; // "user" or "admin"

    public UserModel(String id, String firstName, String lastName, String userType) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userType = userType;
    }

    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getUserType() { return userType; }
    public String getFullName() { return firstName + " " + lastName; }
}