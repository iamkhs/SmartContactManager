package com.iamkhs.contactmanager.configuration;

import com.iamkhs.contactmanager.entities.User;
import com.iamkhs.contactmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository repository;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // fetching the user from database;
        User user = this.repository.getUserByEmail(username);
        if (user == null){
            throw new UsernameNotFoundException("User NOT FOUND!");
        }

        return new CustomUserDetails(user);
    }
}
