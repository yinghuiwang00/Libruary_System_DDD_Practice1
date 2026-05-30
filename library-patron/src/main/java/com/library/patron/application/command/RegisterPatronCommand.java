package com.library.patron.application.command;

import com.library.patron.domain.model.enums.PatronType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RegisterPatronCommand {

    @NotBlank(message = "First name must not be blank")
    private String firstName;

    @NotBlank(message = "Last name must not be blank")
    private String lastName;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String address;
    private String city;
    private String postalCode;

    @NotNull(message = "Patron type must not be null")
    private PatronType patronType;

    public RegisterPatronCommand() {
    }

    public RegisterPatronCommand(String firstName, String lastName, String email,
                                  String phone, String address, String city, String postalCode,
                                  PatronType patronType) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.postalCode = postalCode;
        this.patronType = patronType;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public PatronType getPatronType() { return patronType; }
    public void setPatronType(PatronType patronType) { this.patronType = patronType; }
}
