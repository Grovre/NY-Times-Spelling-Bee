package github.grovre;

import lombok.Data;
import lombok.SneakyThrows;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Game {

    private Map<Character, WebElement> possibleChars = new HashMap<>(7);
    private char forcedChar = 'a';
    private final List<String> possibleWords = new ArrayList<>();
    public final SafariDriver driver;
    private final List<String> wordsCorrect = new ArrayList<>();

    public Game() throws InterruptedException {
        String url = "https://www.nytimes.com/puzzles/spelling-bee";
        SafariOptions safariOptions = new SafariOptions();
        safariOptions.setPageLoadTimeout(Duration.ofSeconds(10));
        this.driver = new SafariDriver(safariOptions);
        this.driver.get(url);
        String xpath = "//*[@id=\"js-hook-pz-moment__welcome\"]/div/div/div/div[2]/button[1]";
        Thread.sleep(1000);
        WebElement playButton = this.driver.findElement(By.xpath(xpath));
        playButton.click();
        this.driver.manage().window().maximize();
        Thread.sleep(1250);
        this.centerScreen();

        this.parseLetters();
        this.makePossibleWords();
    }

    @SneakyThrows
    private void makePossibleWords() {
        try(var words = Files.lines(Path.of("src/main/resources/words.txt"))) {
            // String comparator based on length
            var foundPossibleWords = words.parallel() // 230k words to go through, why not...
                    .map(String::toLowerCase)
                    .filter(w -> w.length() > 3)
                    .filter(w -> {
                        for(char c : w.toCharArray())
                            if(!this.possibleChars.containsKey(c)) // Sorted when retrieved in parseLetters method
                                return false;
                        return true;})
                    .filter(w -> w.contains(Character.toString(this.forcedChar)))
                    .sorted((o1, o2) -> {
                        if(o1.length() < o2.length())
                            return 1;
                        else if(o1.length() > o2.length())
                            return -1;
                        return 0;})
                    .distinct()
                    .toList();
            this.possibleWords.clear();
            this.possibleWords.addAll(foundPossibleWords);
        }
    }

    private void parseLetters() {
        String xpath = "//*[@id=\"pz-game-root\"]/div/div[2]/div/div[3]/div";
        List<WebElement> elements = this.driver
                .findElement(By.xpath(xpath))
                .findElements(By.tagName("svg"));
        for(int i = 0; i < 7; i++)
            this.possibleChars.put(elements.get(i)
                    .findElement(By.className("cell-letter"))
                    .getText()
                    .toCharArray()[0], elements.get(i));
        this.forcedChar = elements.get(0)
                .findElement(By.className("cell-letter"))
                .getText()
                .toCharArray()[0];
    }

    public boolean attempt(String word) throws InterruptedException {
        word = word.toLowerCase();
        char[] toPress = word.toCharArray();
        for(char c : toPress)
            this.click(this.possibleChars.get(c));
        int correct = this.getAmountOfCorrectWordsFromHtml();
        this.driver.findElement(By.xpath("//*[@id=\"pz-game-root\"]/div/div[2]/div/div[4]/div[1]"))
                .click();
        Thread.sleep(50);
        this.centerScreen();
        if(this.getAmountOfCorrectWordsFromHtml() > correct) {
            this.wordsCorrect.add(word);
            return true;
        }
        return false;
    }

    private void centerScreen() {
        this.driver.findElement(By.xpath("//*[@id=\"js-hook-pz-moment__game\"]/div[1]"))
                .click();
    }

    private int getAmountOfCorrectWordsFromHtml() {
        return this.driver.findElement(By.xpath("//*[@id=\"pz-game-root\"]/div/div[1]/div[2]/div[2]/div/div[1]/ul"))
                .findElements(By.tagName("li"))
                .size();
    }

    public void attemptAll() throws InterruptedException {
        for(String word : this.possibleWords)
            this.attempt(word);
    }

    @SneakyThrows
    private void click(WebElement el) {
        el.click();
        Thread.sleep(250);
    }
}
