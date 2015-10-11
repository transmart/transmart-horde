class TransmartHordeGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.3 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Transmart Horde Plugin" // Headline display name of the plugin
    def author = "Florian"
    def authorEmail = ""
    def description = ""

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/transmart-horde"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
        xmlns context:"http://www.springframework.org/schema/context"

        // We need to inject the RestBuilder with its bean declaration because its *crappy* constructor
        // would reinitialize the JSON marshaller we use later; rendering the application incompetent
        // It is important this falls first !
        restBuilder(grails.plugins.rest.client.RestBuilder)

        hordeConceptsResourceService(transmart.horde.HordeConceptsResourceService) {
            restBuilder = ref('restBuilder')
            grailsApplication = ref('grailsApplication')
            i2b2HelperService = ref('i2b2HelperService')
            grailsLinkGenerator = ref('grailsLinkGenerator')
            conceptsResourceService = ref('conceptsResourceService')
        }

        // We use an approach by constructor injection in this case to allow for class constructor code
        // It is cleaner than having the code here
        hordeHolder(transmart.horde.HordeHolder) { beanDefinition ->
            beanDefinition.constructorArgs = [ref('grailsApplication'), ref('restBuilder'), ref('grailsLinkGenerator')]
        }
        hordeSecurity(transmart.horde.HordeSecurity) { beanDefinition ->
            beanDefinition.dependsOn = ['hordeHolder']
            beanDefinition.constructorArgs = [ref('grailsApplication'), ref('springSecurityService')]
        }
    }

    def doWithDynamicMethods = { ctx ->
        Object.metaClass.jsonElement = { ->
            grails.converters.JSON.parse((delegate as grails.converters.JSON).toString(false))
        }
    }

    def doWithApplicationContext = { ctx ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
