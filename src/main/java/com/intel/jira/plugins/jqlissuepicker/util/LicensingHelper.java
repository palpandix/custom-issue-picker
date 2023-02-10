package com.intel.jira.plugins.jqlissuepicker.util;

import com.atlassian.upm.api.license.PluginLicenseManager;
import com.atlassian.upm.api.license.entity.LicenseError;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.atlassian.upm.api.util.Option;
import org.apache.log4j.Logger;

public final class LicensingHelper {
    public static final boolean LICENSING_ACTIVATED = true;
    private static final Logger LOG = Logger.getLogger(LicensingHelper.class);
    private static final int LICENSE_TYPE_MISMATCH = 5;
    private static final int LICENSE_USER_MISMATCH = 4;
    private static final int LICENSE_VERSION_MISMATCH = 3;
    private static final int LICENSE_EXPIRED = 2;
    private static final int LICENSE_VALID = 1;
    private static final int LICENSE_FAILURE = 0;
    private final PluginLicenseManager licenseManager;

    public LicensingHelper(PluginLicenseManager licenseManager) {
        this.licenseManager = licenseManager;
    }

    public boolean isLicensed() {
        boolean isLicensed = true;//false;
        /*
        if (this.getLicenseInfo() == 1) {
            isLicensed = true;
        }
        */
        return isLicensed;
    }

    public int getLicenseInfo() {
        int licenseInfo;
        try {
            PluginLicense pluginLicense = this.getPluginLicense();
            if (pluginLicense != null) {
                if (pluginLicense.isValid()) {
                    LOG.debug("[getLicenseInfo] License is valid");
                    licenseInfo = 1;
                } else {
                    licenseInfo = getLicenseInfoForInvalidLicense(pluginLicense);
                }
            } else {
                LOG.debug("[getLicenseInfo] No license found");
                licenseInfo = 0;
            }
        } catch (Exception var3) {
            LOG.debug("[getLicenseInfo] Error while retrieving license.", var3);
            licenseInfo = 0;
        }

        return licenseInfo;
    }

    private static int getLicenseInfoForInvalidLicense(PluginLicense pluginLicense) {
        Option<LicenseError> errorOption = pluginLicense.getError();
        byte licenseInfo;
        if (errorOption.isDefined()) {
            LicenseError error = (LicenseError)errorOption.get();
            LOG.debug("[getLicenseInfo] License is not valid, error is: " + error);
            switch (error) {
                case EXPIRED:
                    licenseInfo = 2;
                    break;
                case TYPE_MISMATCH:
                    licenseInfo = 5;
                    break;
                case USER_MISMATCH:
                    licenseInfo = 4;
                    break;
                case VERSION_MISMATCH:
                    licenseInfo = 3;
                    break;
                case ROLE_EXCEEDED:
                case ROLE_UNDEFINED:
                case EDITION_MISMATCH:
                default:
                    licenseInfo = 0;
            }
        } else {
            LOG.debug("[getLicenseInfo] License is not valid, but no error has been specified");
            licenseInfo = 0;
        }

        return licenseInfo;
    }

    private PluginLicense getPluginLicense() {
        PluginLicense pluginLicense = null;
        if (this.licenseManager.getLicense().isDefined()) {
            pluginLicense = (PluginLicense)this.licenseManager.getLicense().get();
        }

        return pluginLicense;
    }
}
