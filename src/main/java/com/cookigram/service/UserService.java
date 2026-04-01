package com.cookigram.service;

import com.cookigram.dto.RegistrationDto;
import com.cookigram.model.Role;
import com.cookigram.model.User;
import com.cookigram.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public User registerNewCustomer(RegistrationDto dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.ROLE_CUSTOMER);
        user.setEnabled(true);
        return userRepository.save(user);
    }
    
    public boolean usernameExists(String username) { return userRepository.existsByUsername(username); }
    public boolean emailExists(String email) { return userRepository.existsByEmail(email); }
    public List<User> getRecentUsers() { return userRepository.findTop10ByOrderByIdDesc(); }
    public long getTotalUsers() { return userRepository.count(); }
}