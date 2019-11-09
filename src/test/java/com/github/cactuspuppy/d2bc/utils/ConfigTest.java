package com.github.cactuspuppy.d2bc.utils;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigTest {
    private Config testConfig;
    private static File tempDirectory;

    @BeforeClass
    public static void classSetup() {
        tempDirectory = Files.createTempDir();
    }

    @AfterClass
    public static void classTeardown() {
        tempDirectory.deleteOnExit();
    }

    @Before
    public void setup() {
        testConfig = new Config();
    }

    @After
    public void teardown() {
        testConfig = null;
    }

    @Test
    public void testEmpty() throws InvalidConfigurationException {
        String input =
        "# Empty config";
        testConfig.loadFromString(input);
        assertTrue(testConfig.isEmpty());
    }

    @Test
    public void stringNullCheck() {
        assertThrows(IllegalArgumentException.class, () -> testConfig.load((String) null));
    }

    @Test
    public void fileNullCheck() {
        assertThrows(IllegalArgumentException.class, () -> testConfig.load((File) null));
    }

    @Test
    public void fileNotExistCheck() {
        assertThrows(FileNotFoundException.class, () -> testConfig.load("not-exist.yml"));
    }

    @Test
    public void loadFromStringNullCheck() {
        assertThrows(IllegalArgumentException.class, () -> testConfig.loadFromString(null));
        assertThrows(IllegalArgumentException.class, () -> testConfig.loadFromString(""));
    }

    @Test
    public void saveFileNullCheck() {
        assertThrows(IllegalArgumentException.class, () -> testConfig.save(null));
    }

    @Test
    public void simpleTest() throws InvalidConfigurationException {
        String input =
        "key: value";

        testConfig.loadFromString(input);
        assertEquals("value", testConfig.get("key"));
    }

    @Test
    public void indentTest() throws InvalidConfigurationException {
        String input =
        "object:\n" +
        "  key: value02";

        testConfig.loadFromString(input);
        assertEquals("value02", testConfig.get("object.key"));
    }

    @Test
    public void sizeTest01() throws InvalidConfigurationException {
        String input =
        "object:\n" +
        "  key: value02\n";

        testConfig.loadFromString(input);
        assertEquals(1, testConfig.size());
    }

    @Test
    public void sizeTest02() throws InvalidConfigurationException {
        String input =
        "\n\n# Starter comment\n" +
        "# =========== #\n" +
        "object:   \n" +
        "  key: value02\n" +
        "beep boop: value\n";
        testConfig.loadFromString(input);
        assertEquals(2, testConfig.size());
    }

    @Test
    public void commentTest01() throws InvalidConfigurationException {
        String input =
        "#============#\n" +
        "# Beep Boop\n" +
        "#============#\n" +
        "key: value";

        testConfig.loadFromString(input);
        assertEquals("value", testConfig.get("key"));
    }

    @Test
    public void commentTest02() throws InvalidConfigurationException {
        String input =
        "#============#\n" +
        "# Beep Boop\n" +
        "#============#\n" +
        "object:\n" +
        "  key: value02";

        testConfig.loadFromString(input);
        assertEquals("value02", testConfig.get("object.key"));
    }

    @Test
    public void commentTestInline() throws InvalidConfigurationException {
        String input =
        "# Starting comment\n" +
        "key: value-00  # inline comment\n" +
        "  subkey: value01  #second inline comment\n" +
        "  subkey02: value02\n" +
        "  \n" +
        "  # Explanation comment preceded by blank line\n" +
        "  explainedkey: expl_value\n" +
        "  \n" +
        "# A_comment\n" +
        "topkey: notacreepjustacookie24\n";

        testConfig.loadFromString(input);
        assertEquals("value-00", testConfig.get("key"));
        assertEquals("value01", testConfig.get("key.subkey"));
        assertEquals("expl_value", testConfig.get("key.explainedkey"));
        assertEquals("notacreepjustacookie24", testConfig.get("topkey"));
    }

    @Test
    public void invalidConfigTest() {
        String input =
        "# Valid comment\n" +
        "  Invalid # line\n";
        assertThrows(InvalidConfigurationException.class, () -> testConfig.loadFromString(input));

        String input2 =
        "# Valid starting comment\n" +
        "toplevel: # inline\n" +
        "  validkey: value  # inline\n" +
        "  # valid standalone\n" +
        "  invalid # commented line\n";
        assertThrows(InvalidConfigurationException.class, () -> testConfig.loadFromString(input2));
    }

    @Test
    public void testTopLevelAdd() throws InvalidConfigurationException {
        String input =
        "# Starter comment\n" +
        "key: value\n";
        testConfig.loadFromString(input);
        testConfig.put("key2", "56.7%");
        assertEquals("56.7%", testConfig.get("key2"));
    }

    @Test
    public void testSubkeyAdd() throws InvalidConfigurationException {
        String input =
        "# Starter comment\n" +
        "key: value\n";
        testConfig.loadFromString(input);
        testConfig.put("key.subkey", "eiko_kek");
        assertEquals("eiko_kek", testConfig.get("key.subkey"));
        assertEquals("value", testConfig.get("key"));
    }

    @Test
    public void testKeyAndSubkeyAdd() throws InvalidConfigurationException {
        String input =
        "# Starter comment\n" +
        "key: value\n";
        testConfig.loadFromString(input);
        testConfig.put("key2", "Seagull roller");
        testConfig.put("key2.subkey", "Segol");
        assertEquals("Seagull roller", testConfig.get("key2"));
        assertEquals("Segol", testConfig.get("key2.subkey"));
    }

    @Test
    public void testSubkeyWithoutTopKey() throws InvalidConfigurationException {
        String input =
        "\n\n# Starter comment\n" +
        "key: value\n";
        testConfig.loadFromString(input);
        testConfig.put("key2.subkey", "2?");
        assertNull(testConfig.get("key2"));
        assertEquals("2?", testConfig.get("key2.subkey"));
    }

    @Test
    public void testKeyRemoval() throws InvalidConfigurationException {
        String input =
        "# Starter comment\n" +
        "key2: value2\n";
        testConfig.loadFromString(input);
        assertEquals("value2", testConfig.get("key2"));
        testConfig.remove("key2");
        assertNull(testConfig.get("key2"));
        assertTrue(testConfig.isEmpty());
    }

    @Test
    public void testSubkeyRemoval() throws InvalidConfigurationException {
        String input =
        "# Starter comment\n" +
        "key2: value2\n" +
        "  subkey: value\n";
        testConfig.loadFromString(input);
        assertEquals("value", testConfig.get("key2.subkey"));
        testConfig.remove("key2.subkey");
        assertNull(testConfig.get("key2.subkey"));
        assertEquals("value2", testConfig.get("key2"));
    }

    @Test
    public void testTopKeyRemove() throws InvalidConfigurationException {
        String input =
        "# Starter comment\n" +
        "key2: value2\n" +
        "  subkey: value\n";
        testConfig.loadFromString(input);
        assertEquals("value2", testConfig.get("key2"));
        assertEquals("value", testConfig.get("key2.subkey"));
        testConfig.remove("key2");
        assertNull(testConfig.get("key2"));
        assertEquals("value", testConfig.get("key2.subkey"));
    }

    @Test
    public void testTopKeyKill() throws InvalidConfigurationException {
        String input =
        "# Starter comment\n" +
        "key2: value2\n" +
        "  subkey: value\n";
        testConfig.loadFromString(input);
        assertEquals("value2", testConfig.get("key2"));
        assertEquals("value", testConfig.get("key2.subkey"));
        testConfig.kill("key2");
        assertNull(testConfig.get("key2"));
        assertNull(testConfig.get("key2.subkey"));
        assertTrue(testConfig.isEmpty());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testClear() throws InvalidConfigurationException {
        String input =
        "topkey: value\n" +
        "  subkey: value\n";
        testConfig.loadFromString(input);
        assertFalse(testConfig.isEmpty());
        testConfig.clear();
        assertTrue(testConfig.isEmpty());
    }

    @Test
    public void testLoad01() throws InvalidConfigurationException, IOException {
        File config01 = new File(getClass().getResource("/config01.yml").getFile());
        testConfig.load(config01);
        assertFalse(testConfig.isEmpty());
        assertEquals("topvalue", testConfig.get("topkey"));
        assertEquals("value", testConfig.get("topkey.subkey"));
        assertEquals("poorly formatted key", testConfig.get("a"));
    }

    @Test
    public void testLoad02() throws InvalidConfigurationException, IOException {
        File config02 = new File(getClass().getResource("/config02.yml").getFile());
        testConfig.load(config02);
        assertFalse(testConfig.isEmpty());
        assertEquals("value1", testConfig.get("key1"));
        assertEquals("value1-2", testConfig.get("key1.subkey1-2"));
        assertEquals("value2", testConfig.get("key2"));
        assertEquals("boogle", testConfig.get("key2.subkey2-2"));
        assertEquals("supersubvalue", testConfig.get("key1.subkey1-2.supersubkey"));
    }

    @Test
    public void testSaveToString() throws InvalidConfigurationException {
        String config =
        "\n\n#==============#\n" +
        "# Header comment\n" +
        "#============#\n" +
        "key: value\n" +
        "  test: beep\n";
        testConfig.loadFromString(config);
        assertFalse(testConfig.isEmpty());
        testConfig.addBlankLines(1);
        String output = testConfig.saveToString();
        assertEquals(config, output);
    }

    @Test
    public void testSaveNoMod01() throws InvalidConfigurationException, IOException {
        File config01 = new File(getClass().getResource("/config01.yml").getFile());
        testConfig.load(config01);
        File saved01 = File.createTempFile("config01-temp", ".yml", tempDirectory);
        testConfig.save(saved01);
        boolean filesEqual = FileUtils.contentEqualsIgnoreEOL(config01, saved01, null);
        if (!filesEqual) {
            System.out.println("Original:\n" + FileUtils.readFileToString(config01, (String) null));
            System.out.println("Output:\n" + FileUtils.readFileToString(saved01, (String) null));
        }
        assertTrue(filesEqual);
        if (!saved01.delete()) {
            System.out.println("Unable to delete file, potential memory leak? Deleting on exit.");
            fail("Failed to remove temporary config file");
            saved01.deleteOnExit();
        }
    }

    @Test
    public void testSaveNoMod02() throws InvalidConfigurationException, IOException {
        File config02 = new File(getClass().getResource("/config02.yml").getFile());
        testConfig.load(config02);
        File saved02 = File.createTempFile("config02-temp", ".yml", tempDirectory);
        testConfig.save(saved02);
        assertTrue(FileUtils.contentEqualsIgnoreEOL(config02, saved02, null));
        if (!saved02.delete()) {
            System.out.println("Unable to delete file, potential memory leak? Deleting on exit.");
            fail("Failed to remove temporary config file");
            saved02.deleteOnExit();
        }
    }

    @Test
    public void testSaveAfterMod01() throws InvalidConfigurationException, IOException {
        File config02 = new File(getClass().getResource("/config02.yml").getFile());
        File config02Mod = new File(getClass().getResource("/config02-top.yml").getFile());
        testConfig.load(config02);
        assertNull(testConfig.get("key3"));
        assertNull(testConfig.get("key3.subkey3-1"));
        testConfig.addBlankLines(1);
        testConfig.put("key3.subkey3-1", "asdf");
        testConfig.put("key3", "value3");
        File saved = File.createTempFile("config02-mod-temp", ".yml", tempDirectory);
        testConfig.save(saved);
        boolean filesEqual = FileUtils.contentEqualsIgnoreEOL(config02Mod, saved, null);
        if (!filesEqual) {
            System.out.println("Original:\n" + FileUtils.readFileToString(config02Mod, (String) null));
            System.out.println("Output:\n" + FileUtils.readFileToString(saved, (String) null));
        }
        assertTrue(filesEqual);
        if (!saved.delete()) {
            System.out.println("Unable to delete file, potential memory leak? Deleting on exit.");
            fail("Failed to remove temporary config file");
            saved.deleteOnExit();
        }
    }

    @Test
    public void testSaveAfterMod02() throws InvalidConfigurationException, IOException {
        File config02 = new File(getClass().getResource("/config02.yml").getFile());
        File target = new File(getClass().getResource("/config02-fullmod.yml").getFile());
        testConfig.load(config02);
        assertFalse(testConfig.isEmpty());
        testConfig.kill("key2");
        testConfig.put("awkward", "modification");
        testConfig.addBlankLines(2);
        testConfig.addComment(" The previous 2 blank lines tbh");
        testConfig.put("key2.subkey2-1", "down here");
        testConfig.put("key2", "moved");
        testConfig.addBlankLines(1);
        File saved = File.createTempFile("config02-mod-temp", ".yml", tempDirectory);
        testConfig.save(saved);
        boolean filesEqual = FileUtils.contentEqualsIgnoreEOL(target, saved, null);
        if (!filesEqual) {
            System.out.println("Original:\n" + FileUtils.readFileToString(target, (String) null));
            System.out.println("------");
            System.out.println("Output:\n" + FileUtils.readFileToString(saved, (String) null));
            System.out.println("------");
        }
        assertTrue(filesEqual);
        if (!saved.delete()) {
            System.out.println("Unable to delete file, potential memory leak? Deleting on exit.");
            fail("Failed to remove temporary config file");
            saved.deleteOnExit();
        }
    }
}