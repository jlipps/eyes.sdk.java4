package com.applitools.eyes.images;

import com.applitools.eyes.LogHandler;
import com.applitools.eyes.Region;
import com.applitools.eyes.StdoutLogHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URI;

@RunWith(JUnit4.class)
public class TestImagesApi {
    private static Eyes eyes;

    @BeforeClass
    public static void OneTimeSetUp() {

        // Initialize the eyes SDK and set your private API key.
        eyes = new Eyes();
        eyes.setServerUrl(URI.create("https://localhost.applitools.com"));
        eyes.setApiKey(System.getenv("APPLITOOLS_API_KEY"));

        //logHandler = new FileLogger("c:\\temp\\logs\\TestElement.log", true, true);
        LogHandler logHandler = new StdoutLogHandler(true);
        eyes.setLogHandler(logHandler);

        eyes.open("TestImagesApi", "Test Images API");
    }

    @AfterClass
    public static void Finished() {
        try {
            eyes.close();
        } finally {
            eyes.abortIfNotClosed();
        }
    }

    @Test
    public void TestCheckImage() {
        eyes.checkImage("resources/minions-800x500.jpg");
    }

    @Test
    public void TestCheckImage_Fluent() {
        eyes.check("TestCheckImage_Fluent", Target.image("resources/minions-800x500.jpg"));
    }

    @Test
    public void TestCheckImage_WithIgnoreRegion_Fluent() {
        eyes.check("TestCheckImage_WithIgnoreRegion_Fluent", Target.image("resources/minions-800x500.jpg")
                .ignore(new Region(10,20,30,40)));
    }

}
