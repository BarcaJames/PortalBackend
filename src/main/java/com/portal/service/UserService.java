package com.portal.service;

import com.portal.domain.Users;
import com.portal.exception.domain.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

public interface UserService {

    Users register(String firstName, String lastname, String username, String email) throws UserNotFoundException, EmailExistException, UsernameExistException, MessagingException;
    List<Users> getUsers();
    Users findByUsername(String username);
    Users findByEmail(String email);

    /*This is used to add a user in the system when you are already in the system*/
    Users addNewUser(String firstName, String lastname, String username, String email,
                     String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage
    ) throws UserNotFoundException, EmailExistException, UsernameExistException, MessagingException, NotAnImageFileException, IOException;

    Users updateUser(String currentUsername, String newFirstName, String newLastname, String newUsername, String newEmail,
                     String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage
    ) throws UserNotFoundException, EmailExistException, UsernameExistException, NotAnImageFileException, IOException;

    void deleteUser(String username);

    void resetPassword(String email) throws EmailNotFoundException, MessagingException;

    Users updateProfileImage(String username, MultipartFile profileImage) throws UserNotFoundException, EmailExistException, UsernameExistException, IOException, NotAnImageFileException;
}
