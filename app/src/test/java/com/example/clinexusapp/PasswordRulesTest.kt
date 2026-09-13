package com.example.clinexusapp

import com.example.clinexusapp.util.passwordsMeetRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordRulesTest {
    @Test fun acceptsEightCharactersAndPasswordsLongerThanTwelve() {
        assertTrue(passwordsMeetRules("Abcdefgh", "Abcdefgh"))
        val longPassword = "A" + "b".repeat(80)
        assertTrue(passwordsMeetRules(longPassword, longPassword))
    }

    @Test fun rejectsEachUnmetRule() {
        assertFalse(passwordsMeetRules("Abcdefg", "Abcdefg"))
        assertFalse(passwordsMeetRules("abcdefgh", "abcdefgh"))
        assertFalse(passwordsMeetRules("Abcd efgh", "Abcd efgh"))
        assertFalse(passwordsMeetRules("Abcd\tefgh", "Abcd\tefgh"))
        assertFalse(passwordsMeetRules("Abcdefgh", "AbcdefgH"))
        assertFalse(passwordsMeetRules("", ""))
    }
}