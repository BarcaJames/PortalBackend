package com.portal.repository;

import com.portal.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users, Long> {

    Users findUserByUsername(String username);

    Users findUserByEmail(String email);
}
