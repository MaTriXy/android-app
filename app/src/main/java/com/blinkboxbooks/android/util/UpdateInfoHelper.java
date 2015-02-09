package com.blinkboxbooks.android.util;

import android.content.Context;
import android.content.res.Resources;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.model.updateinfo.UpdateInfo;
import com.blinkboxbooks.android.model.updateinfo.VersionInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Update information will be associated with a particular version code and will have one or more information points.
 */
public class UpdateInfoHelper {

    private static final String UPDATE_INFO_PREFIX = "update_info_";

    /**
     * Get all update information from the values defined in the resource files.
     *
     * @return a list of UpdateInfo objects in version info order (lowest version to highest)
     */
    public static List<UpdateInfo> getAllUpdateInfo(Context context) {

        final Resources resources = context.getResources();
        List<UpdateInfo> updateInfoList = new ArrayList<UpdateInfo>();

        // Iterate all arrays within the resources and construct a new update info for each entry
        for (Field field : R.array.class.getDeclaredFields()) {
            try {
                if (field.getName().startsWith(UPDATE_INFO_PREFIX)) {
                    String versionString = field.getName().substring(UPDATE_INFO_PREFIX.length());
                    int id = field.getInt(null);
                    UpdateInfo updateInfo = new UpdateInfo(new VersionInfo(versionString, "_"), resources.getStringArray(id));
                    updateInfoList.add(updateInfo);
                }
            } catch (Exception e) {
                // If there are any issues parsing the update info then we just ignore it!
            }
        }

        // Now we ensure that the UpdateInfo objects or sorted in ascending order
        Collections.sort(updateInfoList, new Comparator<UpdateInfo>() {
            @Override
            public int compare(UpdateInfo ui1, UpdateInfo ui2) {
                // Just delegate the comparison to the version info objects
                return ui1.versionInfo.compare(ui2.versionInfo);
            }
        });

        return updateInfoList;
    }

    /**
     * Get the most appropriate update info to display based on the old version number and the current
     * version number. If there is no update information to display then this method will return null
     * @param context the application context
     * @param oldVersion the old version number that the user has upgraded from
     * @param currentVersion the current version number that the user has upgraded to
     * @return an UpdateInfo object or null if there is no update info to display
     */
    public static UpdateInfo getUpdateInfo(Context context, VersionInfo oldVersion, VersionInfo currentVersion) {
        List<UpdateInfo> updateInfoList = getAllUpdateInfo(context);

        UpdateInfo updateInfo = null;

        // Go through the version list (which is sorted in version order)
        for (UpdateInfo ui : updateInfoList) {

            // If the version of the update info is greater than the passed in current version then we bail out now.
            // This allows us to add in update info for future versions without the concern of it being displayed (if
            // we should ever wish to do such a thing)
            if (ui.versionInfo.compare(currentVersion) > 0) {
                break;
            }

            // Check if this version is less than the current version. If it is then we set this as the update info
            // to return, but this may be overridden if there is also some newer update info available.
            if (oldVersion.compare(ui.versionInfo) < 0) {
                updateInfo = ui;
            }
        }

        return updateInfo;
    }
}
