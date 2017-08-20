package io.webcrawler;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Crawler {

    private BlockingQueue<String> sharedMainLinksQueue = null;
    private String URL = null;
    private String path = null;
    private String year = null;

    public Crawler(String year, String url, String path) {
        sharedMainLinksQueue = new LinkedBlockingDeque<String>();
        this.year = year;
        this.URL = url;
        this.path = path;
    }

    public static void main(String[] args) {
        String year = null;
        String path = null;
        if (args.length > 0) {
            year = args[0];
            path = args[1];
        } else {
            System.out.println("Please provide year and out path for file download");
        }
        Crawler producer = new Crawler(year, "http://mail-archives.apache.org/mod_mbox/maven-users", path);
        try {
            producer.downloadMailContent();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void downloadMailContent() throws InterruptedException {
        //regex for searching main urls for monthly mails
        String regex = "^.*" + year + "[0-9]{2,2}.*date$";
        File folder = new File(path);
        if (!folder.exists())
            folder.mkdirs();
        //Monthly mail url producer thread
        UrlProducer mailUrl = new UrlProducer(sharedMainLinksQueue, URL, regex);
        Thread mainUrlProducer = new Thread(mailUrl);
        mainUrlProducer.start();

        String url = null;
        //consume mail url from shared queue & download content and write to monthly files
        while ((url = sharedMainLinksQueue.poll(10, TimeUnit.SECONDS)) != null && !url.equalsIgnoreCase("exit")) {
            String fileName = null;
            if (url.matches(".+/" + year + "[0-9]{2,2}.+")) {
                String patternStr = "(" + year + "[0-9]{2,2})";
                Pattern pattern = Pattern.compile(patternStr);
                Matcher matcher = pattern.matcher(url);
                if (matcher.find()) {
                    fileName = matcher.group(0);
                }
            }
            MailContentConsumer contentConsumer = new MailContentConsumer(url, path + fileName + ".html");
            new Thread(contentConsumer).start();
        }
    }
}
