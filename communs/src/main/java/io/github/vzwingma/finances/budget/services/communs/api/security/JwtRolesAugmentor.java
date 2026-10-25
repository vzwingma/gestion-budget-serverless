package io.github.vzwingma.finances.budget.services.communs.api.security;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Complète l'identité issue de {@code quarkus-oidc} (signature, {@code aud}, {@code exp} déjà validés par l'extension) :
 * <ul>
 *   <li>contrôle explicite de l'issuer Google (égalité stricte, deux formes acceptées) : l'extension est configurée avec
 *   {@code quarkus.oidc.token.issuer=any} car Google émet des {@code id_token} avec {@code accounts.google.com} ou
 *   {@code https://accounts.google.com} alors que la discovery n'annonce que la seconde ;</li>
 *   <li>exigence d'un claim {@code email} non vide et {@code email_verified} à {@code true} (booléen ou chaîne) ;</li>
 *   <li>attribution des rôles applicatifs ({@code app.security.roles}) à tout utilisateur authentifié, comme l'ancien
 *   {@code JwtSecurityContext#isUserInRole}.</li>
 * </ul>
 */
@ApplicationScoped
public class JwtRolesAugmentor implements SecurityIdentityAugmentor {

    private static final Logger LOG = LoggerFactory.getLogger(JwtRolesAugmentor.class);

    static final Set<String> GOOGLE_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    @ConfigProperty(name = "app.security.roles", defaultValue = "USER_COMPTES,USER_OPERATIONS,USER_UTILISATEURS")
    Set<String> roles;

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous() || !(identity.getPrincipal() instanceof JsonWebToken jwt)) {
            return Uni.createFrom().item(identity);
        }
        String iss = jwt.getIssuer();
        if (iss == null || !GOOGLE_ISSUERS.contains(iss)) {
            LOG.warn("Token refusé : issuer inattendu");
            return Uni.createFrom().failure(new AuthenticationFailedException("Issuer invalide"));
        }
        Object email = jwt.getClaim("email");
        if (email == null || email.toString().isBlank()) {
            LOG.warn("Token refusé : claim email absent");
            return Uni.createFrom().failure(new AuthenticationFailedException("Email absent"));
        }
        if (!isEmailVerified(jwt.getClaim("email_verified"))) {
            LOG.warn("Token refusé : email non vérifié");
            return Uni.createFrom().failure(new AuthenticationFailedException("Email non vérifié"));
        }
        return Uni.createFrom().item(QuarkusSecurityIdentity.builder(identity).addRoles(roles).build());
    }

    /**
     * Google peut émettre {@code email_verified} en booléen ou en chaîne : les deux formes {@code true} sont acceptées.
     */
    private static boolean isEmailVerified(Object claim) {
        return Boolean.TRUE.equals(claim) || (claim instanceof CharSequence cs && "true".equals(cs.toString()));
    }
}
