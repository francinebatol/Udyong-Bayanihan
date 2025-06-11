package com.example.udyongbayanihan;

import java.io.Serializable;

/**
 * Class to hold all registration data through the registration process.
 * This serializable class will be passed between activities instead of saving to Firestore prematurely.
 */
public class RegistrationData implements Serializable {
    // Phase 1 data
    private String firstName;
    private String middleName;
    private String lastName;
    private String gender;
    private int age;
    private String dateOfBirth;

    // Phase 2 data
    private Long houseNo;
    private String street;
    private String barangay;
    private String otherBarangay;
    private String municipality;
    private Long phoneNo;
    private String idPictureUrl;

    // Phase 3 data
    private String username;
    private String email;
    private String password; // Added to store password temporarily
    private String userId;   // Added to store Firebase Auth user ID

    // Firebase document IDs
    private String unameId;
    private String uotherDetailsId;
    private String uaddressId;

    // Constructor
    public RegistrationData() {
        // Initialize document IDs that would normally be created at each phase
        this.unameId = "";
        this.uotherDetailsId = "";
        this.uaddressId = "";
    }

    // Getters and setters for all fields

    // Phase 1 getters and setters
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    // Phase 2 getters and setters
    public Long getHouseNo() {
        return houseNo;
    }

    public void setHouseNo(Long houseNo) {
        this.houseNo = houseNo;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getBarangay() {
        return barangay;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public String getOtherBarangay() {
        return otherBarangay;
    }

    public void setOtherBarangay(String otherBarangay) {
        this.otherBarangay = otherBarangay;
    }

    public String getMunicipality() {
        return municipality;
    }

    public void setMunicipality(String municipality) {
        this.municipality = municipality;
    }

    public Long getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(Long phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getIdPictureUrl() {
        return idPictureUrl;
    }

    public void setIdPictureUrl(String idPictureUrl) {
        this.idPictureUrl = idPictureUrl;
    }

    // Phase 3 getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Added for storing password temporarily
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // Added for storing Firebase Auth user ID
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Document IDs getters and setters
    public String getUnameId() {
        return unameId;
    }

    public void setUnameId(String unameId) {
        this.unameId = unameId;
    }

    public String getUotherDetailsId() {
        return uotherDetailsId;
    }

    public void setUotherDetailsId(String uotherDetailsId) {
        this.uotherDetailsId = uotherDetailsId;
    }

    public String getUaddressId() {
        return uaddressId;
    }

    public void setUaddressId(String uaddressId) {
        this.uaddressId = uaddressId;
    }
}