package com.signature.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * <p>
 * SHA-256工具类
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
public class Sha256Util {

    private static final String SHA_256 = "SHA-256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 对字符串进行SHA-256哈希
     *
     * @param input 输入字符串
     * @return 哈希后的字符串
     */
    public static String hash(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(input.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 验证字符串与哈希值是否匹配
     *
     * @param input 输入字符串
     * @param hash  哈希值
     * @return 是否匹配
     */
    public static boolean verify(String input, String hash) {
        if (input == null || hash == null) {
            return false;
        }
        return hash(input).equals(hash);
    }

    /**
     * 生成随机盐值
     *
     * @param length 盐值长度
     * @return 盐值
     */
    public static String generateSalt(int length) {
        byte[] salt = new byte[length];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 使用盐值进行哈希
     *
     * @param input 输入字符串
     * @param salt  盐值
     * @return 哈希后的字符串
     */
    public static String hashWithSalt(String input, String salt) {
        if (input == null || salt == null) {
            return null;
        }
        return hash(input + salt);
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * 生成安全的随机字符串
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String generateSecureRandomString(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
