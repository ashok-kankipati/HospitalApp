package com.hospital.app.config;

import java.util.Set;

/** Server policy: browser state and submitted role fields never grant permissions. */
public final class ApiPermissions {
    private ApiPermissions() {}
    private static boolean in(String role, String... allowed) { return Set.of(allowed).contains(role); }
    public static boolean allows(String role, String path, String method) {
        if (role == null) return false;
        if (role.equals("Admin")) return true;
        if (!in(role, "Doctor", "Surgeon", "Nurse", "Receptionist", "Pharmacist", "Lab Technician", "Radiologist")) return false;
        boolean read = method.equals("GET") || method.equals("HEAD");
        boolean clinical = in(role, "Doctor", "Surgeon", "Nurse");
        boolean operationalClinical = in(role, "Surgeon", "Nurse");
        if (path.equals("/api/auth/session")) return true;
        if (path.startsWith("/api/admin/") || path.equals("/api/auth/register")) return false;
        if (path.equals("/api/notifications/queue") || path.matches("/api/notifications/queue/[0-9]+/read")) return true;
        if (path.startsWith("/api/notifications/")) return false;
        if (path.equals("/api/staff") || path.startsWith("/api/staff/")) return read;
        if (path.equals("/api/patients") || path.matches("/api/patients/[0-9]+") || path.startsWith("/api/patients/search/"))
            return read || (method.equals("DELETE") ? false : clinical || role.equals("Receptionist"));
        if (path.startsWith("/api/patients/")) return clinical || (read && in(role, "Pharmacist", "Lab Technician", "Radiologist"));
        if (path.equals("/api/appointments") || path.startsWith("/api/appointments/")) return read || clinical || role.equals("Receptionist");
        if (path.equals("/api/visits") || path.startsWith("/api/visits/")) return clinical || (read && in(role, "Pharmacist", "Lab Technician", "Radiologist"));
        if (path.equals("/api/dispense") || path.startsWith("/api/dispense/"))
            return read ? operationalClinical || role.equals("Pharmacist") : role.equals("Pharmacist");
        if (path.startsWith("/api/pharmacy/")) {
            if (role.equals("Doctor")) {
                if (read) return path.equals("/api/pharmacy/medicines")
                        || path.equals("/api/pharmacy/batches")
                        || path.equals("/api/pharmacy/prescriptions");
                return method.equals("POST") && (path.equals("/api/pharmacy/prescriptions")
                        || path.equals("/api/pharmacy/prescription-items")
                        || path.matches("/api/pharmacy/prescriptions/[0-9]+/finalize"));
            }
            if (read) return operationalClinical || role.equals("Pharmacist");
            if (path.startsWith("/api/pharmacy/prescriptions") || path.startsWith("/api/pharmacy/prescription-items")) return role.equals("Surgeon");
            return role.equals("Pharmacist");
        }
        if (path.equals("/api/lab/tests") && role.equals("Doctor")) return read;
        if (path.startsWith("/api/lab/")) return read ? operationalClinical || in(role, "Lab Technician", "Radiologist") : in(role, "Lab Technician", "Radiologist");
        if (path.startsWith("/api/billing/")) return role.equals("Receptionist");
        if (path.equals("/api/beds/summary")) return read && (operationalClinical || role.equals("Receptionist"));
        if (path.equals("/api/beds") || path.startsWith("/api/beds/") || path.equals("/api/admissions") || path.startsWith("/api/admissions/")) return operationalClinical || role.equals("Receptionist");
        if (path.startsWith("/api/whatsapp/")) return clinical || role.equals("Receptionist");
        return false;
    }
}
