package com.portal.service.impl;

import com.portal.domain.Users;
import com.portal.domain.UserPrincipal;
import com.portal.enumeration.Role;
import com.portal.exception.domain.*;
import com.portal.repository.UserRepository;
import com.portal.service.EmailService;
import com.portal.service.LoginAttemptService;
import com.portal.service.UserService;
import net.bytebuddy.utility.RandomString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.portal.constant.FileConstant.*;
import static com.portal.constant.UserImplServiceConstant.*;
import static com.portal.enumeration.Role.ROLE_USER;
import static org.springframework.http.MediaType.*;

@Service
@Transactional //Manage propagation
@Qualifier("userDetailsService") //Telling spring where to search for userDetailsService to avoid ambiguity
public class UserServiceImpl implements UserService, UserDetailsService {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass()); //getClass() is same as UserServiceImpl.class
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final EmailService emailService;

    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder,
                           LoginAttemptService loginAttemptService, EmailService emailService)
    {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.emailService = emailService;
    }

    /**
     * Locates the user based on the username. In the actual implementation, the search
     * may possibly be case sensitive, or case insensitive depending on how the
     * implementation instance is configured. In this case, the <code>UserDetails</code>
     * object that comes back may have a username that is of a different case than what
     * was actually requested..
     *
     * @param username the username identifying the user whose data is required.
     * @return a fully populated user record (never <code>null</code>)
     * @throws UsernameNotFoundException if the user could not be found or the user has no
     *                                   GrantedAuthority
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = userRepository.findUserByUsername(username);
        if(user == null){
            LOGGER.error(NO_USER_FOUND_BY_USERNAME + username);
            throw new UsernameNotFoundException(NO_USER_FOUND_BY_USERNAME + username);
        }else {
            validateLoginAttempt(user);
            user.setLastLoginDateDisplay(user.getLastLoginDate());
            user.setLastLoginDate(new Date());
            userRepository.save(user);
            UserPrincipal userPrincipal = new UserPrincipal(user);
            LOGGER.info("Returning found user by username: " + username);
            return userPrincipal;
        }
    }

    private void validateLoginAttempt(Users user) {
        if(user.isNotLocked()){
            /// If account is not locked, check if user exceed attempt
            if(loginAttemptService.hasExceededMaxAttempts(user.getUsername())){
                user.setNotLocked(false);
            }else{
                user.setNotLocked(true);
            }
        }else{
            // Runs when the account is locked
            loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());
        }
    }

    @Override
    public Users register(String firstName, String lastname, String username, String email) throws
            UserNotFoundException, EmailExistException, UsernameExistException, MessagingException {
        validateNewUsernameAndEmail("", username, email);
        Users user = new Users();
        user.setUserId(generateUserId());
        String password = generatePassword();
        user.setFirstName(firstName);
        user.setLastName(lastname);
        user.setUsername(username);
        user.setEmail(email);
        user.setJoinDate(new Date());
        user.setPassword(encodePassword(password));
        user.setActive(true);
        user.setNotLocked(true);
        user.setRole(ROLE_USER.name());
        user.setAuthorities(ROLE_USER.getAuthorities());
//        LOGGER.info("New user password is " + "'"+password+"'"); // Todo Remove in production
        emailService.sendNewPasswordEmail(firstName, password, email);
        return userRepository.save(user);
    }

    @Override
    public List<Users> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public Users findByUsername(String username) {
        return userRepository.findUserByUsername(username);
    }

    @Override
    public Users findByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }

    @Override
    public Users addNewUser(String firstName, String lastname, String username,
                            String email, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage
    ) throws UserNotFoundException, EmailExistException, UsernameExistException, NotAnImageFileException, IOException {
        validateNewUsernameAndEmail("", username, email);
        Users user = new Users();
        String password = generatePassword();
        user.setUserId(generateUserId());
        user.setFirstName(firstName);
        user.setLastName(lastname);
        user.setJoinDate(new Date());
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encodePassword(password));
        user.setActive(isActive);
        user.setNotLocked(isNonLocked);
        user.setRole(getRoleEnumName(role).name());
        user.setAuthorities(getRoleEnumName(role).getAuthorities());
        userRepository.save(user);
        saveProfileImage(user, profileImage);
//        emailService.sendNewPasswordEmail(firstName, password, email);
        return user;
    }

    @Override
    public Users updateUser(String currentUsername, String newFirstName, String newLastname, String newUsername,
                            String newEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage)
            throws UserNotFoundException, EmailExistException, UsernameExistException, NotAnImageFileException, IOException {
        Users currentUser = validateNewUsernameAndEmail(currentUsername, newUsername, newEmail);
        currentUser.setFirstName(newFirstName);
        currentUser.setLastName(newLastname);
        currentUser.setUsername(newUsername);
        currentUser.setEmail(newEmail);
        currentUser.setActive(isActive);
        currentUser.setNotLocked(isNonLocked);
        currentUser.setRole(getRoleEnumName(role).name());
        currentUser.setAuthorities(getRoleEnumName(role).getAuthorities());
        userRepository.save(currentUser);
        saveProfileImage(currentUser, profileImage);
        return currentUser;
    }

    @Override
    public void deleteUser(String username){
        Users user = userRepository.findUserByUsername(username);
        userRepository.deleteById(user.getId());
    }

    @Override
    public void resetPassword(String email) throws EmailNotFoundException, MessagingException {
        Users user = userRepository.findUserByEmail(email);
        if(user == null){
            throw new EmailNotFoundException(NO_USER_FOUND_BY_EMAIL + email);
        }
        String password = generatePassword();
        user.setPassword(encodePassword(password));
        userRepository.save(user);
        emailService.sendNewPasswordEmail(user.getFirstName(), password, email);
    }

    @Override
    public Users updateProfileImage(String username, MultipartFile profileImage) throws
            UserNotFoundException, EmailExistException, UsernameExistException, IOException, NotAnImageFileException {

        Users user = validateNewUsernameAndEmail(username, null, null);
        saveProfileImage(user, profileImage);
        return user;
    }

    private void saveProfileImage(Users user, MultipartFile profileImage) throws NotAnImageFileException, IOException {
        if(profileImage != null){
            if(!Arrays.asList(IMAGE_JPEG_VALUE, IMAGE_PNG_VALUE, IMAGE_GIF_VALUE).contains(profileImage.getContentType())){
                throw new NotAnImageFileException(
                        profileImage.getOriginalFilename() + " is not an image file! Please upload an image!"
                );
            }
            user.setProfileImage(profileImage.getBytes());
        }
    }

    private String setProfileImageUrl(String username) {
        /// This will return http://localhost:8080/user/image/{username}/username.jpg
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(USER_IMAGE_PATH + username + FORWARD_SLASH + username + DOT + JPG_EXTENSION).toUriString();
    }

    private Role getRoleEnumName(String role) {
        return Role.valueOf(role.toUpperCase());
    }

    private String getTemporaryProfileImageUrl(String username) {
        /*
         * ServletUriComponentsBuilder.fromCurrentContextPath().path() gets the base url for our application
         * http://localhost:8080 is an example
         * */
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH + username).toUriString();
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String generatePassword() {
//        return "password";
        return RandomString.make(10);
    }

    private String generateUserId() {
        return RandomString.make(10);
    }

    /**
     * validateNewUsernameAndEmail is used to validate info everytime someone tries to create or update account
     * @param currentUsername This will contain a value when user is trying to update their username or email.
     *                        This will be null when user is creating account.
     * */

    private Users validateNewUsernameAndEmail(String currentUsername, String newUsername, String newEmail) throws
            UserNotFoundException, UsernameExistException, EmailExistException
    {
        Users userByNewUsername = findByUsername(newUsername);
        Users userByNewEmail = findByEmail(newEmail);

        // Check if currentUsername is blank
        if(StringUtils.hasText(currentUsername)){
            Users currentUser = findByUsername(currentUsername);
            if(currentUser == null){
                throw new UserNotFoundException(NO_USER_FOUND_BY_USERNAME + currentUsername);
            }

            //Check if someone else in the system is using this newUsername
            if(userByNewUsername != null && !currentUser.getId().equals(userByNewUsername.getId())){
                throw new UsernameExistException(USERNAME_ALREADY_EXISTS);
            }

            //Check if someone else in the system is using this newEmail
            if(userByNewEmail != null && !currentUser.getId().equals(userByNewEmail.getId())){
                throw new EmailExistException(EMAIL_ALREADY_EXISTS);
            }

            return currentUser;
        }else{
            if(userByNewUsername != null){
                throw new UsernameExistException(USERNAME_ALREADY_EXISTS);
            }

            if(userByNewEmail != null){
                throw new EmailExistException(EMAIL_ALREADY_EXISTS);
            }
            return null;
        }
    }

}
