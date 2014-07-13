package org.apache.synapse.synapseobject;


import org.apache.axis2.om.OMElement;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Vikas
 * Date: Feb 7, 2006
 * Time: 10:05:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class OMUtil {

    public static OMElement getOMElementWithAttributeName(OMElement omElement, String attribName) {

        OMElement result = null;
        if (hasAtributeWithName(omElement, attribName))
        {
            return omElement;
        }
        else{
            Iterator elementItr = omElement.getChildElements();
            while(elementItr.hasNext()){
                OMElement element = (OMElement)elementItr.next();
                result = getOMElementWithAttributeName(element,attribName);
                if(result!=null)
                {
                      break;
                }
            }
        }
        return result;
    }


    public static OMElement[] getOMElementsWithAttributeName(OMElement omElement, String attribName) {

        ArrayList resultArray = new ArrayList();
        resultArray = getOMElementsWithAttributeName(omElement, attribName, resultArray);
        int length = resultArray.size();
        OMElement[] result = new OMElement[length];
        for(int i=0;i<length;i++){
            result[i] = (OMElement)resultArray.get(i);
        }
        return result;
    }


    private static ArrayList getOMElementsWithAttributeName(OMElement omElement, String attribName, ArrayList resultArray) {
        if (hasAtributeWithName(omElement, attribName))
        {
            resultArray.add(omElement);
        }
        else{
            Iterator elementItr = omElement.getChildElements();
            while(elementItr.hasNext()){
                OMElement element = (OMElement)elementItr.next();
                resultArray = getOMElementsWithAttributeName(element,attribName, resultArray);
            }
        }
        return resultArray;
    }


    public static boolean hasAtributeWithName(OMElement omElement, String attribName) {

        if (omElement.getNamespace()!= null) {
            if ((omElement.getAttributeValue(new QName(omElement.getNamespace().getName(), attribName))) != null) {
                return true;
            }
        } else if ((omElement.getAttributeValue(new QName(attribName))) != null) {
            return true;
        }
        return false;
    }


     // THE METHODS USED FOR GETTING OMElements BASED ON THE ATTRIBUTE NAME & VALUE


    public static OMElement getOMElementWithAttribute(OMElement omElement, String attribName, String attribValue) {

        OMElement result = null;
        if (hasAtribute(omElement, attribName, attribValue))
        {
            return omElement;
        }
        else{
            Iterator elementItr = omElement.getChildElements();
            while(elementItr.hasNext()){
                OMElement element = (OMElement)elementItr.next();
                result = getOMElementWithAttributeName(element,attribName);
                if(result!=null)
                {
                      break;
                }
            }
        }
        return result;
    }


    public static OMElement[] getOMElementsWithAttribute(OMElement omElement, String attribName, String attribValue) {

        ArrayList resultArray = new ArrayList();
        resultArray = getOMElementsWithAttribute(omElement, attribName, attribValue, resultArray);
        int length = resultArray.size();
        OMElement[] result = new OMElement[length];
        for(int i=0;i<length;i++){
            result[i] = (OMElement)resultArray.get(i);
        }
        return result;
    }


    private static ArrayList getOMElementsWithAttribute(OMElement omElement, String attribName, String attribValue, ArrayList resultArray) {
        if (hasAtribute(omElement, attribName, attribValue))
        {
            resultArray.add(omElement);
        }
        else{
            Iterator elementItr = omElement.getChildElements();
            while(elementItr.hasNext()){
                OMElement element = (OMElement)elementItr.next();
                resultArray = getOMElementsWithAttribute(element,attribName, attribValue, resultArray);
            }
        }
        return resultArray;
    }


    public static boolean hasAtribute(OMElement omElement, String attribName, String attribValue) {

        if (omElement.getNamespace()!= null) {
            if (omElement.getAttributeValue(new QName(omElement.getNamespace().getName(), attribName))!=null) {
                if(omElement.getAttributeValue(new QName(omElement.getNamespace().getName(), attribName)).equals(attribValue))
                return true;
            }
        } else if(omElement.getAttributeValue(new QName(attribName))!=null) {
                if(omElement.getAttributeValue(new QName(attribName)).equals(attribValue))
                return true;
            }

        return false;
    }



}
