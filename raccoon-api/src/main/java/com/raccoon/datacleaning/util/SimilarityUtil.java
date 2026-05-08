package com.raccoon.datacleaning.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import org.apache.commons.text.similarity.LevenshteinDistance;

/**
 * 相似度计算工具类
 */
public class SimilarityUtil {

    private static final LevenshteinDistance LEVENSHTEIN = new LevenshteinDistance();

    /**
     * 计算两个字符串的综合相似度
     * 
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 相似度 (0-1)
     */
    public static double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        
        if (s1.equals(s2)) {
            return 1.0;
        }

        // 1. 编辑距离相似度 (50%)
        double editSimilarity = calculateEditDistanceSimilarity(s1, s2);

        // 2. 拼音相似度 (30%)
        double pinyinSimilarity = calculatePinyinSimilarity(s1, s2);

        // 3. 子串包含关系 (20%)
        double substringScore = calculateSubstringScore(s1, s2);

        return editSimilarity * 0.5 + pinyinSimilarity * 0.3 + substringScore * 0.2;
    }

    /**
     * 计算编辑距离相似度
     */
    public static double calculateEditDistanceSimilarity(String s1, String s2) {
        int distance = LEVENSHTEIN.apply(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        
        if (maxLength == 0) {
            return 1.0;
        }
        
        return 1.0 - (double) distance / maxLength;
    }

    /**
     * 计算拼音相似度（中文特有）
     */
    public static double calculatePinyinSimilarity(String s1, String s2) {
        try {
            String pinyin1 = toPinyin(s1);
            String pinyin2 = toPinyin(s2);
            
            if (pinyin1.isEmpty() || pinyin2.isEmpty()) {
                return 0.0;
            }
            
            return calculateEditDistanceSimilarity(pinyin1, pinyin2);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * 计算子串包含得分
     */
    public static double calculateSubstringScore(String s1, String s2) {
        String longer = s1.length() >= s2.length() ? s1 : s2;
        String shorter = s1.length() < s2.length() ? s1 : s2;
        
        if (longer.contains(shorter)) {
            return 0.3;
        }
        
        // 检查是否有公共子串
        int commonLength = longestCommonSubstring(s1, s2);
        int minLength = Math.min(s1.length(), s2.length());
        
        if (minLength == 0) {
            return 0.0;
        }
        
        return (double) commonLength / minLength * 0.2;
    }

    /**
     * 转换为拼音
     */
    private static String toPinyin(String chinese) {
        try {
            HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
            format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
            
            StringBuilder pinyin = new StringBuilder();
            for (char c : chinese.toCharArray()) {
                if (Character.toString(c).matches("[\\u4E00-\\u9FA5]+")) {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        pinyin.append(pinyinArray[0]);
                    }
                } else {
                    pinyin.append(c);
                }
            }
            return pinyin.toString().toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 计算最长公共子串长度
     */
    private static int longestCommonSubstring(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int maxLength = 0;
        
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    maxLength = Math.max(maxLength, dp[i][j]);
                }
            }
        }
        
        return maxLength;
    }
}
