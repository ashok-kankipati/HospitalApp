package com.hospital.app.service.notification;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailAttachment {
    private String filename;
    private byte[] content;
    private String contentType;
}
