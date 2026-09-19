package com.hospital.app.service;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TotpServiceTest {
    @Test
    void enrollmentEncryptsSecretAndCodesCannotBeReplayed() throws Exception {
        MemoryJdbc jdbc = new MemoryJdbc();
        TotpService service = new TotpService(jdbc);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "issuer", "CareFlow Test");
        ReflectionTestUtils.setField(service, "encryptionKey",
                Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        service.initialize();

                TotpService.Enrollment enrollment = service.beginEnrollment("doctor.one");
                assertTrue(enrollment.qrCode().startsWith("data:image/png;base64,"));
                assertFalse(enrollment.manualKey().isBlank());

                long counter = Instant.now().getEpochSecond() / 30;
                String currentCode = new DefaultCodeGenerator().generate(enrollment.secret(), counter);
                List<String> recoveryCodes = service.enroll(7L, enrollment.secret(), currentCode);

                assertEquals(8, recoveryCodes.size());
                assertNotEquals(enrollment.secret(), jdbc.encryptedSecret);
                assertFalse(jdbc.recoveryHashes.contains(recoveryCodes.get(0)));

                jdbc.lastUsedCounter = null;
                assertTrue(service.verify(7L, currentCode));
                assertFalse(service.verify(7L, currentCode));
                assertTrue(service.verify(7L, recoveryCodes.get(0)));
                assertFalse(service.verify(7L, recoveryCodes.get(0)));
        }

        private static class MemoryJdbc extends JdbcTemplate {
                private String encryptedSecret;
                private String recoveryHashes;
                private Long lastUsedCounter;

                @Override
                public void execute(String sql) {
                        // The test store already has the required shape.
                }

                @Override
                public int update(String sql, Object... args) {
                        if (sql.contains("INSERT INTO user_totp_mfa")) {
                                encryptedSecret = (String) args[1];
                                recoveryHashes = (String) args[2];
                                lastUsedCounter = ((Number) args[3]).longValue();
                        } else if (sql.contains("last_used_counter")) {
                                lastUsedCounter = ((Number) args[0]).longValue();
                        } else if (sql.contains("recovery_codes")) {
                                recoveryHashes = (String) args[0];
                        }
                        return 1;
                }

                @Override
                public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
                        if (encryptedSecret == null) return List.of();
                        try {
                                ResultSet result = mock(ResultSet.class);
                                when(result.getString(1)).thenReturn(encryptedSecret);
                                when(result.getString(2)).thenReturn(recoveryHashes);
                                when(result.getObject(3)).thenReturn(lastUsedCounter);
                                return List.of(rowMapper.mapRow(result, 0));
                        } catch (Exception exception) {
                                throw new IllegalStateException(exception);
                        }
                }
        }
}
