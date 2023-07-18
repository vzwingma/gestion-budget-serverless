package io.github.vzwingma.finances.budget.services.communs.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;


@RegisterForReflection
@JsonDeserialize @JsonSerialize
@Getter @Setter
public class JWTAuthToken {

    private static final Logger LOG = LoggerFactory.getLogger(JWTAuthToken.class);
    private JwtAuthHeader header;

    private JWTAuthPayload payload;

    public JWTAuthToken(JwtAuthHeader header, JWTAuthPayload payload){
        this.header = header;
        this.payload = payload;
    }


    @JsonIgnore
    public LocalDateTime issuedAt(){
        if(this.payload != null && this.payload.getIat() != 0){
            return LocalDateTime.ofEpochSecond(this.getPayload().getIat(), 0, ZoneId.of("Europe/Berlin").getRules().getOffset(LocalDateTime.now()));
        }
        return null;
    }
    @JsonIgnore
    public LocalDateTime expiredAt(){
        if(this.payload != null && this.payload.getExp() != 0){
            return LocalDateTime.ofEpochSecond(this.getPayload().getExp(),0, ZoneId.of("Europe/Berlin").getRules().getOffset(LocalDateTime.now()));
        }
        return null;
    }

    /**
     *
     * @return l'expiration
     */
    public boolean isExpired(){
        LocalDateTime expAt = expiredAt();
        if(expAt != null){
            return !LocalDateTime.now().isBefore(expAt);
        }
        return false;
    }

    @Override
    public String toString() {
        return "JWTIdToken{" +
                ", payload=" + payload +
                ", isExpired=" + isExpired() +
                '}';
    }
}
