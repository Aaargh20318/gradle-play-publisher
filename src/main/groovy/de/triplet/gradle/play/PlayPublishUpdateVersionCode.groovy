package de.triplet.gradle.play

import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.ApksListResponse
import org.apache.commons.lang.StringUtils
import org.gradle.api.tasks.TaskAction

class PlayPublishUpdateVersionCode extends PlayPublishTask {

    String variationName;

    @TaskAction
    updateVersionCode() {
        variationName = StringUtils.uncapitalize(variationName)
        def variantConfig = extension.variants.getAsMap().get(variationName)
        if(variantConfig?.autoIncrementVersionCode || extension.autoIncrementVersionCode) {
            super.publish()

            def maxVersionCode = 0;
            ApksListResponse apksList = edits.apks().list(variant.applicationId,editId).execute();
            for(Apk apk: apksList.getApks()) {
                def versionCode = apk.getVersionCode();
                if(versionCode > maxVersionCode) {
                    maxVersionCode = versionCode;
                }
            }

            println "Max version code = "+maxVersionCode
            variant.getMergedFlavor().versionCode=(maxVersionCode+1)
            println "Variant version code = "+variant.getVersionCode()
        }

    }


}