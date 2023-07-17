package io.github.vzwingma.finances.budget.services.communs.data.trace;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class TestBusinessTraceContext {



    @Test
    void testTraceContext(){

        Map<String, String> contextMap = null;
        Assertions.assertNull(BusinessTraceContext.calculateBusinessContext(contextMap));

        contextMap = new HashMap<>();
        Assertions.assertNull(BusinessTraceContext.calculateBusinessContext(contextMap));

        contextMap.put("id1", "value1");
        Assertions.assertNull(BusinessTraceContext.calculateBusinessContext(contextMap));

        contextMap.put("idUser", "user1");
        Assertions.assertEquals("[idUser:user1]", BusinessTraceContext.calculateBusinessContext(contextMap));

        contextMap.put("idCompte", "compte2");
        Assertions.assertEquals("[idUser:user1,idCompte:compte2]", BusinessTraceContext.calculateBusinessContext(contextMap));


    }
}
