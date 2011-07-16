package client;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import client.Dictionary;
import client.Stats;
import client.TransitionTable;

/**
 * RUBBoS user session emulator. This class plays a random user session
 * emulating a Web browser.
 * 
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a
 *         href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */
public class WordpressUserSession extends UserSession {
	private final int USER_LEVEL_ADMINISTRATOR = 1;
	private final int USER_LEVEL_EDITOR = 2;

	private final int STATUS_LOGGED_OUT = 1;
	private final int STATUS_LOGGED_IN = 2;
	
	public final int STATE_HOMEPAGE = 0;
	public final int STATE_PAGE = 1;
	public final int STATE_SEARCH = 2;
	public final int STATE_BLOGPOST = 3;
	public final int STATE_ARCHIVE_MONTHLY = 4;
	public final int STATE_CATEGORY = 5;
	public final int STATE_AUTHOR = 6;
	public final int STATE_BLOGPOST_PAGED = 7;
	public final int STATE_SEARCH_PAGED = 8;
	public final int STATE_ARCHIVE_MONTHLY_PAGED = 9;
	public final int STATE_CATEGORY_PAGED = 10;
	public final int STATE_AUTHOR_PAGED = 11;	
	public final int STATE_WORDPRESS_WEBSITE = 12;
	public final int STATE_ADD_ANONYMOUS_COMMENT = 13;
	
	public final int STATE_LOGIN_SEND_CREDENTIALS = 14;
	public final int STATE_LOG_OUT = 15;
	public final int STATE_ADD_COMMENT = 16;
	public final int STATE_ADD_POST = 17;
	public final int STATE_ADD_PAGE = 18;
	public final int STATE_ADD_CATEGORY = 19;
	
	public final int STATE_ADD_USER = 20;
	public final int STATE_DELETE_DATA = 21;
	
	public static String STATES[] = {
		"STATE_HOMEPAGE",
		"STATE_PAGE",
		"STATE_SEARCH",
		"STATE_BLOGPOST",
		"STATE_ARCHIVE_MONTHLY",
		"STATE_CATEGORY",
		"STATE_AUTHOR",
		"STATE_BLOGPOST_PAGED",
		"STATE_SEARCH_PAGED",
		"STATE_ARCHIVE_MONTHLY_PAGED",
		"STATE_CATEGORY_PAGED",
		"STATE_AUTHOR_PAGED",
		"STATE_WORDPRESS_WEBSITE",
		"STATE_ADD_ANONYMOUS_COMMENT",
		"STATE_LOGIN_SEND_CREDENTIALS",
		"STATE_LOG_OUT",
		"STATE_ADD_COMMENT",
		"STATE_ADD_POST",
		"STATE_ADD_PAGE",
		"STATE_ADD_CATEGORY",
		"STATE_ADD_USER",
		"STATE_DELETE_DATA"
		};
	
	public static String REGEX[] = {
		"",											//0
		"\\?page_id=(\\d+)#??",						//1 STATE_PAGE
		"",											//2 STATE_SEARCH
		"\\?p=(\\d+)#??",							//3 STATE_BLOGPOST
		"\\?m=(\\d+)#??",							//4 STATE_ARCHIVE_MONTHLY
		"\\?category_name=(\\w+)#??",				//5 STATE_CATEGORY
		"\\?author=(\\d+)",							//6 STATE_AUTHOR
		"\\?paged=(\\d+)",							//7 STATE_BLOGPOST_PAGED
		"\\?s=(\\w+)&.*paged=(\\d+)",				//8 STATE_SEARCH_PAGED
		"\\?m=(\\d+)&.*paged=(\\d+)",				//9 STATE_ARCHIVE_MONTHLY_PAGED
		"\\?category_name=(\\w+)&.*paged=(\\d+)",	//10 STATE_CATEGORY_PAGED
		"\\?author=(\\d+)&.*paged=(\\d+)",			//11 STATE_AUTHOR_PAGED
		"onclick=\\'return addComment.moveForm\\(\"comment-(\\d+)\", \"(\\d+)\", \"respond\", \"(\\d+)\"\\)\\'",	//12 STATE_ADD_ANONYMOUS_COMMENT-1
		"<input type=\\'hidden\\' name=\\'comment_post_ID\\' value=\\'(\\d+)\\' id='comment_post_ID' />",			//13 STATE_ADD_ANONYMOUS_COMMENT
		"_wpnonce=(\\w+)", 																							//14
		"<input type=\\'hidden\\' id=\\'post_ID\\' name=\\'post_ID\\' value=\\'(\\d+)\\' />",						//15
		"<input type=\"hidden\" id=\"_wpnonce\" name=\"_wpnonce\" value=\"(\\w+)\" />",								//16
		"<input type=\"hidden\" id=\"user-id\" name=\"user_ID\" value=\"(\\d+)\" />",								//17
		"<input type=\"hidden\" id=\"autosavenonce\" name=\"autosavenonce\" value=\"(\\w+)\" />",					//18
		"<input type=\"hidden\" id=\"meta-box-order-nonce\" name=\"meta-box-order-nonce\" value=\"(\\w+)\" />",		//19
		"<input type=\"hidden\" id=\"closedpostboxesnonce\" name=\"closedpostboxesnonce\" value=\"(\\w+)\" />",		//20
		"<input type=\"hidden\" id=\"_wpnonce_create-user\" name=\"_wpnonce_create-user\" value=\"(\\w+)\" />", 	//21
		"<input value=\"(\\d+)\" type=\"checkbox\" name=\"post_category\\[\\]\" id=\"in-category-(\\d+)\" />",		//22
		"<input type=\"hidden\" id=\"_wpnonce\" name=\"_wpnonce\" value=\"(\\w+)\" />"								//23
		};
	
