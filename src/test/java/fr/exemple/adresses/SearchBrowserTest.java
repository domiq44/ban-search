package fr.exemple.adresses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.By;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

@EnabledIfSystemProperty(named = "browser.tests", matches = "true")
class SearchBrowserTest {

    private static WebDriver driver;

    // Démarre Firefox avec la locale française pour les tests du navigateur.
    @BeforeAll
    static void startBrowser() {
        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("intl.accept_languages", "fr");
        FirefoxOptions options = new FirefoxOptions();
        options.setProfile(profile);
        driver = new FirefoxDriver(options);
    }

    // Ferme Firefox après l'exécution des tests du navigateur.
    @AfterAll
    static void stopBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    // Vérifie l'accessibilité du formulaire, les résultats, les réponses
    // concurrentes et le changement de langue.
    @Test
    void formIsAccessibleAndDisplaysSearchResults() {
        String baseUrl = System.getProperty("app.baseUrl", "http://localhost:8888/adresses/");
        driver.get(baseUrl);

        assertEquals("fr", driver.findElement(By.tagName("html")).getDomAttribute("lang"));
        assertTrue(driver.findElement(By.cssSelector("label.field-query")).isDisplayed());
        assertTrue(driver.findElement(By.cssSelector("label.field-postcode")).isDisplayed());
        WebElement status = driver.findElement(By.id("msg"));
        assertEquals("status", status.getDomAttribute("role"));
        assertEquals("polite", status.getDomAttribute("aria-live"));

        ((JavascriptExecutor) driver).executeScript("""
                window.fetch = async (url) => {
                    window.searchRequestUrl = url;
                    return {
                        ok: true,
                        json: async () => ({
                            features: [{
                                properties: {
                                    label: '1 rue de test', postcode: '80000', city: 'Amiens',
                                    type: 'housenumber', score: 0.95
                                },
                                geometry: { coordinates: [2.3, 49.9] }
                            }]
                        })
                    };
                };
                """);

        driver.findElement(By.name("q")).sendKeys("rue de test");
        driver.findElement(By.cssSelector("button[type=submit]")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.textToBe(By.id("msg"), "1 résultat"));
        assertTrue(driver.findElement(By.id("result")).isDisplayed());
        assertEquals("1 rue de test", driver.findElement(By.cssSelector("tbody tr td")).getText());
        assertTrue(((String) ((JavascriptExecutor) driver).executeScript("return window.searchRequestUrl"))
                .contains("q=rue+de+test"));

        ((JavascriptExecutor) driver).executeScript("""
                window.pendingSearches = [];
                window.fetch = (url) => new Promise((resolve) => {
                    window.pendingSearches.push({ resolve });
                });
                """);
        WebElement query = driver.findElement(By.name("q"));
        query.clear();
        query.sendKeys("first address");
        driver.findElement(By.cssSelector("button[type=submit]")).click();
        wait.until(webDriver -> Boolean.TRUE.equals(((JavascriptExecutor) webDriver)
                .executeScript("return window.pendingSearches.length === 1")));
        query.clear();
        query.sendKeys("second address");
        driver.findElement(By.cssSelector("button[type=submit]")).click();
        wait.until(webDriver -> Boolean.TRUE.equals(((JavascriptExecutor) webDriver)
                .executeScript("return window.pendingSearches.length === 2")));
        ((JavascriptExecutor) driver).executeScript("""
                const response = (label) => ({
                    ok: true,
                    json: async () => ({ features: [{
                    properties: {
                        label, postcode: '80000', city: 'Amiens', type: 'housenumber', score: 0.95
                    },
                    geometry: { coordinates: [2.3, 49.9] }
                    }] })
                });
                window.pendingSearches[1].resolve(response('second response'));
                window.pendingSearches[0].resolve(response('first response'));
                """);
        wait.until(ExpectedConditions.textToBe(By.id("msg"), "1 résultat"));
        assertEquals("second response", driver.findElement(By.cssSelector("tbody tr td")).getText());

        driver.findElement(By.cssSelector(".language-toggle")).click();
        wait.until(webDriver -> "en".equals(webDriver.findElement(By.tagName("html")).getDomAttribute("lang")));
        assertTrue(driver.findElement(By.cssSelector("label.field-query")).getText().contains("Address to search"));
    }
}