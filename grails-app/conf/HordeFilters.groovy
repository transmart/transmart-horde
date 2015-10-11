class HordeFilters {

    def hordeConceptsResourceService

    def filters = {

        conceptsGetCategories(controller: 'concepts', action: 'getCategories') {
            before = {
                System.out.println("conceptsGetCategories @: $params")
                redirect(controller: 'hordeConcepts', action: 'getCategories')
                return false
            }
            after = { Map model ->
            }
            afterView = { Exception e ->
            }
        }

        conceptsGetChildren(controller: 'concepts', action: 'getChildren') {
            before = {
                System.out.println("conceptsGetChildren @: $params")
                redirect(controller: 'hordeConcepts', action: 'getChildren', params: params)
                return false
            }
            after = { Map model ->
            }
            afterView = { Exception e ->
            }
        }

        initialAccess(controller: 'datasetExplorer', action: 'index') {
            before = {
            }
            after = { Map model ->
                model.initialaccess = hordeConceptsResourceService.initialAccess.toString()
            }
            afterView = { Exception e ->
            }
        }

        chartChildConceptPatientCounts(controller: 'chart', action: 'childConceptPatientCounts') {
            before = {
                System.out.println("chartChildConceptPatientCounts @: $params")
                redirect(controller: 'hordeConcepts', action: 'getChildConceptPatientCounts', params: params)
                return false
            }
            after = { Map model ->
            }
            afterView = { Exception e ->
            }
        }

    }
}
