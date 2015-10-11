package transmart.horde

import grails.converters.JSON

class HordeConceptsController {

    def hordeConceptsResourceService

    def getCategories() {
        render hordeConceptsResourceService.allCategories as JSON
    }

    def getChildren() {
        render hordeConceptsResourceService.children as JSON
    }

    def getInitialAccess() {
        render hordeConceptsResourceService.initialAccess as JSON
    }

    def getChildConceptPatientCounts() {
        render hordeConceptsResourceService.childConceptPatientCounts as JSON
    }
}