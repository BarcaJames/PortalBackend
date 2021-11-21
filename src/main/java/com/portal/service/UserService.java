package com.portal.service;

import com.portal.domain.User;
import com.portal.exception.domain.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

public interface UserService {

    User register(String firstName, String lastname, String username, String email) throws UserNotFoundException, EmailExistException, UsernameExistException, MessagingException;
    List<User> getUsers();
    User findByUsername(String username);
    User findByEmail(String email);

    /*This is used to add a user in the system when you are already in the system*/
    User addNewUser(String firstName, String lastname, String username, String email,
                    String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage
    ) throws UserNotFoundException, EmailExistException, UsernameExistException, IOException, MessagingException, NotAnImageFileException;

    User updateUser(String currentUsername, String newFirstName, String newLastname, String newUsername, String newEmail,
                    String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage
    ) throws UserNotFoundException, EmailExistException, UsernameExistException, IOException, NotAnImageFileException;

    void deleteUser(String username) throws IOException;

    void resetPassword(String email) throws EmailNotFoundException, MessagingException;

    User updateProfileImage(String username, MultipartFile profileImage) throws UserNotFoundException, EmailExistException, UsernameExistException, IOException, NotAnImageFileException;
}
