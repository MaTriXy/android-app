package com.blinkboxbooks.android.model.updateinfo;

import java.util.Comparator;

/**
 * Simple wrapper class for storing a version number as three integers:
 * major,minor1,minor2
 */
public class VersionInfo implements Comparator<VersionInfo> {

    public int major;
    public int minor1;
    public int minor2;

    /**
     * Construct a version number from its constituent version parts
     * @param major
     * @param minor1
     * @param minor2
     */
    public VersionInfo(int major, int minor1, int minor2) {
        this.major = major;
        this.minor1 = minor1;
        this.minor2 = minor2;
    }

    /**
     * Construct a version number from a string in the format "x_y_z", where the separator is passed in to this constructor
     * @param version the version string to parse
     * @param separatorRegEx the separator regular expression between each number
     * @throws IllegalArgumentException if the version string cannot be parsed correcty
     */
    public VersionInfo(String version, String separatorRegEx) throws IllegalArgumentException {
        try {
            String[] versionEntries = version.split(separatorRegEx);
            major = Integer.valueOf(versionEntries[0]);
            minor1 = Integer.valueOf(versionEntries[1]);
            minor2 = Integer.valueOf(versionEntries[2]);
        } catch (Exception e) {
            // If anything goes wrong just chuck an IllegalArgumentException
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Compares this object against another VersionInfo object
     * @return <0 if this version is lower, 0 if they are equal and >0 if this version is higher
     */
    public int compare(VersionInfo other) {
        return compare(this, other);
    }

    /**
     * Helper method that will strip the build number off a version number (e.g. 1.2.3-4 becomes 1.2.3)
     * @param versionNumber the version number with a build number field
     * @return the version number with the build number removed
     */
    public static String stripBuildNumber(String versionNumber) {
        int indexOfBuildNumber = versionNumber.indexOf("-");
        if (indexOfBuildNumber > 0) {
            return versionNumber.substring(0, indexOfBuildNumber);
        } else {
            // If there is no build number then just return the original value
            return versionNumber;
        }
    }

    @Override
    public int compare(VersionInfo vi1, VersionInfo vi2) {
        if (vi1.major < vi2.major) {
            return -1;
        } else if (vi1.major > vi2.major) {
            return 1;
        } else if (vi1.minor1 < vi2.minor1) {
            return -1;
        } else if (vi1.minor1 > vi2.minor1) {
            return 1;
        } else if (vi1.minor2 < vi2.minor2) {
            return -1;
        } else if (vi1.minor2 > vi2.minor2) {
            return 1;
        } else {
            return 0;
        }
    }
}