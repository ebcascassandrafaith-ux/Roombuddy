package com.template;

// One User object = one row of the "users" table
public class User
{
    private int userId;
    private String fullName;
    private String email;
    private String contactNumber;
    private String role;
    private String status;

    // These three are only filled in by UserMain.getUserById (the profile screen, Stage 2)
    private String profilePhoto;
    private String bio;
    private String preferredLanguage;

    public User(int userId, String fullName, String email,
                String contactNumber, String role, String status)
    {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.contactNumber = contactNumber;
        this.role = role;
        this.status = status;
    }

    public int getUserId()
    {
        return userId;
    }

    public String getFullName()
    {
        return fullName;
    }

    public String getEmail()
    {
        return email;
    }

    public String getContactNumber()
    {
        return contactNumber;
    }

    public String getRole()
    {
        return role;
    }

    public String getStatus()
    {
        return status;
    }

    public String getProfilePhoto()
    {
        return profilePhoto;
    }

    public void setProfilePhoto(String profilePhoto)
    {
        this.profilePhoto = profilePhoto;
    }

    public String getBio()
    {
        return bio;
    }

    public void setBio(String bio)
    {
        this.bio = bio;
    }

    public String getPreferredLanguage()
    {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage)
    {
        this.preferredLanguage = preferredLanguage;
    }
}