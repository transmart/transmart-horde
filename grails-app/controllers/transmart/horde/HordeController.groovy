package transmart.horde

import grails.converters.JSON

class HordeController {

    def grailsApplication

    def identifier() {
        render [:] << [ uuid: grailsApplication.config.transmart?.horde?.uuid ?: '' ] as JSON
    }
}
