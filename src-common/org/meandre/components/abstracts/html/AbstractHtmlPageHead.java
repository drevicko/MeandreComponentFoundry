/**
 * 
 */
package org.meandre.components.abstracts.html;

import java.util.Hashtable;
import java.util.Set;

/**
 * @author bernie acs
 *
 */
public abstract class AbstractHtmlPageHead {
	
	private static String pageTitle = "";
	private static StringBuffer defaultHeadHtmlMetaTagsFragment = new StringBuffer();
	private static StringBuffer defaultHeadHtmlScriptsFragment = new StringBuffer();	
	private static StringBuffer defaultHeadHtmlCssTagsFragment = new StringBuffer();
	
	private Hashtable<String,String> selectiveHeadHtmlFragments = new Hashtable<String,String>();
	
	/**
	 * 
	 */
	public void initializeHead(){
		defaultHeadHtmlMetaTagsFragment.append("");
		defaultHeadHtmlScriptsFragment.append("");
		defaultHeadHtmlCssTagsFragment.append("");
	}
	/**
	 * 
	 * @param title
	 */
	public void setPageTitle(String title){
		pageTitle = title;
	}
	
	/**
	 * 
	 * @param fragment
	 */
	public void setHeadDefaultHtmlMetaTagsFragment(String fragment){
		defaultHeadHtmlMetaTagsFragment.append(fragment);
	}

	/**
	 * 
	 * @param fragment
	 */
	public void setHeadDefaultHtmlScriptsFragment(String fragment){
		defaultHeadHtmlScriptsFragment.append(fragment);
	}

	/**
	 * 
	 * @param fragment
	 */
	public void setHeadDefaultHtmlCssTagsFragment(String fragment){
		defaultHeadHtmlCssTagsFragment.append(fragment);
	}
	
	/**
	 * 
	 * @param key
	 * @param fragment
	 */
	public void setHeadSelectiveHtmlFragments(String key, String fragment){
		selectiveHeadHtmlFragments.put(key, fragment);
	}

	/**
	 * getHtml will return HtmlHead built in two
	 * segment concatenated together. The first 
	 * part is made-up of the following;
	 * 
	 * <HTML>
	 *   <Head>
	 *   <Title>
	 *   setPageTitle as defined, defaults to ""
	 *   setDefaultHtmlMetaTagsFragment as defined
	 *   defaultHtmlScriptsFragment 
	 *   
	 * The second part is made up of the following;
	 * 
	 *   defaultHtmlCssTagsFagment
	 *   </Head>
	 *  
	 * Selective fragment(s) can be called for and
	 * would be insert between the two parts described
	 *  
	 * @return
	 * 
	 */
	public String getHeadHtml(){
		StringBuffer s = new StringBuffer();
		s.append( getHeadHtmlOpen() );
		s.append( getHeadHtmlClose() );
		return s.toString();
	}

	/**
	 * getHtml includes fragment previously stored
	 * using setSelectiveHtmlFragments(); Includes
	 * the fragment between HtmlOpen and HtmlClose
	 * as described in getHtml(). 
	 *  
	 * @param keys
	 * @return
	 * 
	 */
	public String getHeadHtml(String key){
		StringBuffer s = new StringBuffer();
		s.append( getHeadHtmlOpen() );
		if( (selectiveHeadHtmlFragments != null)
		  &&(selectiveHeadHtmlFragments.containsKey(key)) 
		){
			s.append( selectiveHeadHtmlFragments.get(key));
		}
		s.append( getHeadHtmlClose() );
		return s.toString();
	}

	/**
	 * getHtml including multiple fragment previously stored
	 * using setSelectiveHtmlFragements(); The argument will
	 * be processed using for(key : keys) which will determine
	 * the order that fragments will be inserted. The inserts
	 * are the same as described in getHtml(String). 
	 *  
	 * @param keys
	 * @return
	 * 
	 */
	public String getHeadHtml(Set<String> keys){
		StringBuffer s = new StringBuffer();
		s.append( getHeadHtmlOpen() );
		if( (selectiveHeadHtmlFragments != null)
		  && (keys.size() > 0 ) 
		){
			for(String key: keys){
				if( (selectiveHeadHtmlFragments != null)
					&&(selectiveHeadHtmlFragments.containsKey(key)) 
				){
					String frag = selectiveHeadHtmlFragments.get(key);
					if( frag != null )
						s.append( frag );
				}
			}
		}
		s.append( getHeadHtmlClose() );
		return s.toString();
	}
	
	private String getHeadHtmlOpen(){
		StringBuffer s = new StringBuffer();
		
		s.append("<HTML>\n");
		s.append("<Head>\n");
		s.append("<Title>\n");
		s.append( pageTitle );
		s.append("</Title>\n");
		s.append(defaultHeadHtmlMetaTagsFragment);
		s.append(defaultHeadHtmlScriptsFragment);
		
		return s.toString();
	}
	
	private String getHeadHtmlClose(){
		StringBuffer s = new StringBuffer();

		s.append(defaultHeadHtmlCssTagsFragment);		
		s.append("</Head>\n");
		
		return s.toString();
	}
	
}
