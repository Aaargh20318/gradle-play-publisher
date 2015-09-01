package de.triplet.gradle.play

class PlayPublishedVariantExtension {
    String name
    boolean autoRemoveLowerPrecedenceVersions = false;
    boolean  autoIncrementVersionCode = false
    private String track;

    def PlayPublishedVariantExtension(name) {
        this.name = name;
    }

    void setTrack(String track) {
        if (!(track in ['alpha', 'beta', 'rollout', 'production'])) {
            throw new IllegalArgumentException("Track has to be one of 'alpha', 'beta', 'rollout' or 'production'.")
        }

        this.track = track
    }

    def getTrack() {
        return track
    }

}
