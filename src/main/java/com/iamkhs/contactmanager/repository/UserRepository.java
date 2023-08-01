package com.iamkhs.contactmanager.repository;

import com.iamkhs.contactmanager.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User getUserByEmail(String email);
    User findUserByVerificationCode(String code);
}
