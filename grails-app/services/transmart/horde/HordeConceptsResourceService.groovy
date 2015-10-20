package transmart.horde

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.log4j.Logger
import org.springframework.web.context.request.RequestContextHolder

@Transactional
class HordeConceptsResourceService {

    def restBuilder
    def grailsApplication
    def i2b2HelperService
    def grailsLinkGenerator
    def conceptsResourceService
    def log = Logger.getLogger(HordeHolder.class);

    def getAllCategories() {

        def (from, data) = HordeSecurity.gatherInput()
        def route = from?.tokenize('|')?.first()

        // Quick check to make sure we are not looping
        // And yes we know services are supposed to be stateless
        if (from?.tokenize('|')?.findAll { it == HordeHolder.config?.uuid }?.size())
            return HordeSecurity.gatherOutput(route, [:])

        // Get copies of both local and remote categories
        def localTree = conceptsResourceService.allCategories.jsonElement()
        def remoteTrees = allRemoteCategories

        HordeSecurity.gatherOutput(route, filterCategories(localTree + remoteTrees))
    }

    def getAllRemoteCategories() {

        def remoteTrees = [].jsonElement()
        def from = RequestContextHolder?.currentRequestAttributes()?.params['from']
        def link = grailsLinkGenerator.link(controller: 'hordeConcepts', action: 'getCategories', contextPath: '')

        from = "${HordeHolder.config?.uuid}${from ? "|${from}" : ''}"
        HordeHolder.endpoints?.findAll { u, e ->
            e && e?.url && e?.url != "null"
        }?.each { u, e ->
            def url = "${e.url}$link"
            def var = [:]
            remoteTrees += HordeSecurity.unbolt(restBuilder.post(url) {
                body([from: from, data: HordeSecurity.bolt(u, var)] as JSON)
            }?.json)
        }

        remoteTrees
    }


    def getChildren() {

        def (from, data) = HordeSecurity.gatherInput()
        def route = from?.tokenize('|')?.first()

        // Quick check to make sure we are not looping
        // And yes we know services are supposed to be stateless
        if (from?.tokenize('|')?.findAll { it == HordeHolder.config?.uuid }?.size())
            return HordeSecurity.gatherOutput(route, [:])

        def remoteTrees = [].jsonElement()
        def link = grailsLinkGenerator.link(controller: 'hordeConcepts', action: 'getChildren', contextPath: '')

        from = "${HordeHolder.config?.uuid}${from ? "|${from}" : ''}"
        HordeHolder.retrieve(data?.concept_key)?.tokenize('&')?.each { n ->
            if (n?.tokenize('|')?.size() > 1) {
                def end = n?.tokenize('|')?.getAt(1)
                def url = "${HordeHolder.endpoints[end].url}$link"
                def var = [concept_key: data?.concept_key]
                remoteTrees += HordeSecurity.unbolt(restBuilder.post(url) {
                    body([from: from, data: HordeSecurity.bolt(end, var)] as JSON)
                }?.json ?: [:].jsonElement())
            } else {
                remoteTrees += conceptsResourceService.getByKey(data?.concept_key).children.jsonElement()
            }
        }

        return HordeSecurity.gatherOutput(route, filterCategories(remoteTrees))
    }

    def filterCategories(def categories) {

        def filteredTree = [].jsonElement()

        categories.each { c ->
            if (c != null)
                c.hordeNode = "${HordeHolder.config?.uuid}${c.hordeNode ? "|${c.hordeNode}" : ''}"
        }.findAll { c ->
            c != null && c.containsKey('key')
        }.each { c ->

            def element = filteredTree.find { f ->
                f.key == c.key
            }

            if (element) {
                element.hordeNode += "&${c.hordeNode}"
                element.visualAttributes.addAll(c.visualAttributes - element.visualAttributes)
                HordeHolder.cache(element.key, element.hordeNode)
            } else {
                filteredTree << c
                HordeHolder.cache(c.key, c.hordeNode)
            }
        }

        filteredTree
    }

    def getInitialAccess() {

        def (from, data) = HordeSecurity.gatherInput()
        def route = from?.tokenize('|')?.first()

        // Quick check to make sure we are not looping
        // And yes we know services are supposed to be stateless
        if (from?.tokenize('|')?.findAll { it == HordeHolder.config?.uuid }?.size())
            return HordeSecurity.gatherOutput(route, [:])

        def user = HordeSecurity.fetchUser data
        from = "${HordeHolder.config?.uuid}${from ? "|${from}" : ''}"
        if (user) {

            def accesses = i2b2HelperService.getAccess(i2b2HelperService.getRootPathsWithTokens(), user).jsonElement()
            def link = grailsLinkGenerator.link(controller: 'hordeConcepts', action: 'getInitialAccess', contextPath: '')
            HordeHolder.endpoints?.findAll { u, e ->
                e && e?.url && e?.url != "null"
            }?.each { u, e ->
                def url = "${e.url}$link"
                def var = [user: user.username]
                accesses += HordeSecurity.unbolt(restBuilder.post(url) {
                    body([from: from, data: HordeSecurity.bolt(u, var)] as JSON)
                }?.json) ?: [:].jsonElement()
            }

            return HordeSecurity.gatherOutput(route, accesses.jsonElement())
        }

        return HordeSecurity.gatherOutput(route, [:].jsonElement())
    }

    def getChildConceptPatientCounts() {

        def (from, data) = HordeSecurity.gatherInput()
        def route = from?.tokenize('|')?.first()

        // Quick check to make sure we are not looping
        // And yes we know services are supposed to be stateless
        if (from?.tokenize('|')?.findAll { it == HordeHolder.config?.uuid }?.size())
            return HordeSecurity.gatherOutput(route, [:])

        def user = HordeSecurity.fetchUser data
        def results = [:]
        if (data?.concept_key && user) {

            def link = grailsLinkGenerator.link(controller: 'hordeConcepts', action: 'getChildConceptPatientCounts', contextPath: '')

            from = "${HordeHolder.config?.uuid}${from ? "|${from}" : ''}"
            HordeHolder.retrieve(data?.concept_key)?.tokenize('&')?.each { n ->
                if (n?.tokenize('|')?.size() > 1) {
                    def end = n?.tokenize('|')[1]
                    def url = "${HordeHolder.endpoints[end].url}$link"
                    def var = [concept_key: data?.concept_key, user: user.username]
                    results << HordeSecurity.unbolt(restBuilder.post(url) {
                        body([from: from, data: HordeSecurity.bolt(end, var)] as JSON)
                    }?.json) ?: [:].jsonElement()
                } else {
                    results["counts"] = i2b2HelperService.getChildrenWithPatientCountsForConcept(data?.concept_key)
                    results["accesslevels"] = i2b2HelperService.getChildrenWithAccessForUserNew(data?.concept_key, user)
                }
            }
        }

        return HordeSecurity.gatherOutput(route, results)
    }
}
