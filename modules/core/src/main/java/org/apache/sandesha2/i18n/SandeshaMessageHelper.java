/*
 * Copyright 2006 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *  
 */

package org.apache.sandesha2.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.axis2.i18n.MessageBundle;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.i18n.ProjectResourceBundle;

public class SandeshaMessageHelper {
  public static final String projectName = "org.apache.sandesha2".intern();
  public static final String resourceName = "resource".intern();
  public static final Locale locale = null;
  public static final String msgBundleKey = projectName; 

  public static final String rootPackageName = "org.apache.sandesha2.i18n".intern();

  public static final ResourceBundle rootBundle =
          ProjectResourceBundle.getBundle(projectName,
                  rootPackageName,
                  resourceName,
                  locale,
                  SandeshaMessageHelper.class.getClassLoader(),
                  null);
  
  public static void innit(){
		MessageBundle bundle = new MessageBundle(
				projectName,
        rootPackageName,
        resourceName,
        locale,
        SandeshaMessageHelper.class.getClassLoader(),
        rootBundle);
		
		Messages.addMessageBundle(msgBundleKey, bundle);
  }
  
  
  /**
   * Get a message from resource.properties from the package of the given object.
   *
   * @param key The resource key
   * @return The formatted message
   */
  public static String getMessage(String key)
          throws MissingResourceException{
  	try{
  		return Messages.getMessageFromBundle(msgBundleKey, key);
  	}
  	catch(MissingResourceException e){
  		throw e;
  	}
  	catch(Exception e){
  		return null;
  	}
  }
  
  /**
   * Get a message from resource.properties from the package of the given object.
   *
   * @param key  The resource key
   * @param arg0 The argument to place in variable {0}
   * @return The formatted message
   */
  public static String getMessage(String key, String arg0)
          throws MissingResourceException{
  	try{
  		return Messages.getMessageFromBundle(msgBundleKey, key, arg0);
  	}
  	catch(MissingResourceException e){
  		throw e;
  	}
  	catch(Exception e){
  		return null;
  	}
  }
  

  /**
   * Get a message from resource.properties from the package of the given object.
   *
   * @param key  The resource key
   * @param arg0 The argument to place in variable {0}
   * @param arg1 The argument to place in variable {1}
   * @return The formatted message
   */
  public static String getMessage(String key, String arg0, String arg1)
          throws MissingResourceException{
  	try{
  		return Messages.getMessageFromBundle(msgBundleKey, key, arg0, arg1);
  	}
  	catch(MissingResourceException e){
  		throw e;
  	}
  	catch(Exception e){
  		return null;
  	}
  }
  
  /**
   * Get a message from resource.properties from the package of the given object.
   *
   * @param key  The resource key
   * @param arg0 The argument to place in variable {0}
   * @param arg1 The argument to place in variable {1}
   * @param arg2 The argument to place in variable {2}
   * @return The formatted message
   */
  public static String getMessage(String key, String arg0, String arg1, String arg2)
          throws MissingResourceException{
  	try{
  		return Messages.getMessageFromBundle(msgBundleKey, key, arg0, arg1, arg2);
  	}
  	catch(MissingResourceException e){
  		throw e;
  	}
  	catch(Exception e){
  		return null;
  	}
  }
  
  /**
   * Get a message from resource.properties from the package of the given object.
   *
   * @param key  The resource key
   * @param arg0 The argument to place in variable {0}
   * @param arg1 The argument to place in variable {1}
   * @param arg2 The argument to place in variable {2}
   * @param arg3 The argument to place in variable {3}
   * @return The formatted message
   */
  public static String getMessage(String key, String arg0, String arg1, String arg2, String arg3)
          throws MissingResourceException{
  	try{
  		return Messages.getMessageFromBundle(msgBundleKey, key, arg0, arg1, arg2, arg3);
  	}
  	catch(MissingResourceException e){
  		throw e;
  	}
  	catch(Exception e){
  		return null;
  	}
  }
  
  /**
   * Get a message from resource.properties from the package of the given object.
   *
   * @param key  The resource key
   * @param arg0 The argument to place in variable {0}
   * @param arg1 The argument to place in variable {1}
   * @param arg2 The argument to place in variable {2}
   * @param arg3 The argument to place in variable {3}
   * @param arg4 The argument to place in variable {4}
   * @return The formatted message
   */
  public static String getMessage(String key, String arg0, String arg1, String arg2, String arg3, String arg4)
          throws MissingResourceException{
  	try{
  		return Messages.getMessageFromBundle(msgBundleKey, key, arg0, arg1, arg2, arg3, arg4);
  	}
  	catch(MissingResourceException e){
  		throw e;
  	}
  	catch(Exception e){
  		return null;
  	}
  }

  
  

}

