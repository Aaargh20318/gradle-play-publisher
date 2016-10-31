package de.triplet.gradle.play

import org.gradle.api.NamedDomainObjectContainer

class PlayPublisherPluginExtension {



    String serviceAccountEmail

    File pk12File

    File jsonFile

    boolean uploadImages = false

    boolean errorOnSizeLimit = true

    boolean autoIncrementVersionCode = false

    boolean autoRemoveLowerPrecedenceVersions = false

    private String track = 'alpha'

    NamedDomainObjectContainer<PlayPublisherPluginExtension> variants;


    def PlayPublisherPluginExtension(variants) {
        this.variants=variants;
    }


    def variants(Closure closure) {
        variants.configure(closure)
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

    Double userFraction = 0.1


}
