package io.github.vzwingma.finances.budget.services.communs.utils.security;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthPayload;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwtAuthHeader;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.EncodeException;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

/**
 * Classe utilitaire pour le décodage et l'encodage des tokens JWT ID_TOKEN de Google.
 * Fournit des méthodes statiques pour transformer un token JWT de/vers sa représentation en Base64.
 */
public class JWTUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JWTUtils.class);

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
            String signature = null;
            if(chunks.length > 3){
                signature = new String(decoder.decode(chunks[2]));
            }
            LOG.info("Header : {}", header);
            LOG.info("Payload : {}", payload);
            LOG.info("Signature : {}", signature);
            return new JWTAuthToken(Json.decodeValue(header, JwtAuthHeader.class),
                                    Json.decodeValue(payload, JWTAuthPayload.class),
                                    signature);
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
}
