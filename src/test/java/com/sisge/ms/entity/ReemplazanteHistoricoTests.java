package com.sisge.ms.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ReemplazanteHistoricoTests {
    @Test
    void derivesDniFromCuil() {
        assertEquals("12345678", ReemplazanteHistorico.dniFromCuil("20-12345678-6"));
        assertEquals("01234567", ReemplazanteHistorico.dniFromCuil("20-01234567-3"));
        assertEquals("12345678", ReemplazanteHistorico.dniFromCuil("20123456786"));
        assertNull(ReemplazanteHistorico.dniFromCuil(null));
        assertNull(ReemplazanteHistorico.dniFromCuil("123"));
    }
}
