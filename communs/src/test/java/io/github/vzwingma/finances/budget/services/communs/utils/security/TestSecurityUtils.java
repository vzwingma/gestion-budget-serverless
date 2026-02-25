package io.github.vzwingma.finances.budget.services.communs.utils.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de SecurityUtils
 */
class TestSecurityUtils {

    @Test
    void testEscapeInputRegex() {
        assertNotNull(SecurityUtils.ESCAPE_INPUT_REGEX);
        // Vérifie que la regex matche \n, \r, \t
        assertTrue("\n".matches(SecurityUtils.ESCAPE_INPUT_REGEX));
        assertTrue("\r".matches(SecurityUtils.ESCAPE_INPUT_REGEX));
        assertTrue("\t".matches(SecurityUtils.ESCAPE_INPUT_REGEX));
        assertFalse("a".matches(SecurityUtils.ESCAPE_INPUT_REGEX));
    }

    @Test
    void testEscapeInputRegexRemplacement() {
        String input = "injection\nattack\r\ttest";
        String result = input.replaceAll(SecurityUtils.ESCAPE_INPUT_REGEX, "_");
        assertFalse(result.contains("\n"));
        assertFalse(result.contains("\r"));
        assertFalse(result.contains("\t"));
        assertTrue(result.contains("_"));
    }
}

