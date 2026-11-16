package hotel.service;

import hotel.data.HotelDataStore;
import hotel.model.Account;
import hotel.model.AdminAccount;
import hotel.model.GuestAccount;
import hotel.model.HotelData;
import hotel.model.HotelException;
import hotel.model.Role;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class AuthenticationService {
    public static final String DEMO_ADMIN_USERNAME = "admin";
    private static final String INITIAL_ADMIN_PASSWORD_PROPERTY = "stayease.admin.initialPassword";
    private static final String INITIAL_ADMIN_PASSWORD_ENV = "STAYEASE_ADMIN_INITIAL_PASSWORD";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;

    private final HotelDataStore repository;
    private final HotelData data;
    private final SecureRandom random = new SecureRandom();

    public AuthenticationService(HotelDataStore repository, HotelData data) {
        this.repository = repository;
        this.data = data;
    }

    public void ensureAdministrator() throws IOException {
        boolean hasAdmin = data.getAccounts().stream().anyMatch(account -> account.getRole() == Role.ADMIN);
        if (!hasAdmin) {
            String initialPassword = initialAdminPassword();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            byte[] hash = hashPassword(initialPassword.toCharArray(), salt);
            data.getAccounts().add(new AdminAccount(DEMO_ADMIN_USERNAME, "Administrator StayEase",
                    "admin@stayease.local", "080000000000", salt, hash));
            repository.save(data);
            System.out.println("Akun admin StayEase dibuat. Username: " + DEMO_ADMIN_USERNAME
                    + " | Kata sandi awal: " + initialPassword);
        }
    }

    private String initialAdminPassword() {
        String configuredPassword = System.getProperty(INITIAL_ADMIN_PASSWORD_PROPERTY);
        if (configuredPassword == null || configuredPassword.isBlank()) {
            configuredPassword = System.getenv(INITIAL_ADMIN_PASSWORD_ENV);
        }
        if (configuredPassword != null && !configuredPassword.isBlank()) {
            if (configuredPassword.length() < 12) {
                throw new IllegalStateException(INITIAL_ADMIN_PASSWORD_ENV
                        + " harus memiliki minimal 12 karakter.");
            }
            return configuredPassword;
        }
        return "admin12345678";
    }

    public GuestAccount register(String username, String fullName, String email,
                                 String phone, char[] password) throws HotelException {
        try {
            if (username == null || !username.trim().matches("[A-Za-z0-9._-]{3,30}")) {
                throw new HotelException("Username harus 3–30 karakter (huruf, angka, titik, garis bawah, atau minus).");
            }
            if (password == null || password.length < 8) {
                throw new HotelException("Kata sandi harus memiliki minimal 8 karakter.");
            }
            String normalized = username.trim();
            boolean duplicate = data.getAccounts().stream().anyMatch(account ->
                    account.getUsername().equalsIgnoreCase(normalized)
                            || account.getEmail().equalsIgnoreCase(email == null ? "" : email.trim()));
            if (duplicate) throw new HotelException("Username atau email tersebut sudah terdaftar.");

            byte[] salt = new byte[16];
            random.nextBytes(salt);
            GuestAccount account = new GuestAccount(normalized, fullName, email, phone,
                    salt, hashPassword(password, salt));
            data.getAccounts().add(account);
            try {
                repository.save(data);
            } catch (IOException exception) {
                data.getAccounts().remove(account);
                throw new HotelException("Akun gagal disimpan: " + exception.getMessage(), exception);
            }
            return account;
        } catch (IllegalArgumentException exception) {
            throw new HotelException(exception.getMessage(), exception);
        } finally {
            if (password != null) Arrays.fill(password, '\0');
        }
    }

    public Account login(String username, char[] password) throws HotelException {
        try {
            String normalized = username == null ? "" : username.trim();
            Account account = data.getAccounts().stream()
                    .filter(item -> item.getUsername().equalsIgnoreCase(normalized))
                    .findFirst().orElse(null);
            if (account == null || password == null
                    || !MessageDigest.isEqual(account.getPasswordHash(),
                    hashPassword(password, account.getPasswordSalt()))) {
                throw new HotelException("Username atau kata sandi tidak sesuai.");
            }
            return account;
        } finally {
            if (password != null) Arrays.fill(password, '\0');
        }
    }

    private static byte[] hashPassword(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Fitur pengamanan kata sandi tidak tersedia.", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
