package transmart.horde

import grails.converters.JSON
import groovy.transform.Synchronized
import org.apache.log4j.Logger

class HordeHolder {

    static def restBuilder
    static def grailsApplication
    static def grailsLinkGenerator
    static def log = Logger.getLogger(HordeHolder.class);
    static def cache = [:]
    static def endpoints = [:]

    HordeHolder(def grailsApplication, def restBuilder, def grailsLinkGenerator) {

        log.debug("Instantiating HordeHolder ...")
        this.restBuilder = restBuilder
        this.grailsApplication = grailsApplication
        this.grailsLinkGenerator = grailsLinkGenerator

    }

    @Synchronized
    static synchronized def identifyEndpoints() {

        log.debug("Horde endpoints UUID detection ...")
        def link = grailsLinkGenerator.link(controller: 'horde', action: 'identifier', contextPath: '')
        config?.endpoints?.each { e ->
            log.debug("Indentifying '$e'")
            restBuilder.post("$e$link") {
                body([uuid: config?.uuid ?: '', pub: HordeSecurity.asymmetricKeys.public?.encoded?.encodeAsBase64()] as JSON)
            }?.json?.with { j ->
                registerEndpoint(j.uuid, j.pub, e)
            }
        }
    }

    static def registerEndpoint(uuid, pub, url = null) {
        if (!uuid || !pub)
            return
        log.debug("Adding '$uuid' : ${"$pub".take(20)} !")
        endpoints["$uuid"] = [url : url, pub: HordeSecurity.convertPublicKey(pub)]
    }

    static def getConfig() {
        grailsApplication.config.transmart?.horde
    }

    @Synchronized
    static synchronized def cache(k, v) {
        cache[k] = v
    }

    static def retrieve(k) {
        cache[k]
    }
}