package com.vdt.log_monitor.alert;

import java.security.SecureRandom;

public class UidGenerator {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int UID_LENGTH = 14;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateUid() {
        StringBuilder sb = new StringBuilder(UID_LENGTH);
        for (int i = 0; i < UID_LENGTH; i++) {
            int index = RANDOM.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }
}