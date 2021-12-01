package com.portal.configuration;

import com.portal.domain.Users;
import com.portal.enumeration.Role;
import com.portal.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Date;

import static com.portal.enumeration.Role.*;

@Configuration
@EnableScheduling
@Slf4j
@AllArgsConstructor
public class ScheduleConfiguration {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Scheduled(cron = "0 32 14 * * *", zone = "GMT-5")
//    @Scheduled(cron = "0 0 0 * * *", zone = "GMT-5")
    public void cleanUp(){
        userRepository.deleteAll();

        userRepository.save(buildUser("Jamie", "James", "Jamie", ROLE_SUPER_ADMIN));
        userRepository.save(buildUser("Jane", "Brown", "Jane", ROLE_ADMIN));
        userRepository.save(buildUser("Kimberly", "Davis", "Kimmy", ROLE_MANAGER));
        userRepository.save(buildUser("Pam", "Green", "Pam", ROLE_HR));
        userRepository.save(buildUser("John", "Brown", "Johnny", ROLE_USER));

    }

    private Users buildUser(String firstName, String lastName, String username, Role role){
        Users user = new Users();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(firstName+"@portal.com");
        user.setRole(role.name());
        user.setUserId(RandomString.make(10));
        user.setJoinDate(new Date());
        user.setActive(true);
        user.setNotLocked(true);
        user.setPassword(bCryptPasswordEncoder.encode("password"));
        user.setAuthorities(role.getAuthorities());

        return user;
    }
}
