package com.hospital.app.controller;

import com.hospital.app.dto.LoginResponse;
import com.hospital.app.service.AccountService;
import com.hospital.app.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/accounts")
public class AccountController {
    private final AccountService accounts;
    private final UserService users;

    public AccountController(AccountService accounts, UserService users) {
        this.accounts = accounts;
        this.users = users;
    }

    static Long actorId(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again.");
        Object id = session.getAttribute(LoginController.ACCOUNT_ID);
        if (!(id instanceof Long accountId) || session.getAttribute(LoginController.AUTHENTICATED_USER) == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again.");
        return accountId;
    }

    @GetMapping public List<AccountService.Account> list(HttpServletRequest request) { return accounts.list(actorId(request)); }
    @PostMapping public ResponseEntity<AccountService.Account> create(@Valid @RequestBody AccountService.CreateAccount body, HttpServletRequest request) {
        return ResponseEntity.status(201).body(accounts.create(actorId(request), body));
    }
    @PutMapping("/{id}/role") public AccountService.Account changeRole(@PathVariable Long id, @Valid @RequestBody AccountService.ChangeRole body, HttpServletRequest request) {
        return accounts.changeRole(actorId(request), id, body.role());
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        accounts.delete(actorId(request), id);
        return ResponseEntity.noContent().build();
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> failure(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message", exception.getReason() == null ? "Request could not be completed." : exception.getReason()));
    }
}
