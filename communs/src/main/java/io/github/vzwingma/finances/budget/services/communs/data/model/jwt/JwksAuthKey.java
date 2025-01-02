package io.github.vzwingma.finances.budget.services.communs.data.model.jwt;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonId;


/**
 * Clé de signature des tokens JWT
 */
@MongoEntity(collection = "jwks_auth_signing_keys")
@RegisterForReflection
@Setter @Getter
@NoArgsConstructor
public class JwksAuthKey {


    @BsonId
    private String kid;
    private String e;
    private String n;
    private String alg;
    private String kty;
    private String use;



    @Override
    public String toString() {
        return "JwksAuthKey{" +
                "keyId='" + kid + '\'' +
                '}';
    }
}
