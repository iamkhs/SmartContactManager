package com.iamkhs.contactmanager.service.impl;

import com.iamkhs.contactmanager.entities.User;
import com.iamkhs.contactmanager.repository.UserRepository;
import com.iamkhs.contactmanager.service.UserService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // saving the user
    @Override
    public User saveUser(User user) {
        return this.userRepository.save(user);
    }

    @Override
    public User getUser(String email) {
        return this.userRepository.getUserByEmail(email);
    }

    @Override
    public User getUserById(Long id) {
        return this.userRepository.findById(id).orElseThrow();
    }

    @Override
    public boolean isVerified(String code) {
        User user = userRepository.findUserByVerificationCode(code);
        if (user == null) {
            // User not found
            return false;
        }  else {

            var userRegisterDate = user.getUserRegisterDate();

            LocalDateTime currentDateTime = LocalDateTime.now();

            long minutes = Duration.between(userRegisterDate, currentDateTime).toMinutes();

            if (minutes > 1) {
                // if the duration greater than or equal 1 minute than delete the user
                System.err.println("the time is greater than 1 : " + minutes);
                userRepository.deleteById(user.getId());
                return false;
            } else {
                user.setEnable(true);
                user.setVerificationCode(null); // setting the verification code null so that the user cannot try to verify again
                System.err.println(user);
                this.saveUser(user);
                return true;
            }
        }
    }

}
