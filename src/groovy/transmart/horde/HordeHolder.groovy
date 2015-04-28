package transmart.horde

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

        this.restBuilder = restBuilder
        this.grailsApplication = grailsApplication
        this.grailsLinkGenerator = grailsLinkGenerator

        identifyEndpoints()
    }

    @Synchronized
    static def identifyEndpoints() {

        log.debug("Horde endpoints UUID detection")
        def link = grailsLinkGenerator.link(controller: 'horde', action: 'identifier', contextPath: '')
        config?.endpoints?.each { e ->
            log.debug("Indentifying '$e' ...")
            restBuilder.get("$e$link")?.json?.uuid?.with { u ->
                endpoints[u] = e
            }
        }
    }

    static def getConfig() {
        grailsApplication.config.transmart?.horde
    }

    @Synchronized
    static def cache(k, v) {
        cache[k] = v
    }

    static def retrieve(k) {
        cache[k]
    }
}
