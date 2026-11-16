package hotel.model;

public final class AdminAccount extends Account {
    private static final long serialVersionUID = 1L;

    public AdminAccount(String username, String fullName, String email, String phone,
                        byte[] salt, byte[] hash) {
        super(username, fullName, email, phone, salt, hash);
    }

    @Override
    public Role getRole() { return Role.ADMIN; }
}
