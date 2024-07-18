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
import java.util.List;

/**
 * Classe utilitaire pour le décodage et l'encodage des tokens JWT ID_TOKEN de Google.
 * Fournit des méthodes statiques pour transformer un token JWT de/vers sa représentation en Base64.
 */
public class JWTUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JWTUtils.class);

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
                                    chunks.length > 2,
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
    public static boolean isTokenSignatureValid(String jwtRawContent, List<JwksAuthKey> authKeys){
        LOG.trace("Vérification de la signature du Token JWT : {}", jwtRawContent);
        if(authKeys == null || authKeys.isEmpty()){
            LOG.error("Aucune clé publique n'a été fournie pour vérifier la signature du token JWT");
            return false;
        }
        for(JwksAuthKey key : authKeys){
            try {
                RSAPublicKey publicKey = (RSAPublicKey) getPublicKey(key.getN(), key.getE());

                String signedData = jwtRawContent.substring(0, jwtRawContent.lastIndexOf("."));
                String signatureB64u = jwtRawContent.substring(jwtRawContent.lastIndexOf(".")+1);
                byte[] signature = Base64.getUrlDecoder().decode(signatureB64u);

                Signature sig = Signature.getInstance(SHA_256_WITH_RSA);
                sig.initVerify(publicKey);
                sig.update(signedData.getBytes());
                if(sig.verify(signature)){
                    LOG.trace("Le token est signé avec la clé publique : {}", key.getKid());
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
