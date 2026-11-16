package hotel.model;

import java.io.Serializable;
import java.util.Arrays;

public abstract class Account implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String fullName;
    private final String email;
    private final String phone;
    private final byte[] passwordSalt;
    private final byte[] passwordHash;

    protected Account(String username, String fullName, String email, String phone,
                      byte[] passwordSalt, byte[] passwordHash) {
        this.username = required(username, "Username");
        this.fullName = required(fullName, "Nama");
        this.email = required(email, "Email");
        if (!this.email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Format email tidak valid.");
        }
        this.phone = required(phone, "Nomor telepon");
        this.passwordSalt = Arrays.copyOf(passwordSalt, passwordSalt.length);
        this.passwordHash = Arrays.copyOf(passwordHash, passwordHash.length);
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " wajib diisi.");
        }
        return value.trim();
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public byte[] getPasswordSalt() { return Arrays.copyOf(passwordSalt, passwordSalt.length); }
    public byte[] getPasswordHash() { return Arrays.copyOf(passwordHash, passwordHash.length); }
    public abstract Role getRole();
}
