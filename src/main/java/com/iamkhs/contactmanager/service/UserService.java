package com.iamkhs.contactmanager.service;

import com.iamkhs.contactmanager.entities.User;

public interface UserService {

    User saveUser(User user);
    User getUser(String email);
    User getUserById(Long id);
    boolean isVerified(String code);
}
