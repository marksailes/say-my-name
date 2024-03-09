package net.sailes.saymyname;

/**
 * UK Gov standard is 35 chars for a given name and 35 chars for a family name.
 * <p>
 * <a href="https://webarchive.nationalarchives.gov.uk/ukgwa/20100407173424/http://www.cabinetoffice.gov.uk/govtalk/schemasstandards/e-gif/datastandards.aspx">UK Gov data standards</a>
 */
public class NameRequestValidator {

    public static final int MAX_NAME_LENGTH = 70;

    public static boolean isValid(String name) {
        return name.length() <= MAX_NAME_LENGTH;
    }
}
