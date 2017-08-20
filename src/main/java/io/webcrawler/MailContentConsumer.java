package io.webcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class MailContentConsumer implements Runnable {

    private String url;
    private String fileName;
    private int count = 0;
    private Set<String> duplicates = new HashSet<>();

    public MailContentConsumer(String url, String fileName) {
        this.url = url;
        this.fileName = fileName;
    }

    @Override
    public void run() {
        Set<String> mailUrlSet = new LinkedHashSet<String>();
        //get all mail links for a particular month
        mailUrlSet = getMailLinks(url, mailUrlSet);
        String content = downloadMailContent(mailUrlSet);
        //write monthly files
        writeToFile(content);
        System.out.println("Number of mails in the file " + fileName + " is " + count);
    }

    private String downloadMailContent(Set<String> mailUrlSet) {
        Iterator<String> iterator = mailUrlSet.iterator();
        StringBuilder strBud = new StringBuilder();
        while (iterator.hasNext()) {
            try {
                //download mail content
                Document document = Jsoup.connect(iterator.next()).get();
                Elements elemts = document.getElementsByClass("from");
                if (elemts.hasText()) {
                    strBud.append(elemts.eachText());
                }
                elemts = document.getElementsByClass("subject");
                if (elemts.hasText()) {
                    strBud.append(elemts.eachText());
                }
                elemts = document.getElementsByClass("date");
                if (elemts.hasText()) {
                    strBud.append(elemts.eachText());
                }
                elemts = document.getElementsByClass("contents");
                if (elemts.hasText()) {
                    strBud.append(elemts);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return strBud.toString();
    }

    private void writeToFile(String mailContent) {
        File file = new File(fileName);
        try {
            if (!file.exists()) {
                file.createNewFile();
            } else {
                file.delete();
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file, true);
            writer.write(mailContent);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<String> getMailLinks(String URL, Set<String> linkSet) {
        try {
            Document document = Jsoup.connect(URL).get();
            duplicates.add(URL);
            //mail content links
            Elements linksOnPage = document.getElementsByAttributeValueMatching("href", "^%3c");
            for (Element page : linksOnPage) {
                count++;
                linkSet.add(page.attr("abs:href"));
            }
            //for getting page links for a particular month
            linksOnPage = document.getElementsByAttributeValueMatching("href", ".+date[\\?][1-9]+");
            for (Element el : linksOnPage) {
                String urlTemp = el.attr("abs:href");
                if (urlTemp.matches(".+date[\\?][1-9]+") && !duplicates.contains(urlTemp)) {
                    duplicates.add(urlTemp);
                    getMailLinks(urlTemp, linkSet);
                }
            }

        } catch (IOException e) {
            System.err.println("For '" + URL + "': " + e.getMessage());
        }
        return linkSet;
    }
}
