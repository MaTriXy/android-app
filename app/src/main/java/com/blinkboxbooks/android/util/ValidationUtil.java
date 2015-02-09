// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import com.blinkboxbooks.android.BusinessRules;
import com.blinkboxbooks.android.api.BBBApiConstants;

import java.util.Calendar;
import java.util.regex.Pattern;

/**
 * Utility class for validating various values
 */
public class ValidationUtil {

    private static Pattern sEmailPattern;
    private static Pattern sPostcodePattern;
    private static final int BASE = 10;
    private static final int WEIGHT_FACTOR_OP1 = 1;
    private static final int WEIGHT_FACTOR_OP2 = 2;

    public static final int VALIDATION_CVV_LENGTH = 3;
    public static final int VALIDATION_MIN_CARD_LENGTH = 16;

    /**
     * Validates an email address checking it confirms to predefined regular expression
     *
     * @param email the email address you want to validate
     * @return true if valid else false
     */
    public static boolean validateEmail(String email) {

        if (email == null) {
            return false;
        }

        if (sEmailPattern == null) {
            sEmailPattern = Pattern.compile(BusinessRules.REGULAR_EXPRESSION_EMAIL);
        }

        return sEmailPattern.matcher(email).matches();
    }

    /**
     * Validates a first or last name ensuring it does not exceed a maximum length
     *
     * @param name the name you want to validate
     * @return true if valid else false
     */
    public static boolean validateName(String name) {

        if (name == null) {
            return false;
        }

        if (name.length() > BusinessRules.NAME_MAX_LENGTH || name.length() < BusinessRules.NAME_MIN_LENGTH) {
            return false;
        }

        return true;
    }

    /**
     * Validates a password ensuring it meets minimum length requirements
     *
     * @param password the password you want to validate
     * @return true if valid else false
     */
    public static boolean validatePassword(String email, String password) {

        if (password == null) {
            return false;
        }

        if (password.length() < BusinessRules.PASSWORD_MIN_LENGTH) {
            return false;
        }

        if (email != null && email.equals(password)) {
            return false;
        }

        return true;
    }

    public static boolean validatePasswordMatch(String password1, String password2) {
        return password1.equals(password2);
    }

    /**
     * Calculates the entropy of the given password. If the password is less than 6 characters we return 0.
     *
     * @param password the password you want to check
     * @return the calculated entropy
     */
    public static double calculatePasswordStrength(String password) {

        int length = password.length();

        if (length < 6) {
            return 0;
        }

        int score = 0;
        int c;

        boolean hasLowerCase = false, hasUpperCase = false, hasSpecialChars = false, hasNumbers = false;

        for (int i = 0; i < length; i++) {
            c = (int) password.charAt(i);

            if (!hasNumbers) {

                if (c >= 48 && c <= 57) {
                    hasNumbers = true;
                    score += 10;
                }
            }

            if (!hasLowerCase) {

                if (c >= 97 && c <= 122) {
                    hasLowerCase = true;
                    score += 26;
                }
            }

            if (!hasUpperCase) {

                if (c >= 65 && c <= 90) {
                    hasUpperCase = true;
                    score += 26;
                }
            }

            if (!hasSpecialChars) {

                if (!(c >= 65 && c <= 90) && !(c >= 97 && c <= 122) && !(c >= 48 && c <= 57)) {
                    hasSpecialChars = true;
                    score += 33;
                }
            }
        }

        double entropy = (length * Math.log10(score)) / Math.log10(2);

        return entropy;
    }

    /**
     * validates given card number according to the following check sum rule.
     * <p/>
     * <pre>Card Number (without check digit)
     * 6	3	4	0	0	4	0	0	0	2	3	2	3	1	3	9	2</pre>
     * <pre>Weighting
     * 2	1	2	1	2	1	2	1	2	1	2	1	2	1	2	1	2</pre>
     * <pre>Digit x weighting
     * 12	3	8	0	0	4	0	0	0	2	6	2	6	1	6	9	4</pre>
     * <pre>Add digits together
     * 3	3	8	0	0	4	0	0	0	2	6	2	6	1	6	9	4</pre>
     * <pre>Add up these digits
     * 3	6	14	14	14	18	18	18	18	20	26	28	34	35	41	50	5<b>4</b></pre>
     * <pre>Subtract the last digit from 10</pre>
     * <pre>See below	10 – <b>4</b> = 6</pre>
     * <pre>This is the check digit	6</pre>
     *
     * @param cardNumber clubcard number
     * @return true if given card number's check sum is successful
     */
    public static boolean isClubcardValid(Long cardNumber) {

        try {
            long checkDigit = cardNumber % BASE;
            cardNumber = cardNumber / BASE;
            int digitsSize = digitsSize(cardNumber);
            int[] digitXWeighted = new int[digitsSize];
            int[] digitXAddedUp = new int[digitsSize];
            short pos = 0;
            int remaining;
            int weighting;

            //calculate digit x weighted with the weight factor
            while (cardNumber > 0) {
                remaining = (int) (cardNumber % BASE);
                cardNumber /= BASE;
                weighting = (pos % 2 == 0) ? WEIGHT_FACTOR_OP2 : WEIGHT_FACTOR_OP1;
                digitXWeighted[pos] = weighting * remaining;
                pos++;
            }

            // calculate sum of each digit
            while (pos > 0) {
                int weightedNumber = digitXWeighted[pos - 1];
                digitXAddedUp[pos - 1] = sumDigit(weightedNumber);
                pos--;
            }

            int sum = 0;
            while (pos < digitsSize) {
                sum += digitXAddedUp[pos];
                pos++;
            }

            int lastDigit = sum % BASE;
            int controlDigit = BASE - lastDigit;

            return controlDigit == checkDigit;
        } catch (Exception e) {
            return false;
        }
    }

