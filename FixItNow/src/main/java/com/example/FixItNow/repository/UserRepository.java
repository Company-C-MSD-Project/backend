package com.example.FixItNow.repository;

import com.fixitnow.app.entity.User;
import com.fixitnow.app.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findByUserType(UserType userType);

    /** Find verified, active providers offering a specific service category. */
    @Query("SELECT u FROM User u WHERE u.userType = 'SERVICE_PROVIDER' " +
           "AND u.isVerified = true AND u.isActive = true " +
           "AND u.serviceCategory = :category")
    List<User> findVerifiedProvidersByCategory(@Param("category") String category);
}
