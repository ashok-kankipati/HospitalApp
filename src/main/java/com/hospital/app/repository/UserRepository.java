package com.hospital.app.repository;

import com.hospital.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByRole(String role);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.role = 'Admin' and u.isActive = true order by u.id")
    List<User> lockActiveAdmins();
}