    //modulo 9 for sum of digits for small numbers calculation yields the correct result up to number 18
    private static int sumDigit(int n) {
        return (n % 9 == 0 && n != 0) ? 9 : n % 9;
    }

    /**
     * Validates a tesco clubcard
     *
     * @param cardNumber the number you want to validate
     * @return true if valid else false
     */
    public static boolean validateTescoClubcardNumber(String cardNumber) {

        try {
            long number = Long.parseLong(cardNumber);

            return isClubcardValid(number);
        } catch (NumberFormatException e) {
        }

        return false;
    }

    public static int digitsSize(long x) {
        return 1 + (int) Math.floor(Math.log10(x));
    }

    public static boolean validateMonth(String month) {
        try {
            int monthNum = Integer.valueOf(month);
            if (monthNum > 0 && monthNum <= 12) {
                return true;
            }
        } catch (NumberFormatException e) {
        }
        return false;
    }

    public static boolean validateCVC(String cvc) {
        cvc = cvc.replaceAll("\\s+", "");
        return cvc.length() == 3;
    }

    public static boolean validateYear(String year) {
        Calendar date = Calendar.getInstance();
        int startYear = date.get(Calendar.YEAR);
        try {
            int yearNum = Integer.valueOf(year);
            if (yearNum >= startYear) {
                return true;
            }
        } catch (NumberFormatException e) {
        }
        return false;
    }

    public static boolean validateField(String field) {
        field = field.replaceAll("\\s+", "");
        return (field.length() > 0);
    }

    public static boolean validatePostcode(String postcode) {
        if (sPostcodePattern == null) {
            sPostcodePattern = Pattern.compile(BusinessRules.REGULAR_EXPRESSION_UK_POSTCODE);
        }

        return sPostcodePattern.matcher(postcode).matches();
    }

    public static String getCreditCardType(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 2) {
            return null;
        }

        if (cardNumber.startsWith("4")) {
            return BBBApiConstants.PARAM_CARDTYPE_VISA;
        } else if (cardNumber.startsWith("5")) {
            return BBBApiConstants.PARAM_CARDTYPE_MASTERCARD;
        }

        return null;
    }

    /**
     * Check a card number with the LUHN algorithm
     * http://en.wikipedia.org/wiki/Luhn_algorithm
     *
     * @param cardNumber
     * @return true, if the card is valid
     */
    public static boolean validateCreditCardNumber(String cardNumber) {
        cardNumber = cardNumber.replaceAll("\\s+", "");
        if (cardNumber.length() < VALIDATION_MIN_CARD_LENGTH) {
            return false;
        }
        int oddEven = cardNumber.length() % 2;
        int length = cardNumber.length();
        int total = 0;
        for (int i = 0; i < length; i++) {
            int charValue = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (i % 2 == oddEven) {
                charValue = charValue * 2;
                if (charValue > 9) {
                    charValue = charValue - 9;
                }
            }
            total += charValue;
        }

        if (total % 10 == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if a String contains numbers
     *
     * @param str the String you want to check
     * @return true if the String contains numbers else false
     */
    public static boolean containsNumbers(String str) {

        if (str != null && str.matches(".*\\d.*")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if a String contains upper and lower case characters
     *
     * @param str the String you want to check
     * @return true if the String contains uppper and lower case characters else false
     */
    public static boolean containsUppercaseAndLowercase(String str) {

        if (str == null) {
            return false;
        }

        int length = str.length();

        boolean containsLower = false, containsUpper = false;
        char c;

        for (int i = 0; i < length; i++) {
            c = str.charAt(i);

            containsLower |= Character.isLowerCase(c);
            containsUpper |= Character.isUpperCase(c);

            if (containsLower && containsUpper) {
                return true;
            }
        }

        return false;
    }
}