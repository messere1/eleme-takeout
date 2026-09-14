package cn.edu.tju.takeout.auth;

import cn.edu.tju.takeout.admin.AdminMapper;
import cn.edu.tju.takeout.merchant.MerchantMapper;
import cn.edu.tju.takeout.rider.RiderMapper;
import cn.edu.tju.takeout.user.UserMapper;
import org.springframework.stereotype.Service;

/** Validates account state on every authenticated request so disabling an account revokes old JWTs. */
@Service
public class AccountStatusService {
    private final UserMapper users;
    private final MerchantMapper merchants;
    private final RiderMapper riders;
    private final AdminMapper admins;

    public AccountStatusService(UserMapper users, MerchantMapper merchants,
            RiderMapper riders, AdminMapper admins) {
        this.users = users;
        this.merchants = merchants;
        this.riders = riders;
        this.admins = admins;
    }

    public boolean isUsable(Long id, String role) {
        return switch (role) {
            // Unknown IDs are left to resource authorization. Existing disabled accounts are rejected.
            case "CUSTOMER" -> users.findAnyById(id).map(user -> user.isEnabled()).orElse(true);
            case "MERCHANT" -> merchants.findById(id).map(merchant -> merchant.isEnabled()).orElse(true);
            case "RIDER" -> riders.findById(id).map(rider -> rider.isEnabled()).orElse(true);
            case "ADMIN" -> admins.findById(id).map(admin -> admin.isEnabled()).orElse(true);
            default -> false;
        };
    }
}
