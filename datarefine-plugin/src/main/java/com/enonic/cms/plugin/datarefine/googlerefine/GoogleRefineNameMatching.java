package com.enonic.cms.plugin.datarefine.googlerefine;

/**
 * User: rfo
 * Date: 9/23/13
 * Time: 3:13 PM
 */
public abstract class GoogleRefineNameMatching {


    public String getFingerPrintName(String matchingName) {
        return FingerprintKeyer.getInstance().key(matchingName);
    }

    public String getNGramFingerprintName(String matchingName) {
        return NGramFingerprintKeyer.getInstance().key(matchingName);
    }

    public abstract String getMatchingName();
}
