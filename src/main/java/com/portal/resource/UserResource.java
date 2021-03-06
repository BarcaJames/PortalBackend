package com.portal.resource;

import com.portal.domain.HttpResponse;
import com.portal.domain.Users;
import com.portal.domain.UserPrincipal;
import com.portal.exception.domain.*;
import com.portal.service.UserService;
import com.portal.utility.JWTTokenProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.portal.constant.FileConstant.*;
import static com.portal.constant.SecurityConstant.JWT_TOKEN_HEADER;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

@RestController
@RequestMapping(path = {"/","/user"})
public class UserResource extends ExceptionHandling {

    public static final String EMAIL_SENT = "An email with a new password was sent to: ";
    public static final String USER_DELETED_SUCCESSFULLY = "User deleted successfully";
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JWTTokenProvider jwtTokenProvider;

    public UserResource(UserService userService, AuthenticationManager authenticationManager, JWTTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<Users> login(@RequestBody Users user) {
        authenticate(user.getUsername(), user.getPassword());
        Users loginUser = userService.findByUsername(user.getUsername());
        UserPrincipal userPrincipal = new UserPrincipal(loginUser);
        HttpHeaders jwtHeader = getJwtHeader(userPrincipal);
        return new ResponseEntity<>(loginUser,jwtHeader, HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<Users> register(@RequestBody Users user) throws
            UserNotFoundException, EmailExistException, UsernameExistException, MessagingException {
       Users newUser =  userService.register(user.getFirstName(), user.getLastName(), user.getUsername(), user.getEmail());
       return new ResponseEntity<>(newUser, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyAuthority('user:update')")
    @PostMapping("/add")
    public ResponseEntity<Users> addNewUser(@RequestParam("firstName") String firstName,
                                            @RequestParam("lastName") String lastName,
                                            @RequestParam("username") String username,
                                            @RequestParam("email") String email,
                                            @RequestParam("role") String role,
                                            @RequestParam("isActive") String isActive,
                                            @RequestParam("isNonLocked") String isNonLocked,
                                            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage)
            throws UserNotFoundException, EmailExistException, UsernameExistException, MessagingException, NotAnImageFileException, IOException {

        Users newUser = userService.addNewUser(firstName, lastName, username, email, role,
                Boolean.parseBoolean(isNonLocked), Boolean.parseBoolean(isActive), profileImage);

        return new ResponseEntity<>(newUser, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyAuthority('user:update')")
    @PostMapping("/update")
    public ResponseEntity<Users> update(@RequestParam("currentUsername") String currentUsername,
                                        @RequestParam("firstName") String firstName,
                                        @RequestParam("lastName") String lastName,
                                        @RequestParam("username") String username,
                                        @RequestParam("email") String email,
                                        @RequestParam("role") String role,
                                        @RequestParam("isActive") String isActive,
                                        @RequestParam("isNonLocked") String isNonLocked,
                                        @RequestParam(value = "profileImage", required = false) MultipartFile profileImage)
            throws UserNotFoundException, EmailExistException, UsernameExistException, NotAnImageFileException, IOException {

        Users updatedUser = userService.updateUser(currentUsername,firstName, lastName, username, email, role,
                Boolean.parseBoolean(isNonLocked), Boolean.parseBoolean(isActive), profileImage);

        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    @GetMapping("/find/{username}")
    public ResponseEntity<Users> getUser(@PathVariable("username") String username){
        return new ResponseEntity<>(userService.findByUsername(username), HttpStatus.OK);
    }

    @GetMapping("/list")
    public ResponseEntity<List<Users>> getAllUsers(){
        return new ResponseEntity<>(userService.getUsers(), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyAuthority('user:create')")
    @PostMapping("reset-password/{email}")
    public ResponseEntity<HttpResponse> resetPassword(@PathVariable("email") String email) throws EmailNotFoundException, MessagingException {
        userService.resetPassword(email);
        return response(HttpStatus.OK, EMAIL_SENT + email);
    }

    @DeleteMapping("/delete/{username}")
    @PreAuthorize("hasAnyAuthority('user:delete')")
    public ResponseEntity<HttpResponse> deleteUser(@PathVariable("username") String username) {
        userService.deleteUser(username);
        return response(HttpStatus.NO_CONTENT, USER_DELETED_SUCCESSFULLY);
    }

    @PostMapping("/update-profile-Image")
    public ResponseEntity<Users> updateProfileImage(@RequestParam("username") String username,
                                                    @RequestParam(value = "profileImage") MultipartFile profileImage)
            throws UserNotFoundException, EmailExistException, IOException, UsernameExistException, NotAnImageFileException {

        Users updatedUser = userService.updateProfileImage(username, profileImage);

        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    /**
     * @deprecated
     * This method should not be used since we no longer add pictures to the fie system
     * but instead send them to the database as blobs*/
    @Deprecated
    // Get user image from file system
    @GetMapping(path = "/image/{username}/{fileName}", produces = IMAGE_JPEG_VALUE)
    public byte[] getProfileImage(@PathVariable String fileName, @PathVariable String username) throws IOException {
        return Files.readAllBytes(Paths.get(USER_FOLDER + username + FORWARD_SLASH + fileName));
    }

    /**
     * @deprecated
     * This method should not be used since we no longer add pictures to the fie system
     * but instead send them to the database as blobs*/
    @Deprecated
    //Responsible for getting the avatar from TEMP_PROFILE_IMAGE_BASE_URL
    @GetMapping(path = "/image/profile/{username}", produces = IMAGE_JPEG_VALUE)
    public byte[] getTempProfileImage( @PathVariable String username) throws IOException {
        // Get url
        URL url = new URL(TEMP_PROFILE_IMAGE_BASE_URL + username);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try(InputStream inputStream = url.openStream()){
            int bytesRead;
            byte[] chunk = new byte[1024];
            while((bytesRead = inputStream.read(chunk)) > 0){
                byteArrayOutputStream.write(chunk, 0, bytesRead);
            }
        }

        return byteArrayOutputStream.toByteArray();
    }

    private ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String message) {
        HttpResponse body = new HttpResponse(httpStatus.value(), httpStatus,
                httpStatus.getReasonPhrase().toUpperCase(),message.toUpperCase());

        return new ResponseEntity<>(body, httpStatus);
    }

    private HttpHeaders getJwtHeader(UserPrincipal userPrincipal) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(JWT_TOKEN_HEADER, jwtTokenProvider.generateJwtToken(userPrincipal));
        return headers;
    }

    private void authenticate(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }
}
