package io.webcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class UrlProducer implements Runnable {

    private BlockingQueue<String> sharedLinksQueue = null;
    private Set<String> linkSet = new HashSet<String>();
    private String url = null;
    private String regex = null;

    public UrlProducer(BlockingQueue<String> sharedLinksQueue, String url, String regex){
        this.sharedLinksQueue=sharedLinksQueue;
        this.url = url;
        this.regex = regex;
    }

    @Override
    public void run() {
        getMailLinks(url, regex,sharedLinksQueue);
        //adding exit String
        String msg = new String("exit");
        try {
            sharedLinksQueue.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void getMailLinks(String urlTemp, String regex, BlockingQueue<String> sharedLinksQueue){
        if (regex != null && !regex.trim().isEmpty()) {
            try {
                Elements linksOnPage = null;
                Document document = Jsoup.connect(urlTemp).get();
                //document.ge
                linksOnPage = document.getElementsByAttributeValueMatching("href", regex);
                if(linksOnPage.size()==0){
                    System.err.println("Mail list not found");
                }
                for (Element page : linksOnPage) {
                    String mainUrl = page.attr("abs:href");
                    try {
                            sharedLinksQueue.put(mainUrl);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                System.err.println("For '" + urlTemp + "': " + e.getMessage());
            }
        }
    }

}
