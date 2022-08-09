package org.apache.lucene.harvest;

import org.apache.xerces.dom.AttrNSImpl;
import org.apache.xerces.dom.TextImpl;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: kalle
 * Date: 2007-mar-20
 * Time: 09:16:35
 */
public class WikipediaHarvester {

  public static void main(String[] args) throws Exception {
    WikipediaHarvester harvester = new WikipediaHarvester();
    Article a = harvester.getArticle("sv", "stockholm");
    for (URL url : a.getOtherLanguageRepresentationURLsByLanguageISO().values()) {
      harvester.getArticle(url);
    }
    System.currentTimeMillis();
  }

  private Map<String, Article> articles = new HashMap<String, Article>();


  public WikipediaHarvester() {
  }

  public Article getArticle(String iso, String articleTitle) throws SAXException, IOException {
    return getArticle(new URL("http://" + iso + ".wikipedia.org/wiki/" + articleTitle));
  }

  public Article getArticle(URL url) throws SAXException, IOException {
    if (url.toString().startsWith("http://en.")) {
      System.currentTimeMillis();
    }
    Article article = articles.get(url.toString());
    if (article == null) {
      try {

        article = new Article(url);
        articles.put(url.toString(), article);
      } catch (Exception e) {
        article = null;
      }
    }
    if (article == null) {
      System.out.println("Did not fetch " + url.toString());
    }
    return article;
  }

  private static final Pattern otherLanguagePattern = Pattern.compile("http://(.+)\\.wikipedia.org/wiki/(.+)");
  private static final Pattern titlePattern = Pattern.compile(".*var wgTitle = \"([^\"]+)\";.*");

  public class Article {

    private URL url;
    private String title;
    private String[] bodyParagraphs;

    private Map<String, URL> otherLanguageRepresentationURLsByLanguageISO = new HashMap<String, URL>();


    private Article(URL url) throws SAXException, IOException {

      this.url = url;

      // download and parse html
      final DOMParser parser = new DOMParser();
      parser.parse(new InputSource(new InputStreamReader(url.openStream(), "UTF8")));

      // debug
      debug(parser.getDocument(), "0");

      // extract title
      String variables = getNode(parser.getDocument(), new int[]{1, 1, 29, 0}).getTextContent().replaceAll("\\s+", " ").trim();
      Matcher matcher = titlePattern.matcher(variables);
      if (!matcher.matches()) {
        throw new RuntimeException("No title found.");
      }
      this.title = matcher.group(1).trim();

      // extract body paragraphs
      List<String> bodyParagraphs = new LinkedList<String>();
      extractParagraphsRecursive(getNode(parser.getDocument(), new int[]{1, 3, 1, 1, 1}), bodyParagraphs);
      this.bodyParagraphs = bodyParagraphs.toArray(new String[0]);

      // find article in other languages
      for (Node node : searchRecursive(getNode(parser.getDocument(), new int[]{1, 3, 1, 3, 15, 3}), ".*", TextImpl.class)) {
        AttrNSImpl href = (AttrNSImpl) node.getParentNode().getAttributes().getNamedItem("href");
        if (href != null) {
          matcher = otherLanguagePattern.matcher(href.getValue());
          if (matcher.matches()) {
            otherLanguageRepresentationURLsByLanguageISO.put(matcher.group(1), new URL(href.getValue()));
          }
        }
      }

      System.currentTimeMillis();
    }


    public URL getUrl() {
      return url;
    }

    public String getTitle() {
      return title;
    }

    public String[] getBodyParagraphs() {
      return bodyParagraphs;
    }

    public Map<String, URL> getOtherLanguageRepresentationURLsByLanguageISO() {
      return otherLanguageRepresentationURLsByLanguageISO;
    }
  }

// html dom helpers

  public static Node getNode(Node start, int[] path) {
    Node node = start;
    for (int child : path) {
      node = node.getChildNodes().item(child);
    }
    return node;
  }

  public static List<Node> searchRecursive(Node start, Pattern regexp, Class _class) {
    LinkedList<Node> matches = new LinkedList<Node>();
    searchRecursive(start, regexp, _class, matches, "0");
    return matches;
  }

  public static List<Node> searchRecursive(Node start, String regexp, Class _class) {
    LinkedList<Node> matches = new LinkedList<Node>();
    searchRecursive(start, Pattern.compile(regexp), _class, matches, "0");
    return matches;
  }

  public static List<Node> searchRecursive(Node start, Pattern regexp, Class _class, List<Node> matches, String path) {
    if (start.getTextContent() != null && (_class == null || start.getClass().equals(_class))) {
      if (regexp.matcher(start.getTextContent()).matches()) {
        //System.out.println(path + start.getTextContent());
        matches.add(start);
      }
    }
    for (int child = 0; child < start.getChildNodes().getLength(); child++) {
      searchRecursive(start.getChildNodes().item(child), regexp, _class, matches, path + "." + child);
    }
    return matches;
  }

  public static void extractParagraphsRecursive(Node node, List<String> paragraphs) {
    if ("P".equals(node.getLocalName())) {
      String paragraph = node.getTextContent();
      paragraph = paragraph.replaceAll("\\[[0-9]+\\]", ""); // remove references[1]
      paragraph = paragraph.trim();
      if (!"".equals(paragraph)) {
        paragraphs.add(paragraph);
      }
    } else {
      for (int i = 0; i < node.getChildNodes().getLength(); i++) {
        Node child = node.getChildNodes().item(i);
        extractParagraphsRecursive(child, paragraphs);
      }
    }
  }

  public static void extractTextRecursive(Node node, StringBuilder buf) {
    if (node instanceof org.apache.xerces.dom.TextImpl && !"".equals(node.getTextContent().trim())) {
      buf.append(node.getTextContent());
      buf.append("\n");
    }
    for (int i = 0; i < node.getChildNodes().getLength(); i++) {
      Node child = node.getChildNodes().item(i);
      extractTextRecursive(child, buf);
    }

  }

  public static void debug(Node node, String path) {
    if (node instanceof org.apache.xerces.dom.TextImpl && !"".equals(node.getTextContent().trim())) {
      System.out.println(path);
      System.out.println(node.getTextContent());
    }
    for (int i = 0; i < node.getChildNodes().getLength(); i++) {
      Node child = node.getChildNodes().item(i);
      debug(child, path + "." + i);
    }

  }

}
