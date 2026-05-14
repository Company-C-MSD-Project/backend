package com.example.FixItNow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.repository.UserRepository;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.exception.ResourceNotFoundException;
import com.example.FixItNow.exception.BadRequestException;

import java.util.List;

/** User profile management (SRS FR3, FR5). */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    @Transactional
    public User updateProfile(Long id, User updates) {
        User user = findById(id);
        if (updates.getName()    != null) user.setName(updates.getName());
        if (updates.getPhone()   != null) user.setPhone(updates.getPhone());
        if (updates.getAddress() != null) user.setAddress(updates.getAddress());
        if (updates.getPhoto()   != null) user.setPhoto(updates.getPhoto());
        return userRepository.save(user);
    }

    /** Admin: suspend or reactivate a user account (SRS FR3). */
    @Transactional
    public User setActiveStatus(Long id, boolean active) {
        User user = findById(id);
        user.setActive(active);
        return userRepository.save(user);
    }

    /** Admin: blacklist a homeowner who has violated platform rules. */
    @Transactional
    public User blacklistHomeowner(Long id) {
        User user = findById(id);
        if (user.getUserType() != UserType.HOMEOWNER) {
            throw new BadRequestException("Only homeowners can be blacklisted.");
        }
        user.setBlacklisted(true);
        user.setActive(false);
        return userRepository.save(user);
    }

    /** Admin: approve a service provider after document verification. */
    @Transactional
    public User verifyProvider(Long id) {
        User user = findById(id);
        if (user.getUserType() != UserType.SERVICE_PROVIDER) {
            throw new BadRequestException("User is not a service provider.");
        }
        user.setVerified(true);
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByType(UserType type) {
        return userRepository.findByUserType(type);
    }
}