	public static String DELETE_QUERY[] = {
		"DELETE FROM wp_users WHERE ID <> '1'",
		"DELETE FROM wp_usermeta WHERE user_id <> '1'",
		"DELETE FROM wp_comments",
		"DELETE FROM wp_commentmeta",
		"DELETE FROM wp_posts",
		"DELETE FROM wp_postmeta",
		"DELETE FROM wp_term_taxonomy",
		"DELETE FROM wp_term_relationships"
	};

	private WordpressProperties properties = null; // access to wordpress.properties
	private TransitionTable transition = null; // transition table user for this
	private TransitionTable transitionLoggedOut = null; // transition table user for this
	private TransitionTable transitionLoggedIn = null; // transition table user for this
	private int state;
	private Stats stats;
	private Dictionary dictionary = null;
	private int loggedStatus = STATUS_LOGGED_OUT;
	private int userLevel = USER_LEVEL_EDITOR;
	int prev = -1;

	public WordpressUserSession(String threadId, URLGenerator URLGen, Dictionary dict, WordpressProperties properties, 
			TransitionTable transitionLoggedOut, TransitionTable transitionLoggedIn, Stats statistics, String username) {
		super(threadId);
		urlGen = URLGen;
		this.properties = properties;
		stats = statistics;
		this.transitionLoggedOut = new TransitionTable(transitionLoggedOut);
		this.transitionLoggedIn = new TransitionTable(transitionLoggedIn);
		this.transition = this.transitionLoggedOut;
		dictionary = dict;
		this.username = username;
		this.password = username;
	}

	// get IDs
	private int extractIDFromHTML(String regex) {
		if (lastHTMLReply == null) {
			return 0;
		}
		String IDvalue = null;
		Pattern pattern = Pattern.compile(regex);
		Matcher m = pattern.matcher(lastHTMLReply);
		try {
			while(m.find()) {
				IDvalue = m.group(1);
				if (rand.nextInt() % 5 == 0)
					break;
			}
		}
		catch (Exception e) {
			return 0;
		}
		if (IDvalue != null)
			return Integer.parseInt(IDvalue);
		else {
			return 0;
		}
	}
	
	private Vector<Object> extractMultipleIDFromHTML(String regex) {
		if (lastHTMLReply == null) {
			return null;
		}
		Vector<Object> IDValues = new Vector<Object>();
		String value = new String();
		MatchResult matcherFound = null;
		Pattern pattern = Pattern.compile(regex);
		Matcher m = pattern.matcher(lastHTMLReply);
		try {
			while(m.find()) {
				value = m.group(1);
				matcherFound = m.toMatchResult();
				if (rand.nextInt() % 5 == 0) {
					break;
				}
			}
			if(value!=null)
				IDValues.add(0, value);
			else
				IDValues.add(0, -1);
			value = matcherFound.group(2);
			if(value!=null)
				IDValues.add(1, Integer.parseInt(value));
			else
				IDValues.add(1, -1);
		}
		catch (Exception e) {
			return IDValues;
		}
		return IDValues;
	}
	
	private String extractWordFromHTML(String regex) {
		if (lastHTMLReply == null) {
			return null;
		}
		int order = 1;
		String WordValue = null;
		Pattern pattern = Pattern.compile(regex);
		Matcher m = pattern.matcher(lastHTMLReply);
		try {
			while(m.find()) {
				WordValue = m.group(order);
				if (rand.nextInt() % 7 == 0)
					break;
			}
		}
		catch (Exception e) {
			return "";
		}
		if (WordValue != null)
			return WordValue;
		else
			return "";	
	}
	
