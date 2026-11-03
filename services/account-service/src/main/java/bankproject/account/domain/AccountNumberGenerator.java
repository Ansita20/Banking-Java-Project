package bankproject.account.domain;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** 12-digit account numbers: bank prefix + random body + Luhn check digit (catches typos). */
@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private static final String BANK_PREFIX = "5001";

    private final AccountRepository accounts;
    private final SecureRandom random = new SecureRandom();

    public String next() {
        String candidate;
        do {
            String body = BANK_PREFIX + String.format("%07d", random.nextInt(10_000_000));
            candidate = body + luhnDigit(body);
        } while (accounts.existsById(candidate));
        return candidate;
    }

    public static boolean isValid(String accountNumber) {
        if (accountNumber == null || !accountNumber.matches("\\d{12}")) {
            return false;
        }
        String body = accountNumber.substring(0, 11);
        return luhnDigit(body) == accountNumber.charAt(11) - '0';
    }

    static int luhnDigit(String body) {
        int sum = 0;
        boolean dbl = true;
        for (int i = body.length() - 1; i >= 0; i--) {
            int d = body.charAt(i) - '0';
            if (dbl) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            dbl = !dbl;
        }
        return (10 - sum % 10) % 10;
    }
}
