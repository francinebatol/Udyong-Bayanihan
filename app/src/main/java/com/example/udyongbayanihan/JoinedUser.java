package com.example.udyongbayanihan;

import java.util.List;

public class JoinedUser {
    private String firstName, middleName, lastName, gender, barangay, municipality, otherBarangay;
    private Long age; // Use Long for consistency with Firestore
    private List<String> skills; // Add this to store the user's skills

    public JoinedUser() {}

    public JoinedUser(String firstName, String middleName, String lastName, Long age, String gender,
                      String barangay, String municipality, String otherBarangay, List<String> skills) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.age = age;
        this.gender = gender;
        this.barangay = barangay;
        this.municipality = municipality;
        this.otherBarangay = otherBarangay;
        this.skills = skills;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public void setMunicipality(String municipality) {
        this.municipality = municipality;
    }

    public void setAge(Long age) {
        this.age = age;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public String getFirstName() { return firstName; }
    public String getMiddleName() { return middleName; }
    public String getLastName() { return lastName; }
    public Long getAge() { return age; }
    public String getGender() { return gender; }
    public String getBarangay() { return barangay; }
    public String getMunicipality() { return municipality; }
    public String getOtherBarangay() { return otherBarangay; }
    public List<String> getSkills() { return skills; }
}
