package transmart.horde

import grails.converters.JSON
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.RandomStringUtils
import org.apache.log4j.Logger
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.codehaus.groovy.grails.web.util.WebUtils
import org.springframework.web.context.request.RequestContextHolder
import org.transmart.searchapp.AuthUser

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.security.*
import java.security.spec.X509EncodedKeySpec

class HordeSecurity {

    static def grailsApplication
    static def springSecurityService
    static def log = Logger.getLogger(HordeSecurity.class);
    static def keys = [:]

    HordeSecurity(def grailsApplication, def springSecurityService) {

        log.debug("Instantiating HordeSecurity ...")
        this.grailsApplication = grailsApplication
        this.springSecurityService = springSecurityService
        Security.addProvider(new BouncyCastleProvider());

        generateKeys()
        HordeHolder.identifyEndpoints()
    }

    static def getSymmetricKey() throws NoSuchAlgorithmException {

        log.debug("Generating AES Keys ...")
        new SecretKeySpec(RandomStringUtils.random(32, true, true).bytes, "AES")
    }

    static def getAsymmetricKeys() throws NoSuchAlgorithmException {

        [public: keys['pub'], private: keys['prv']]
    }

    static def convertPublicKey(pub) {

        def fact = KeyFactory.getInstance("RSA");
        fact.generatePublic(new X509EncodedKeySpec("$pub".decodeBase64()))
    }

    static def generateKeys() {

        log.debug("Generating Horde's Local RSA Keys ...")
        def generator = KeyPairGenerator.getInstance("RSA", "BC")
        generator.initialize(4096)
        def pair = generator?.genKeyPair()
        keys['prv'] = pair?.private
        keys['pub'] = pair?.public
        HordeHolder?.registerEndpoint(HordeHolder?.config?.uuid, keys['pub'].encoded?.encodeAsBase64())
    }

    static def gatherInput() {

        def source = null
        def data = null
        def input = IOUtils.toString(WebUtils?.retrieveGrailsWebRequest()?.currentRequest?.reader)
        if (!input.empty) {
            System.out.println("INPUT : $input")
            try {
                source = JSON.parse(input)
                data = unbolt(source?.data)
            } catch (e) {
                ;
            }
        }

        if (!data)
            data = [:]

        RequestContextHolder?.currentRequestAttributes()?.params?.each { k, v ->
            data[k] = v
        }

        System.out.println("DATA : $data")
        [source?.from, source?.pub ?: data]
    }

    static def gatherOutput(route, data) {
        System.out.println("REPLY TO ? : ${route}")
        System.out.println("PRESERVE ? : ${!!route}")
        if (route == null)
            System.out.println("OUTPUT ? : $data")
        else
            System.out.println("OUTPUT ? : ${bolt(route, data)}")
        System.out.println("")
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
        System.out.println("")
        return route == null ? data : bolt(route, data)
    }

    static def fetchUser(data) {

        def user = data?.user
        if (springSecurityService.isLoggedIn())
            user = springSecurityService?.getPrincipal()?.username

        user = AuthUser.findByUsername(user)
        if (!user && grailsApplication.config.com?.recomdata?.guestAutoLogin)
            user = AuthUser.findByUsername(grailsApplication.config.com?.recomdata?.guestUserName)

        user
    }

    static def bolt(uuid, params) {

        def result = [:]
        if (!uuid || !params)
            return result

        def message = "${params as JSON}"
        try {

            def key = symmetricKey
            def aes = Cipher.getInstance("AES")
            aes.init(Cipher.ENCRYPT_MODE, key)
            result['t'] = aes.doFinal(message.bytes).encodeAsBase64()
            def rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding", "BC")
            rsa.init(Cipher.ENCRYPT_MODE, (PublicKey) HordeHolder.endpoints[uuid]?.pub)
            result['k'] = rsa.doFinal(key.encoded).encodeAsBase64()

        } catch (e) {
            e.printStackTrace()
        }
        return result
    }

    static def unbolt(data) {

        def result = null;
        if (!data)
            return result

        if (!data?.t || !data?.k)
            return data

        try {

            def rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding", "BC")
            rsa.init(Cipher.DECRYPT_MODE, (PrivateKey) keys['prv'])
            def key = new SecretKeySpec(rsa.doFinal(data?.k?.decodeBase64()), "AES")
            def aes = Cipher.getInstance("AES")
            aes.init(Cipher.DECRYPT_MODE, key)
            result = JSON.parse(new String(aes.doFinal(data?.t?.decodeBase64())))

        } catch (Exception ex) {
            ex.printStackTrace()
        }

        return result
    }
}
