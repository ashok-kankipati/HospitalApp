package com.hospital.app.service;

import com.hospital.app.model.User;
import com.hospital.app.repository.UserRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {
    public static final List<String> ROLES = List.of("Admin", "Doctor", "Nurse", "Patient", "Receptionist", "Lab Technician", "Pharmacist", "Radiologist", "Surgeon");
    public record CreateAccount(@NotBlank @Size(min=3, max=100) @jakarta.validation.constraints.Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._-]{2,99}", message="Use 3 to 100 letters, digits, dots, underscores or hyphens; start with a letter or digit.") String username,
            @Email @com.hospital.app.validation.InputText(kind="email", required=true, max=254) String email,
            @NotBlank @Size(min=8, max=72) String password, @NotBlank String role) {}
    public record ChangeRole(@NotBlank String role) {}
    public record Account(Long id, String username, String email, String role, boolean active, String createdAt) {
        static Account from(User user) { return new Account(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), Boolean.TRUE.equals(user.getIsActive()), user.getCreatedAt()); }
    }
    private final UserRepository users;
    public AccountService(UserRepository users) { this.users = users; }

    private void requireAdmin(Long actorId) {
        var actor = users.findById(actorId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!Boolean.TRUE.equals(actor.getIsActive()) || !"Admin".equals(actor.getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can manage accounts.");
    }

    private List<User> lockAdmins(Long actorId) {
        var admins = users.lockActiveAdmins();
        if (admins.stream().noneMatch(user -> user.getId().equals(actorId))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can manage accounts.");
        return admins;
    }

    private void validateRole(String role) {
        if (!ROLES.contains(role)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a valid account role.");
    }

    @Transactional(readOnly=true)
    public List<Account> list(Long actorId) {
        requireAdmin(actorId);
        return users.findAll(org.springframework.data.domain.Sort.by("username")).stream().map(Account::from).toList();
    }

    @Transactional
    public Account create(Long actorId, CreateAccount request) {
        lockAdmins(actorId);
        validateRole(request.role());
        String username = request.username().trim();
        if (username.length() < 3) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must contain at least 3 characters.");
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes.");
        if (users.existsByUsername(username) || users.existsByEmail(request.email().trim())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email is already in use.");
        User user = new User();
        user.setUsername(username); user.setEmail(request.email().trim()); user.setRole(request.role());
        user.setPassword(AccountPasswords.encode(request.password())); user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now().toString());
        try { return Account.from(users.saveAndFlush(user)); }
        catch (DataIntegrityViolationException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email is already in use."); }
    }

    @Transactional
    public Account changeRole(Long actorId, Long id, String role) {
        var admins = lockAdmins(actorId);
        validateRole(role);
        User user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));
        if (actorId.equals(id) && !"Admin".equals(role)) throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot remove your own administrator role.");
        if ("Admin".equals(user.getRole()) && Boolean.TRUE.equals(user.getIsActive()) && !"Admin".equals(role) && admins.size() <= 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Keep at least one active administrator.");
        user.setRole(role);
        return Account.from(users.save(user));
    }

    @Transactional
    public void delete(Long actorId, Long id) {
        var admins = lockAdmins(actorId);
        if (actorId.equals(id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot delete your own administrator account.");
        User user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));
        if ("Admin".equals(user.getRole()) && Boolean.TRUE.equals(user.getIsActive()) && admins.size() <= 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Keep at least one active administrator.");
        users.delete(user);
        users.flush();
    }
}
