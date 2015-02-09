package com.blinkboxbooks.android.model.updateinfo;

/**
 * A simple wrapper class associating some update information with a particular version number
 */
public class UpdateInfo {

    public VersionInfo versionInfo;
    public String[] updates;

    public UpdateInfo(VersionInfo versionInfo, String[] updates) {
        this.versionInfo = versionInfo;
        this.updates = updates;
    }
}
