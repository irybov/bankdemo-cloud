package com.github.irybov.account;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import com.github.irybov.shared.BillDTO;

public record BillsResult(Set<BillDTO> bills, boolean unavailable) implements Serializable {
    public static BillsResult success(Set<BillDTO> bills) {
        return new BillsResult(bills, false);
    }
    public static BillsResult fallback() {
        return new BillsResult(new LinkedHashSet<>(), true);
    }
}
