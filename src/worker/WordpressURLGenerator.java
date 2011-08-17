package worker;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This abstract class provides the needed URLs to access all states of the WordPress Website.
 * Requires IDs and nonces from previous HTTP responses received from the Web server.
 * 
 * @author <a href="mailto:a.andronescu@student.vu.nl">Alexandra Andronescu</a>
 */

public class WordpressURLGenerator extends worker.URLGenerator {	
	public WordpressURLGenerator(String host, int port, String ScriptFilesPath) {
		super(host, port, ScriptFilesPath);
	}
	
	private String listAddressScript() {
		return "";
	}

	public URL homePage() {
		return createURL(listAddressScript());
	}

	public URL page(int pageID) {
		return createURL(listAddressScript() + "?page_id=" + pageID);
	}
	
	public URL search(String keyword) {
		return createURL(listAddressScript() + "?s=" + keyword);
	}
	
	public URL blogpost(int pageID) {
		return createURL(listAddressScript() + "?p=" + pageID);
	}
	
	public URL monthlyArchive(int pageID) {
		return createURL(listAddressScript() + "?m=" + pageID);
	}
	
	public URL category(String keyword) {
		return createURL(listAddressScript() + "?category_name=" + keyword);
	}

	public URL author(int authorID) {
		return createURL(listAddressScript() + "?author=" + authorID);
	}
	
	public URL blogpostPaged(int pageNr) {
		return createURL(listAddressScript() + "?paged=" + pageNr);
	}
	
	public URL searchPaged(String keyword, int pageNr) {
		return createURL(listAddressScript() + "?s=" + keyword + "&paged=" + pageNr);
	}
	
	public URL monthlyArchivePaged(String keyword, int pageNr) {
		return createURL(listAddressScript() + "?m=" + keyword + "&paged=" + pageNr);
	}
	
	public URL categoryPaged(String keyword, int pageNr) {
		return createURL(listAddressScript() + "?category_name=" + keyword + "&paged=" + pageNr);
	}
	
	public URL authorPaged(int authorID, int pageNr) {
		return createURL(listAddressScript() + "?author=" + authorID + "&paged=" + pageNr);
	}
	
	public URL wpWebsite() {
		try{
			return new URL("http://wordpress.org:80/");
		} catch (MalformedURLException e) {
			System.out.println("Error while generating script path: "
					+ e.getMessage());
			return null;
		}
	}
	
	public URL writeComment() {
		return createURL(listAddressScript() + "wp-comments-post.php");
	}
	
	public URL sendCredentials() {
		return createURL(listAddressScript() + "wp-login.php");//"wp-admin/index.php");
	}
	
	public URL logOut(String nonce) {
		return createURL(listAddressScript() + "wp-login.php?action=logout&_wpnonce=" + nonce);
	}
	
	public URL logOutFinalStep() {
		return createURL(listAddressScript() + "wp-login.php?loggedout=true");
	}
	
	public URL newPostForm(String type, int postID) {
		return createURL(listAddressScript() + "wp-admin/post-new.php?post_type="+type);
	}
	
	public URL newPostPublish(String type, int postID) {
		return createURL(listAddressScript() + "wp-admin/post.php?post="+postID+"&action=edit&message=6");
	}
	
	public URL newUser() {
		return createURL(listAddressScript() + "wp-admin/user-new.php");
	}
	
	public URL newCategory() {
		return createURL(listAddressScript() + "wp-admin/edit-tags.php?taxonomy=category");
	}
}
