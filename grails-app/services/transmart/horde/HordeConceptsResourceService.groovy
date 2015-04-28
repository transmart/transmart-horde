package transmart.horde

import grails.transaction.Transactional
import org.apache.log4j.Logger
import org.springframework.web.context.request.RequestContextHolder
import org.transmart.searchapp.AuthUser

@Transactional
class HordeConceptsResourceService {

    def restBuilder
    def grailsApplication
    def i2b2HelperService
    def grailsLinkGenerator
    def springSecurityService
    def conceptsResourceService
    def log = Logger.getLogger(HordeHolder.class);

    def getAllCategories() {

        // Quick check to make sure we are not looping
        // And yes we know services are supposed to be stateless
        def from = RequestContextHolder?.currentRequestAttributes()?.params['from']
        if (from?.tokenize('|')?.findAll { it == HordeHolder.config?.uuid }?.size())
            return [:]

        // Get copies of both local and remote categories
        def localTree = conceptsResourceService.allCategories.jsonElement()
        def remoteTrees = allRemoteCategories

        filterCategories(localTree + remoteTrees)
    }

    def getChildren(parent) {

        // Quick check to make sure we are not looping
        // And yes we know services are supposed to be stateless
        def from = RequestContextHolder?.currentRequestAttributes()?.params['from']
        if (from?.tokenize('|')?.findAll { it == HordeHolder.config?.uuid }?.size())
            return [:]

        System.out.println("Stored value for $parent: ${HordeHolder.retrieve(parent)}")

        def remoteTrees = [].jsonElement()
        def link = grailsLinkGenerator.link(controller: 'hordeConcepts', action: 'getChildren', contextPath: '')

        from = "${HordeHolder.config?.uuid}${from ? "|${from}" : ''}"
        HordeHolder.retrieve(parent)?.tokenize('&')?.each { n ->
            if (n?.tokenize('|')?.size() > 1) {
                log.debug("Querying $parent on node: ${HordeHolder.endpoints[n?.tokenize('|')[1]]}$link")
                def url = "${HordeHolder.endpoints[n?.tokenize('|')[1]]}$link?from={f}&concept_key={k}"
                def var = [f: from, k: parent]
                remoteTrees += restBuilder.get(url, var)?.json ?: [:].jsonElement()
            }
            else {
                log.debug("Querying $parent locally")
                remoteTrees += conceptsResourceService.getByKey(parent).children.jsonElement()
            }
        }

        filterCategories(remoteTrees)
    }

    def getAllRemoteCategories() {

        def remoteTrees = [].jsonElement()
        def from = RequestContextHolder?.currentRequestAttributes()?.params['from']
        def link = grailsLinkGenerator.link(controller: 'hordeConcepts', action: 'getCategories', contextPath: '')

        from = "${HordeHolder.config?.uuid}${from ? "|${from}" : ''}"
        HordeHolder.config?.endpoints?.each { e ->
            remoteTrees += restBuilder.get("$e$link?from={f}", [f: from])?.json ?: [:].jsonElement()
        }

        remoteTrees
    }

    def filterCategories(def categories) {

        def filteredTree = [].jsonElement()

        categories.each { c ->
            c.hordeNode = "${HordeHolder.config?.uuid}${c.hordeNode ? "|${c.hordeNode}" : ''}"
        }.findAll { c ->
            c.containsKey('key')
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

        // Quick check to make sure we are not looping
        // And yes we know services are supposed to be stateless
        def from = RequestContextHolder?.currentRequestAttributes()?.params['from']
        if (from?.tokenize('|')?.findAll { it == HordeHolder.config?.uuid }?.size())
            return [:]

        def user = RequestContextHolder?.currentRequestAttributes()?.params['user'] ?: grailsApplication.config.com?.recomdata?.guestUserName
        if (springSecurityService.isLoggedIn())
            user = springSecurityService?.getPrincipal()?.username

        if (user && (user = AuthUser.findByUsername(user))) {

            def accesses = i2b2HelperService.getAccess(i2b2HelperService.getRootPathsWithTokens(), user).jsonElement()
            def link = grailsLinkGenerator.link(controller: 'hordeConcepts', action: 'getInitialAccess', contextPath: '')

            HordeHolder.config?.endpoints?.each { e ->
                System.out.println("Endpoint ($e) -> $user")
                System.out.println("Endpoint ($e) -> ${restBuilder.get("$e$link?user={u}", [u: user.username])?.json}")
                accesses += restBuilder.get("$e$link?user={u}", [u: user.username])?.json ?: [:].jsonElement()
            }

            System.out.println(accesses.jsonElement())

            return accesses.jsonElement()
        }

        [:].jsonElement()
    }

    def getChildConceptPatientCounts(concept) {

        def user = RequestContextHolder?.currentRequestAttributes()?.params['user'] ?: grailsApplication.config.com?.recomdata?.guestUserName
        if (springSecurityService.isLoggedIn())
            user = springSecurityService?.getPrincipal()?.username

        if (concept && user && (user = AuthUser.findByUsername(user))) {

            def results = [:]
            def link = grailsLinkGenerator.link(controller: 'hordeConcepts', action: 'getChildConceptPatientCounts', contextPath: '')

            results["counts"] = i2b2HelperService.getChildrenWithPatientCountsForConcept(concept)
            results["accesslevels"] = i2b2HelperService.getChildrenWithAccessForUserNew(concept, user)

            HordeHolder.config?.endpoints?.each { e ->
                (restBuilder.get("$e$link?user={u}&concept_key={k}", [u: user.username, k: concept])?.json ?: [:].jsonElement()).each { k, v ->
                    results[k] += v
                }
            }

            return results
        }

        [:].jsonElement()
    }
}
