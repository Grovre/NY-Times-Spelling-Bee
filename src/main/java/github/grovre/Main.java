package github.grovre;

import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        // Initial Setup
        WebDriverManager.chromedriver().setup();
        Game hiveGame = new Game();
        System.out.println(hiveGame.getPossibleWords());

        // Game
        hiveGame.attemptAll();
    }
}