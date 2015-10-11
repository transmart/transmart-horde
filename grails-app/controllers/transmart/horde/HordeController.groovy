package transmart.horde

import grails.converters.JSON

class HordeController {

    def grailsApplication

    def identifier() {

        def (from, data) = HordeSecurity.gatherInput()
        HordeHolder.registerEndpoint(from, data)
        render [:] << [ uuid: HordeHolder.config?.uuid ?: '', pub: HordeSecurity.asymmetricKeys.public?.encoded?.encodeAsBase64()] as JSON
    }

    def forceReload() {

        HordeSecurity.generateKeys()
        HordeHolder.identifyEndpoints()
        redirect action: 'identifier'
    }

    def testEncryption() {

        def var = [user: "If you see this message, decryption went fine !"]
        def enc = HordeSecurity.bolt(HordeHolder.config?.uuid, var)
        def dec = HordeSecurity.unbolt(enc)
        System.out.println("ENC $enc")
        System.out.println("DEC ${dec?.user}")
        render [:] << [stat: HordeSecurity.keys['pub'] == HordeHolder.endpoints[HordeHolder.config?.uuid]?.pub, text: dec] as JSON
    }
}
