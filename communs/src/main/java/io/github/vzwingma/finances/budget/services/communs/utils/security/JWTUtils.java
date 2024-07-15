package io.github.vzwingma.finances.budget.services.communs.utils.security;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.*;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.EncodeException;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * Classe utilitaire pour le décodage et l'encodage des tokens JWT ID_TOKEN de Google.
 * Fournit des méthodes statiques pour transformer un token JWT de/vers sa représentation en Base64.
 */
public class JWTUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JWTUtils.class);


    protected static final String JWKS_GOOGLE_KEYS = """
            {
              "keys": [
                {
                  "e": "AQAB",
                  "n": "nzGsrziOYrMVYMpvUZOwkKNiPWcOPTYRYlDSdRW4UpAHdWPbPlyqaaphYhoMB5DXrVxI3bdvm7DOlo-sHNnulmAFQa-7TsQMxrZCvVdAbyXGID9DZYEqf8mkCV1Ohv7WY5lDUqlybIk1OSHdK7-1et0QS8nn-5LojGg8FK4ssLf3mV1APpujl27D1bDhyRb1MGumXYElwlUms7F9p9OcSp5pTevXCLmXs9MJJk4o9E1zzPpQ9Ko0lH9l_UqFpA7vwQhnw0nbh73rXOX2TUDCUqL4ThKU5Z9Pd-eZCEOatKe0mJTpQ00XGACBME_6ojCdfNIJr84Y_IpGKvkAEksn9w",
                  "alg": "RS256",
                  "kty": "RSA",
                  "kid": "87bbe0815b064e6d449cac999f0e50e72a3e4374",
                  "use": "sig"
                },
                {
                  "kid": "0e345fd7e4a97271dffa991f5a893cd16b8e0827",
                  "use": "sig",
                  "alg": "RS256",
                  "kty": "RSA",
                  "e": "AQAB",
                  "n": "rv95jmy91hibD7cb_BCA25jv5HrX7WoqHv-fh8wrOR5aYcM8Kvsc3mbzs2w1vCUlMRv7NdEGVBEnOZ6tHvUzGLon4ythd5XsX-wTvAtIHPkyHdo5zGpTgATO9CEn78Y-f1E8By63ttv14kXe_RMjt5aKttK4yqqUyzWUexSs7pET2zWiigd0_bGhJGYYEJlEk_JsOBFvloIBaycMfDjK--kgqnlRA8SWUkP3pEJIAo9oHzmvX6uXZTEJK10a1YNj0JVR4wZY3k60NaUX-KCroreU85iYgnecyxSdL-trpKdkg0-2OYks-_2Isymu7jPX-uKVyi-zKyaok3N64mERRQ"
                }
              ]
            }""";
    public static final String SHA_256_WITH_RSA = "SHA256withRSA";


    /**
     * Constructeur privé pour empêcher l'instanciation de la classe utilitaire.
     */
    private JWTUtils() {
    }

    /**
     * Décodage d'un token JWT à partir d'une chaîne en Base64.
     *
     * @param base64JWT Le token JWT encodé en Base64 à décoder.
     * @return Un objet {@link JWTAuthToken} représentant le token JWT décodé.
     * @throws DecodeException Si le token n'est pas bien formé ou si une erreur de décodage survient.
     */
    public static JWTAuthToken decodeJWT(String base64JWT) throws DecodeException {
        LOG.trace("Décodage du Token JWT : {}", base64JWT);

        Base64.Decoder decoder = Base64.getUrlDecoder();
        try {
            String[] chunks = base64JWT.split("\\.");
            String header = new String(decoder.decode(chunks[0]));
            String payload = new String(decoder.decode(chunks[1]));
            return new JWTAuthToken(Json.decodeValue(header, JwtAuthHeader.class),
                                    Json.decodeValue(payload, JWTAuthPayload.class),
                                    base64JWT);
        } catch (Exception e) {
            LOG.error("Erreur lors du décodage du token [{}]", base64JWT, e);
            throw new DecodeException("Erreur lors du décodage du token");
        }
    }

    /**
     * Encodage d'un objet {@link JWTAuthToken} en une chaîne JWT en Base64.
     *
     * @param jwt L'objet {@link JWTAuthToken} à encoder.
     * @return La chaîne JWT encodée en Base64.
     * @throws EncodeException Si une erreur d'encodage survient.
     */
    public static String encodeJWT(JWTAuthToken jwt) throws DecodeException {
        LOG.trace("Encodage du Token JWT : {}", jwt);

        Base64.Encoder encoder = Base64.getUrlEncoder();
        try {
            String headerJson = Json.encode(jwt.getHeader());
            String payloadJson = Json.encode(jwt.getPayload());
            String chunks0 = encoder.encodeToString(headerJson.getBytes()).replace("==", "");
            String chunks1 = encoder.encodeToString(payloadJson.getBytes()).replace("==", "");
            return chunks0 + "." + chunks1;
        } catch (Exception e) {
            LOG.error("Erreur lors de l'encodage du token [{}]", jwt);
            throw new EncodeException("Erreur lors de l'encodage du token");
        }
    }


    /**
     * Vérifie si la signature du token JWT est valide en utilisant les clés publiques de Google.
     *
     * @param jwtRawContent Le contenu brut du token JWT à vérifier.
     * @return true si la signature est valide, false sinon.
     */
    public static boolean isTokenSigValid(String jwtRawContent){
        JwksAuthKeys authKeys = Json.decodeValue(JWKS_GOOGLE_KEYS, JwksAuthKeys.class);

        for(JwksAuthKey key : authKeys.getKeys()){
            try {
                RSAPublicKey publicKey = (RSAPublicKey) getPublicKey(key.getN(), key.getE());

                String signedData = jwtRawContent.substring(0, jwtRawContent.lastIndexOf("."));
                String signatureB64u = jwtRawContent.substring(jwtRawContent.lastIndexOf(".")+1);
                byte[] signature = Base64.getUrlDecoder().decode(signatureB64u);

                Signature sig = Signature.getInstance(SHA_256_WITH_RSA);
                sig.initVerify(publicKey);
                sig.update(signedData.getBytes());
                if(sig.verify(signature)){
                    LOG.info("Le token est signé avec la clé publique : {}", key.getKid());
                    return true;
                }
            } catch (GeneralSecurityException e) {
                LOG.error("Erreur lors de la vérification de la signature du token JWT", e);
            }
        }
        LOG.error("Aucune clé publique n'a pu être utilisée pour vérifier la signature du token JWT");
        return false;
    }


    /**
     * Génère une clé publique RSA à partir de son module et de son exposant, tous deux encodés en Base64.
     *
     * @param modulus Le module de la clé, encodé en Base64.
     * @param exponent L'exposant de la clé, encodé en Base64.
     */
    private static PublicKey getPublicKey(String modulus, String exponent)  {
        try {
            byte[] exponentB = Base64.getUrlDecoder().decode(exponent);
            byte[] modulusB = Base64.getUrlDecoder().decode(modulus);
            BigInteger bigExponent = new BigInteger(1,exponentB);
            BigInteger bigModulus = new BigInteger(1,modulusB);

            PublicKey publicKey;
            publicKey = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(bigModulus, bigExponent));

            return publicKey;

        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {

            return null;
        }

    }

}