	public Vector<String> initWebsiteData(int usersNb){
		Vector<String> newUsers = new Vector<String>();
		userLevel = USER_LEVEL_ADMINISTRATOR;
		username = "admin";
		password = username;
		loggedStatus = STATUS_LOGGED_OUT;

		createNewUsers(0, usersNb);
		try {
			doState(STATE_LOGIN_SEND_CREDENTIALS);
			for(int i=0;i<3;i++) {
				doState(STATE_ADD_POST);
				doState(STATE_ADD_PAGE);
				doState(STATE_HOMEPAGE);
				doState(STATE_ADD_CATEGORY);
				doState(STATE_HOMEPAGE);
				doState(STATE_BLOGPOST);
				doState(STATE_ADD_COMMENT);
			}
			doState(STATE_HOMEPAGE);
			doState(STATE_LOG_OUT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return newUsers;
	}
	
	public Vector<String> createNewUsers(int usersNbMin, int usersNbMax){
		Vector<String> newUsers = new Vector<String>();
		userLevel = USER_LEVEL_ADMINISTRATOR;
		username = "admin";
		password = username;
		loggedStatus = STATUS_LOGGED_OUT;

		try {
			doState(STATE_LOGIN_SEND_CREDENTIALS);
			for(int i=usersNbMin;i<usersNbMax;i++)
				newUsers.add(addUser(i));
			doState(STATE_LOG_OUT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return newUsers;
	}
	
	public String addUser(int index) throws IOException {
		Hashtable<String, String> post = new Hashtable<String, String>();
		WordpressURLGenerator urlGen = (WordpressURLGenerator)this.urlGen;
		String keyword, username;
		if(userLevel == USER_LEVEL_ADMINISTRATOR) {
			lastURL = urlGen.newUser();
			System.out.println(" Link: " + lastURL);
			lastHTMLReply = callHTTPServer(lastURL);
			
			post.clear();
			username = "user"+index;
			post.put("user_login", username);
			post.put("email", username+"@"+username+".com");
			post.put("pass1", username);
			post.put("pass2", username);
			post.put("role", "editor");
			post.put("action", "createuser");
			keyword = extractWordFromHTML(REGEX[21]);
			post.put("_wpnonce_create-user", ""+keyword);
			post.put("_wp_http_referer", "/wp/blog/wp-admin/user-new.php");
			post.put("noconfirmation", "true");

			lastURL = urlGen.newUser();
			System.out.println("  Request page for STATE_ADD_USER! Link: " + lastURL + " USER:" + post);
			lastHTMLReply = callHTTPServer(lastURL, post);
			return username;
		}
		return null;
	}
	
	public void deleteWebsiteData() {
		userLevel = USER_LEVEL_ADMINISTRATOR;
		username = "admin";
		password = username;
		loggedStatus = STATUS_LOGGED_OUT;
		try {
			doState(STATE_LOGIN_SEND_CREDENTIALS);
			doState(STATE_DELETE_DATA);
			doState(STATE_HOMEPAGE);
			doState(STATE_LOG_OUT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void doState(int state) throws IOException {
		int pageID;
		String paragraph;
		Hashtable<String, String> post = new Hashtable<String, String>();
		Vector<Object> ids;
		String keyword;
		Logger logger = Logger.getLogger(WordpressUserSession.class);
		if(ClientEmulator.isEndOfSimulation()) {
			logger.warn("end of simulation");
			return;
		}
		
//		NO
//		if (lastHTMLReply != null) {
//			if (lastHTMLReply.indexOf("Sorry") != -1) // Nothing matched the
//				// request, we have to go back
//				state = transition.backToPreviousState();
//		}
		WordpressURLGenerator urlGen = (WordpressURLGenerator)this.urlGen;
		switch (state) {
		case -1: // An error occured, reset to home page
			this.transitionLoggedOut.resetToInitialState();
			this.transitionLoggedIn.resetToInitialState();
			this.transition = this.transitionLoggedOut;
		case STATE_HOMEPAGE:
			lastURL = urlGen.homePage();
			System.out.println(" Request page for STATE_HOMEPAGE! Link: " + lastURL);
			lastHTMLReply = callHTTPServer(lastURL);
			break;
		case STATE_PAGE:
			pageID = extractIDFromHTML(REGEX[STATE_PAGE]);
			lastURL = urlGen.page(pageID);
			System.out.println(" Request page for STATE_PAGE! Link: " + lastURL);
			lastHTMLReply = callHTTPServer(lastURL);
			break;
		case STATE_SEARCH:
			keyword = dictionary.getWord();
			lastURL = urlGen.search(keyword);
			System.out.println(" Request page for STATE_SEARCH! Link: " + lastURL);
			lastHTMLReply = callHTTPServer(lastURL);
			break;	
		case STATE_BLOGPOST:
			pageID = extractIDFromHTML(REGEX[STATE_BLOGPOST]);
			lastURL = urlGen.blogpost(pageID);
			System.out.println(" Request page for STATE_BLOGPOST! Link: " + lastURL);
			lastHTMLReply = callHTTPServer(lastURL);
			break;
		case STATE_ARCHIVE_MONTHLY:
			pageID = extractIDFromHTML(REGEX[STATE_ARCHIVE_MONTHLY]);
			lastURL = urlGen.monthlyArchive(pageID);
			System.out.println(" Request page for STATE_ARCHIVE_MONTHLY! Link: " + lastURL);
			lastHTMLReply = callHTTPServer(lastURL);
			break;
		case STATE_CATEGORY:
			keyword = extractWordFromHTML(REGEX[STATE_CATEGORY]);
			lastURL = urlGen.category(keyword);
			System.out.println(" Request page for STATE_CATEGORY! Link: " + lastURL);
			lastHTMLReply = callHTTPServer(lastURL);
			break;		
		case STATE_AUTHOR:
			pageID = extractIDFromHTML(REGEX[STATE_AUTHOR]);
			lastURL = urlGen.author(pageID);
			System.out.println(" Request page for STATE_AUTHOR! Link: " + lastURL);
			lastHTMLReply = callHTTPServer(lastURL);
			break;
		case STATE_BLOGPOST_PAGED:
			prev = STATE_HOMEPAGE;
			if(prev==STATE_HOMEPAGE || prev==STATE_BLOGPOST_PAGED) {
				pageID = extractIDFromHTML(REGEX[STATE_BLOGPOST_PAGED]);
				if(pageID == -1)
					lastURL = urlGen.blogpostPaged(0);
				else
					lastURL = urlGen.blogpostPaged(pageID);
				System.out.println(" Request page for STATE_BLOGPOST_PAGED! Link: " + lastURL);
				lastHTMLReply = callHTTPServer(lastURL);
			}
			else {
				doState(STATE_HOMEPAGE);
				doState(STATE_BLOGPOST_PAGED);
			}
			break;
		case STATE_SEARCH_PAGED:
			prev = STATE_SEARCH;
			if(prev==STATE_SEARCH || prev==STATE_SEARCH_PAGED) {
				ids = extractMultipleIDFromHTML(REGEX[STATE_SEARCH_PAGED]);
				if(ids.size()<2)
					lastURL = urlGen.searchPaged("", 0);
				else if(Integer.parseInt(ids.get(1).toString()) == -1)
					lastURL = urlGen.searchPaged(ids.get(0).toString(), 0);
				else if(ids.get(0).toString()!="" && ids.get(1).toString()!="")
					lastURL = urlGen.searchPaged(ids.get(0).toString(), Integer.parseInt(ids.get(1).toString()));
				System.out.println(" Request page for STATE_SEARCH_PAGED! Link: " + lastURL);
				lastHTMLReply = callHTTPServer(lastURL);
			}
			else
				doState(STATE_SEARCH);
			break;
		case STATE_ARCHIVE_MONTHLY_PAGED:
			prev = STATE_ARCHIVE_MONTHLY;
			if(prev==STATE_ARCHIVE_MONTHLY || prev==STATE_ARCHIVE_MONTHLY_PAGED) {
				ids = extractMultipleIDFromHTML(REGEX[STATE_ARCHIVE_MONTHLY_PAGED]);
				if(ids.size()<2)
					lastURL = urlGen.monthlyArchivePaged("", 0);
				else if(Integer.parseInt(ids.get(1).toString()) == -1)
					lastURL = urlGen.monthlyArchivePaged(ids.get(0).toString(), 0);
				else if(ids.get(0).toString()!="" && ids.get(1).toString()!="")
					lastURL = urlGen.monthlyArchivePaged(ids.get(0).toString(), Integer.parseInt(ids.get(1).toString()));
				System.out.println(" Request page for STATE_ARCHIVE_MONTHLY_PAGED! Link: " + lastURL);
				lastHTMLReply = callHTTPServer(lastURL);
			}
			else
				doState(STATE_ARCHIVE_MONTHLY);
			break;
		case STATE_CATEGORY_PAGED:
			prev = STATE_CATEGORY;
			if(prev==STATE_CATEGORY || prev==STATE_CATEGORY_PAGED) {
				ids = extractMultipleIDFromHTML(REGEX[STATE_CATEGORY_PAGED]);
				if(ids.size()<2)
					lastURL = urlGen.categoryPaged("", 0);
				else if(Integer.parseInt(ids.get(1).toString()) == -1)
					lastURL = urlGen.categoryPaged(ids.get(0).toString(), 0);
				else if(ids.get(0).toString()!="" && ids.get(1).toString()!="")
					lastURL = urlGen.categoryPaged(ids.get(0).toString(), Integer.parseInt(ids.get(1).toString()));
				System.out.println(" Request page for STATE_CATEGORY_PAGED! Link: " + lastURL);
				lastHTMLReply = callHTTPServer(lastURL);
			}
			else
				doState(STATE_CATEGORY);
			break;
		case STATE_AUTHOR_PAGED:
			prev = STATE_AUTHOR;
			if(prev==STATE_AUTHOR || prev==STATE_AUTHOR_PAGED) {
				ids = extractMultipleIDFromHTML(REGEX[STATE_AUTHOR_PAGED]);
				if(ids.size()<2)
					lastURL = urlGen.authorPaged(1, 0);
				else if(Integer.parseInt(ids.get(1).toString()) == -1)
					lastURL = urlGen.authorPaged(1, 0);
				else if(ids.get(0).toString()!="" && ids.get(1).toString()!="")
					lastURL = urlGen.authorPaged(Integer.parseInt(ids.get(0).toString()), Integer.parseInt(ids.get(1).toString()));
				System.out.println(" Request page for STATE_AUTHOR_PAGED! Link: " + lastURL);
				lastHTMLReply = callHTTPServer(lastURL);
			}
			else
				doState(STATE_AUTHOR);
			break;
		/* case STATE_LOGIN:
			lastURL = urlGen.sendCredentials();
			System.out.println(" Request page for STATE_LOGIN! Link: " + lastURL);
			lastHTMLReply = callHTTPServer(lastURL);
			break;
		
		case STATE_RSS_ENTRIES:
			lastURL = urlGen.rssFeed();
			System.out.println(" Request page for STATE_RSS_ENTRIES! Link: " + lastURL);
			lastHTMLReply = callHTTPServer(lastURL);
			break;
		case STATE_RSS_COMMENTS:
			lastURL = urlGen.rssCommentsFeed();
			System.out.println(" Request page for STATE_RSS_COMMENTS! Link: " + lastURL);
			lastHTMLReply = callHTTPServer(lastURL);
			break; */
		case STATE_WORDPRESS_WEBSITE:
			lastURL = urlGen.wpWebsite();
			System.out.println(" Request page for STATE_RSS_COMMENTS! Link: " + lastURL);
			lastHTMLReply = callHTTPServer(lastURL);
			break;
		case STATE_ADD_ANONYMOUS_COMMENT:
			prev = STATE_BLOGPOST;
			if(prev==STATE_BLOGPOST || prev==STATE_PAGE) {
				post.clear();
				post.put("author", dictionary.getWord());
				post.put("email", dictionary.getWord()+"@yahoo.com");
				post.put("url", "");
				paragraph = dictionary.getParagraph(1 + (rand.nextInt() % properties.getCommentMaximumLength()));
				if(paragraph == "")
					paragraph = dictionary.getWord();
				post.put("comment", paragraph);
				post.put("comment_parent", extractIDFromHTML(REGEX[STATE_ADD_ANONYMOUS_COMMENT-1])+"");
				pageID = extractIDFromHTML(REGEX[STATE_ADD_ANONYMOUS_COMMENT]);
				post.put("comment_post_ID", pageID + "");
				lastURL = urlGen.writeComment();
				System.out.println(" Request page for STATE_ADD_ANONYMOUS_COMMENT! Link: " + lastURL + " POST:" + post);
				lastHTMLReply = callHTTPServer(lastURL, post);
			}
			else
				doState(STATE_BLOGPOST);
			break;
		case STATE_LOGIN_SEND_CREDENTIALS:
			// user_login or user_email  ;  pass1  or pass2
			// redirect_to; rememberme
			// log ; pwd
			// testcookie  $_COOKIE[TEST_COOKIE]
			
			// wp-login.php -> wp_signon() -> user.php
			
			lastURL = urlGen.sendCredentials();
			lastHTMLReply = callHTTPServer(lastURL);
			
			post.clear();
			System.out.println(username+"\n\n");
			post.put("log", username);
			post.put("pwd", password);
			//post.put("user_login", username);
			//post.put("pass1", password);
			post.put("action", "login");
			post.put("testcookie", "true");
			
			lastURL = urlGen.sendCredentials();
			System.out.println(" Request page for STATE_LOGIN_SEND_CREDENTIALS! Link: " + lastURL);
			lastHTMLReply = callHTTPServer(lastURL, post);
			loggedStatus = STATUS_LOGGED_IN;
			transition = transitionLoggedIn;
			break;
		case STATE_LOG_OUT:
			if(loggedStatus == STATUS_LOGGED_IN) {
				keyword = extractWordFromHTML(REGEX[14]);
				System.out.println("keyword=" + keyword);
				lastURL = urlGen.logOut(keyword);
				System.out.println(" Request page for STATE_LOG_OUT! Link: " + lastURL);
				lastHTMLReply = callHTTPServer(lastURL, post);
				resetCookies();
				lastURL = urlGen.logOutFinalStep();
				System.out.println(" Link: " + lastURL);
				lastHTMLReply = callHTTPServer(lastURL);
				loggedStatus = STATUS_LOGGED_OUT;
				transition = transitionLoggedOut;
			}
			break;
		case STATE_ADD_COMMENT:
			prev = STATE_BLOGPOST;
			if(loggedStatus == STATUS_LOGGED_IN && (prev==STATE_BLOGPOST || prev==STATE_PAGE || prev==STATE_ADD_COMMENT)) {
				post.clear();
				post.put("author", username);
				post.put("email", username + "@"+ username +".com");
				post.put("url", "");
				paragraph = dictionary.getParagraph(1 + (rand.nextInt() % properties.getCommentMaximumLength()));
				if(paragraph == "")
					paragraph = dictionary.getWord();
				post.put("comment", paragraph);
				post.put("comment_parent", extractIDFromHTML(REGEX[STATE_ADD_ANONYMOUS_COMMENT-1])+"");
				pageID = extractIDFromHTML(REGEX[STATE_ADD_ANONYMOUS_COMMENT]);
				post.put("comment_post_ID", pageID + "");
				lastURL = urlGen.writeComment();
				System.out.println(" Request page for STATE_ADD_COMMENT! Link: " + lastURL + " POST:" + post);
				lastHTMLReply = callHTTPServer(lastURL, post);
			}
		break;
		case STATE_ADD_POST:
			if(loggedStatus == STATUS_LOGGED_IN) {
				lastURL = urlGen.newPostForm("post", 0);
				System.out.println(" Link: " + lastURL);
				lastHTMLReply = callHTTPServer(lastURL);
				
				post.clear();
				keyword = extractWordFromHTML(REGEX[16]);
				post.put("_wpnonce", keyword);
				pageID = extractIDFromHTML(REGEX[17]);
				post.put("user_ID", ""+pageID);
				post.put("action", "editpost");
				post.put("originalaction", "editpost");
				post.put("post_author", ""+pageID);
				post.put("post_type", "post");
				post.put("original_post_status", "auto-draft");
				post.put("auto_draft", "1");
				pageID = extractIDFromHTML(REGEX[22]);
				post.put("post_category[]", ""+pageID);
				pageID = extractIDFromHTML(REGEX[15]);
				post.put("post_ID", ""+pageID);
				keyword = extractWordFromHTML(REGEX[18]);
				post.put("autosavenonce", keyword);
				keyword = extractWordFromHTML(REGEX[19]);
				post.put("meta-box-order-nonce", keyword);
				keyword = extractWordFromHTML(REGEX[20]);
				post.put("closedpostboxesnonce", keyword);	
				post.put("comment_status", "open");
				
				paragraph = dictionary.getParagraph(1 + (rand.nextInt() % properties.getCommentMaximumLength()));
				if(paragraph == "")
					paragraph = dictionary.getWord();
				post.put("content", paragraph);
				post.put("post_title", dictionary.getWord());
				post.put("publish", "true");
				
				lastURL = urlGen.newPostPublish("post", pageID);
				System.out.println("  Request page for STATE_ADD_POST! Link: " + lastURL + " POST:" + post);
				lastHTMLReply = callHTTPServer(lastURL, post);
			}
		break;		
		case STATE_ADD_PAGE:
			if(loggedStatus == STATUS_LOGGED_IN) {
				lastURL = urlGen.newPostForm("page", 0);
				System.out.println(" Link: " + lastURL + " PAGE:" + post);
				lastHTMLReply = callHTTPServer(lastURL);
	
				post.clear();
				keyword = extractWordFromHTML(REGEX[16]);
				post.put("_wpnonce", keyword);
				pageID = extractIDFromHTML(REGEX[17]);
				post.put("user_ID", ""+pageID);
				post.put("action", "editpost");
				post.put("originalaction", "editpost");
				post.put("post_author", ""+pageID);
				post.put("post_type", "post");
				post.put("original_post_status", "auto-draft");
				post.put("auto_draft", "1");
				pageID = extractIDFromHTML(REGEX[15]);
				post.put("post_ID", ""+pageID);
				keyword = extractWordFromHTML(REGEX[18]);
				post.put("autosavenonce", keyword);
				keyword = extractWordFromHTML(REGEX[19]);
				post.put("meta-box-order-nonce", keyword);
				keyword = extractWordFromHTML(REGEX[20]);
				post.put("closedpostboxesnonce", keyword);
				post.put("comment_status", "open");
				
				paragraph = dictionary.getParagraph(1 + (rand.nextInt() % properties.getCommentMaximumLength()));
				if(paragraph == "")
					paragraph = dictionary.getWord();
				post.put("content", paragraph);
				post.put("post_title", dictionary.getWord());
				post.put("publish", "true");
				
				lastURL = urlGen.newPostPublish("page", pageID);
				System.out.println("  Request page for STATE_ADD_PAGE! Link: " + lastURL + " PAGE:" + post);
				lastHTMLReply = callHTTPServer(lastURL, post);
			}
		break;
		case STATE_ADD_CATEGORY:
			if(loggedStatus == STATUS_LOGGED_IN) {
				lastURL = urlGen.newCategory();
				System.out.println(" Request page for STATE_ADD_CATEGORY! Link: " + lastURL);
				lastHTMLReply = callHTTPServer(lastURL);

				post.clear();
				post.put("log", username);
				post.put("pwd", password);
				post.put("action", "add-tag");
				post.put("screen", "edit-category");
				post.put("taxonomy", "category");
				post.put("post_type", "post");
				keyword = extractWordFromHTML(REGEX[23]);
				post.put("_wpnonce", keyword);
				post.put("_wp_http_referer", "/wp/blog/wp-admin/edit-tags.php?taxonomy=category");
				
				keyword = dictionary.getWord();
				post.put("tag-name", keyword);
				post.put("parent", "-1");
				
				paragraph = dictionary.getParagraph(1 + (rand.nextInt() % properties.getCommentMaximumLength()));
				if(paragraph == "")
					paragraph = dictionary.getWord();
				post.put("description", paragraph);
	
				lastURL = urlGen.newCategory();
				System.out.println("  Link: " + lastURL + " POST:" + post);
				lastHTMLReply = callHTTPServer(lastURL, post);
			}
		break;
		case STATE_ADD_USER:
			if(userLevel == USER_LEVEL_ADMINISTRATOR) {
				lastURL = urlGen.newUser();
				System.out.println(" Link: " + lastURL);
				lastHTMLReply = callHTTPServer(lastURL);
				
				post.clear();
				keyword = "user"+rand.nextInt()%1000; // !!!!!!!!!!!!
				post.put("user_login", keyword);
				post.put("email", keyword+"@"+keyword+".com");
				post.put("pass1", keyword);
				post.put("pass2", keyword);
				post.put("role", "editor");
				post.put("action", "createuser");
				keyword = extractWordFromHTML(REGEX[21]);
				post.put("_wpnonce_create-user", ""+keyword);
				post.put("_wp_http_referer", "/wp/blog/wp-admin/user-new.php");
				post.put("noconfirmation", "true");

				lastURL = urlGen.newUser();
				System.out.println("  Request page for STATE_ADD_USER! Link: " + lastURL + " USER:" + post);
				lastHTMLReply = callHTTPServer(lastURL, post);
			}
		break;
		case STATE_DELETE_DATA:
			if(userLevel == USER_LEVEL_ADMINISTRATOR) {
				System.out.println(" Request page for STATE_DELETE_DATA! Link: " + lastURL);
		        String url = "jdbc:mysql://localhost:3306/";
	
		        String dbName = "wpblog";
		        String userName = "root";
		        String password = "root";
	            java.sql.Connection con;
	            Statement stmt;
	            try {
	            	Class.forName("com.mysql.jdbc.Driver").newInstance ();
	            	con = DriverManager.getConnection(url+dbName, userName, password);
	            	for (int i=0;i<DELETE_QUERY.length;i++) { 
		            	try {
		            		stmt = ((java.sql.Connection) con).createStatement();
		            		//String query = "DELETE FROM wp_users WHERE ID <> '1'";
		            		int deletedRows=stmt.executeUpdate(DELETE_QUERY[i]);
		            		if ( deletedRows > 0 ) {
		            			System.out.println("Deleted All Rows In The Table Successfully...");
		            		}
		            		else {
		            			System.out.println("Table already empty.");
		            		}
		            	}
		            	catch(SQLException s) {
		            		System.out.println("Deleted All Rows In  Table Error. ");
		            		s.printStackTrace();
		            	}
	            	}
	            	// close Connection
	            	con.close();
	            }
	            catch (Exception e) {
	            	e.printStackTrace();
	            }
			}
		break;
		default:
			Logger.getLogger(Thread.currentThread().getName()).error("DID NOT MATCH ANY STATE");
		}
	}
	
	/**
	 * Emulate a user session using the current transition table.
	 */
	public void run() {
		int nbOfTransitions = STATES.length;
		int next = 0;
		long time, lastRequestTime;
		long startSession, endSession;

		Logger logger = Logger.getLogger(WordpressUserSession.class);

		userLevel = USER_LEVEL_EDITOR;
		loggedStatus = STATUS_LOGGED_OUT;
//		try {
//			Thread.sleep(rand.nextInt(40000) + 1);// max 40 seconds
//		} catch (InterruptedException e) {
//		}

		nbOfTransitions = ( rand.nextInt() % ( properties.getMaxNumberOfTransitions() -1 ) ) + 1;
		startSession = System.currentTimeMillis();
		// Start from Home Page
		transitionLoggedOut.resetToInitialState();
		transitionLoggedIn.resetToInitialState();
		transition = transitionLoggedOut;
		state = transitionLoggedOut.getCurrentState();
		
		
//		try {
//			for (int i=0; i<16;i++) {
//				System.out.print("State nr " + i + ": ");
//				doState(i);
//			}
//			if(true)
//				System.exit(0);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}	

//		try {
//			doState(STATE_HOMEPAGE);
//			doState(STATE_PAGE);
//			doState(STATE_SEARCH);
//			doState(STATE_SEARCH_PAGED);
//			doState(STATE_BLOGPOST);
//			doState(STATE_HOMEPAGE);
//			doState(STATE_BLOGPOST_PAGED);
//			doState(STATE_ARCHIVE_MONTHLY);
//			doState(STATE_ARCHIVE_MONTHLY_PAGED);
//			doState(STATE_CATEGORY);
//			doState(STATE_CATEGORY_PAGED);
//			doState(STATE_WORDPRESS_WEBSITE);
//			doState(STATE_AUTHOR);
//			doState(STATE_AUTHOR_PAGED);
//			doState(STATE_BLOGPOST);
//			doState(STATE_ADD_ANONYMOUS_COMMENT);	//? 404
//
//			doState(STATE_LOGIN_SEND_CREDENTIALS);
//			doState(STATE_HOMEPAGE);
//			doState(STATE_BLOGPOST);
//			doState(STATE_ADD_COMMENT);				//? 404
//			doState(STATE_ADD_POST);
//			doState(STATE_ADD_PAGE);
//			doState(STATE_ADD_USER);
//			doState(STATE_ADD_CATEGORY);			//? 403 or 302 x 4times
//			//doState(STATE_DELETE_DATA);
//			doState(STATE_LOG_OUT);					//? 403
//
//			System.exit(0);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
			
		while (!ClientEmulator.isEndOfSimulation() && nbOfTransitions > 0) {
			// Compute next step and call HTTP server (also measure time
			// spend in server call)
			time = System.currentTimeMillis();
			try {
				doState(state);
				prev = state;
			} catch (NullPointerException e) {
				stats.incrementError(state);
				transitionLoggedOut.resetToInitialState();
				transitionLoggedIn.resetToInitialState();
				transition = transitionLoggedOut;
				state = transition.getCurrentState();
				continue;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			lastRequestTime = System.currentTimeMillis() - time;
			stats.updateTime(state, lastRequestTime);
			logger.info("Requested '" + STATES[state] + "' in " + lastRequestTime + "ms");
			logger.debug("did '" + lastURL + "' in " + lastRequestTime + "ms");
			
			// If an error occured, reset to Home page
			if ((lastHTMLReply == null)
					|| (lastHTMLReply.indexOf("ERROR") != -1)) {
				logger.warn("Resetting to initial state");
				stats.incrementError(state);
				transitionLoggedOut.resetToInitialState();
				transitionLoggedIn.resetToInitialState();
				transition = transitionLoggedOut;
				next = transition.getCurrentState();
			} else {
				stats.incrementCount(state);
				for (;;) {
					next = transition.nextState();
					if(!stateInvalid(next))
						break;
					transition.backToPreviousState();
				}
				System.out.println(" * CURRENT STATE "+ nbOfTransitions + " : " + transition.getCurrentStateName());
			}
			state = next; 
			nbOfTransitions--;
					
//			try {
//		    	  long sleepTime = transition.TPCWthinkTime();
//		    	  Thread.sleep(sleepTime);
//		    	  Logger.getLogger(Thread.currentThread().getName()).info("slept for " + sleepTime + "ms");
//			 }catch (java.lang.InterruptedException ie) {
//			    	Logger.getLogger(Thread.currentThread().getName()).error("Thread "+Thread.currentThread().getName()+" has been interrupted.");
//			 }

		}
		if ((transition.isEndOfSession()) || (nbOfTransitions == 0)) {
			endSession = System.currentTimeMillis();
			long sessionTime = endSession - startSession;
			stats.addSessionTime(sessionTime);
		}
	}
	
	boolean stateInvalid(int state) {
		/*if((state == STATE_ACTION_EDIT || state == STATE_ACTION_DELETE) && actionId == -1)
			return true;
		if((state == STATE_TICKET_EDIT || state == STATE_TICKET_DELETE || state == STATE_TICKET_VIEW_DETAILS || state == STATE_ACTION_ADD) && ticketId == -1)
			return true;*/
		return false;
	}

}
