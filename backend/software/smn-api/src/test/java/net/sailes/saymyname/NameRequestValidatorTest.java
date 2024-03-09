package net.sailes.saymyname;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class NameRequestValidatorTest  {

    @Test
    public void invalidIfOver70characters() {
        assertFalse(NameRequestValidator.isValid("sdsgsfhdfhdghdsfgsdfasfgsdgsdfhxfdgrergdfgdfgdfgdf sdfsdfsdfsdfasdaawfaf"));
    }
}