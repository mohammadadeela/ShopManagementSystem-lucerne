package com.lucerne.model;

import java.time.LocalDateTime;
import java.util.Set;

public record UserAccount(
        int userId,
        String username,
        String fullName,
        Role role,
        Integer employeeId,
        Integer customerId,
        Integer branchId,
        Integer warehouseId,
        boolean active,
        boolean passwordChangeRequired,
        LocalDateTime loginTime,
        Set<String> permissions
) { }
