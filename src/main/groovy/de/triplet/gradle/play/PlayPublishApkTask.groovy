package de.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.ApkListing
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TracksListResponse
import org.apache.commons.lang.StringUtils
import org.gradle.api.tasks.TaskAction

class PlayPublishApkTask extends PlayPublishTask {

    private static final String[] TRACK_PRECEDENCE=["alpha","beta","rollout","production"]


    static def MAX_CHARACTER_LENGTH_FOR_WHATS_NEW_TEXT = 500
    static def FILE_NAME_FOR_WHATS_NEW_TEXT = "whatsnew"

    String variationName
    File inputFolder

    @TaskAction
    publishApk() {
        super.publish()

        def apkOutput = variant.outputs.find { variantOutput -> variantOutput instanceof ApkVariantOutput }
        FileContent newApkFile = new FileContent(AndroidPublisherHelper.MIME_TYPE_APK, apkOutput.outputFile)

        Apk apk = edits.apks()
                .upload(variant.applicationId, editId, newApkFile)
                .execute()

        variationName = StringUtils.uncapitalize(variationName)
        def variantConfig = extension.variants.getAsMap().get(variationName)

        if(variantConfig?.autoRemoveLowerPrecedenceVersions || extension.autoRemoveLowerPrecedenceVersions) {

            // If the 'alpha' track has v1 we cannot publish v2 to the beta track because that would implicitly hide v1,
            // the Play API throws an error if we do. setting autoRemoveLowerPrecedenceVersions will remove the builds
            // from all tracks that have a lower precedence so there is no implicit hiding and we can upload a newer version
            // this is based on the assumption that the newer build is later version of the application anyway and allows
            // better integration with tooling to manage releases.


            int targetTrackWeight = trackWeight(extension.getTrack());

            // get version codes for tracks
            TracksListResponse tracksResponse = edits.tracks().list(variant.applicationId, editId).execute();
            List<Track> tracks = tracksResponse.getTracks()
            for (Track t : tracks) {
                int trackWeight = trackWeight(t.getTrack());
                boolean hasVersions = t.getVersionCodes().size() != 0
                if (hasVersions && targetTrackWeight > trackWeight) {
                    Track patch = new Track().setVersionCodes([apk.getVersionCode()])
                    patch.setVersionCodes(new LinkedList<Integer>());
                    t.setVersionCodes(new LinkedList<Integer>());
                    edits.tracks().patch(variant.applicationId, editId, t.getTrack(), patch).execute();
                    println "Removed versionCodes from " + t.getTrack() + ", it has higher precedence"
                }
            }
        }


        Track newTrack = new Track().setVersionCodes([apk.getVersionCode()])
        if (extension.track?.equals("rollout")) {
            newTrack.setUserFraction(extension.userFraction)
        }
        edits.tracks()
                .update(variant.applicationId, editId, extension.track, newTrack)
                .execute()

        if (inputFolder.exists()) {

            // Matches if locale have the correct naming e.g. en-US for play store
            inputFolder.eachDirMatch(matcher) { dir ->
                File whatsNewFile = new File(dir, FILE_NAME_FOR_WHATS_NEW_TEXT + "-" + extension.track)

                if (!whatsNewFile.exists()) {
                    whatsNewFile = new File(dir, FILE_NAME_FOR_WHATS_NEW_TEXT)
                }

                if (whatsNewFile.exists()) {

                    def whatsNewText = TaskHelper.readAndTrimFile(whatsNewFile, MAX_CHARACTER_LENGTH_FOR_WHATS_NEW_TEXT, extension.errorOnSizeLimit)
                    def locale = dir.name

                    ApkListing newApkListing = new ApkListing().setRecentChanges(whatsNewText)
                    edits.apklistings()
                            .update(variant.applicationId, editId, apk.getVersionCode(), locale, newApkListing)
                            .execute()
                }
            }

        }

        edits.commit(variant.applicationId, editId).execute()
    }

    int trackWeight(String trackName) {
        for(int i = 0; i < TRACK_PRECEDENCE.length; i++) {
            if(trackName.equalsIgnoreCase(TRACK_PRECEDENCE[i]))
                return i;
        }
        return -1;
    }


}
