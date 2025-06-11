package com.example.udyongbayanihan;

public class Users {

        public String firstName, middleName, lastName, gender, age, dateOfBirth, houseNo, street, barangay, email, phoneNo, username, password;

        // Default constructor required for Firebase
        public Users() {}

        // Constructor to initialize all fields
        public Users(String firstName, String middleName, String lastName, String gender, String age, String dateOfBirth,
                    String houseNo, String street, String barangay, String email, String phoneNo, String username, String password) {
            this.firstName = firstName;
            this.middleName = middleName;
            this.lastName = lastName;
            this.gender = gender;
            this.age = age;
            this.dateOfBirth = dateOfBirth;
            this.houseNo = houseNo;
            this.street = street;
            this.barangay = barangay;
            this.email = email;
            this.phoneNo = phoneNo;
            this.username = username;
            this.password = password;
        }
    }
