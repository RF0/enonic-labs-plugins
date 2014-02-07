package com.enonic.plugins.datarefine;

import com.enonic.cms.api.client.Client;
import com.enonic.cms.api.client.model.*;
import com.enonic.cms.api.client.model.content.*;
import com.enonic.cms.api.plugin.PluginConfig;
import com.enonic.cms.api.plugin.ext.http.HttpController;
import com.enonic.plugins.datarefine.view.FreeMarkerViewRenderer;
import com.enonic.plugins.datarefine.utils.Helper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import com.google.common.base.Strings;

public class DatarefineControllerPlugin extends HttpController {

    PluginConfig config;
    Client client;
    private FreeMarkerViewRenderer viewRenderer;
    Boolean commitOperationsToDB = false;

    Logger logger = LoggerFactory.getLogger(DatarefineControllerPlugin.class);

    Map<String, String> anObject;
    Map<String, String> aModifiedObject;
    List<Map> someObjects;
    List<Map> someModifiedObjects;
    List<String> objectFields = new ArrayList<String>();
    Map<String, String> objectFieldType = new HashMap<String,String>();

    Map<Integer, Element> someObjectsCache = new HashMap<Integer, Element>();
    Map<Integer, Document> contenttypeConfigCache = new HashMap<Integer, Document>();

    List<String> operationsTargetsList = new ArrayList<String>();
    String[] operations = null;
    String[] operationsTargets = null;

    Boolean replaceInString = false;
    String replaceInStringSrc;
    String replaceInStringTarget;

    Map<String, Object> model;
    ContentDataInput contentDataInput;

    String contentKey;
    String contentType;

    public void setClient(Client client) {
        this.client = client;
    }

    public void setConfig(PluginConfig config) {
        this.config = config;
    }

    public void setViewRenderer(FreeMarkerViewRenderer viewRenderer) {
        this.viewRenderer = viewRenderer;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = request.getRequestURI();
        if (requestUri.endsWith(".css")) {
            serveCssFile(response, requestUri);
        } else {
            model = new HashMap<String, Object>();
            someObjects = new ArrayList<Map>();
            someModifiedObjects = new ArrayList<Map>();
            objectFields = new ArrayList<String>();
            objectFieldType = new HashMap<String, String>();
            model.put("h1", "Datarefine plugin");
            Boolean cache = (request.getParameter("cache") != null ? Boolean.parseBoolean(request.getParameter("cache")) : false);
            commitOperationsToDB = (request.getParameter("commit-op-to-db") != null ? Boolean.parseBoolean(request.getParameter("commit-op-to-db")) : false);


            //A content folder key has to be set in client, check if this is set first. If it exists but is not a valid key, give warning.
            if ( !Strings.isNullOrEmpty( request.getParameter("content_folder_key") )) {
                Integer content_folder_key = Integer.parseInt(request.getParameter("content_folder_key"));
                model.put("content_folder_key", content_folder_key);
                model.put("cache", cache);

                Element contentFolderEl;
                if (someObjectsCache.containsKey(content_folder_key) && cache) {
                    logger.info("Fetch data from cache..");
                    contentFolderEl = someObjectsCache.get(content_folder_key);
                } else {
                    logger.info("Fetch data from api..");
                    contentFolderEl = getFolderCategoryWithId(content_folder_key, 0, false, true);
                    someObjectsCache.put(content_folder_key, contentFolderEl);
                }

                if (contentFolderEl == null) {
                    model.put("h2", "Please enter category key.");
                    model.put("warning", "Category with key " + content_folder_key + " does not exist!");
                } else {
                    //A valid key is given, check if content folder contains a contenttype. If not give warning.
                    String contentFolderTitle = ((Element) XPath.selectSingleNode(contentFolderEl, "title")).getValue();
                    model.put("contentfoldertitle", contentFolderTitle);
                    model.put("h2", "Category '" + contentFolderTitle + " (" + content_folder_key + ")' selected.");

                    Integer count = Integer.parseInt(contentFolderEl.getAttributeValue("contentcount"));
                    String contentypekey = contentFolderEl.getAttributeValue("contenttypekey");

                    if (count == 0 || contentypekey == null) {
                        model.put("warning", "Category with id " + content_folder_key + " has no content, select another folder!");
                    } else {
                        //A contentfolder with a contentype is selected. get contenttype and append valid contenttype fields to model
                        Document contenttypeDocument;
                        if (cache && contenttypeConfigCache.containsKey(contentypekey)) {
                            contenttypeDocument = contenttypeConfigCache.get(contentypekey);
                        } else {
                            GetContentTypeConfigXMLParams getContentTypeConfigXMLParams = new GetContentTypeConfigXMLParams();
                            getContentTypeConfigXMLParams.key = Integer.parseInt(contentypekey);
                            contenttypeDocument = client.getContentTypeConfigXML(getContentTypeConfigXMLParams);
                            contenttypeConfigCache.put(Integer.parseInt(contentypekey), contenttypeDocument);
                        }

                        List<Element> contenttypeInputFields = XPath.selectNodes(contenttypeDocument.getRootElement(), "//input");
                        Iterator<Element> contenttypeInputFieldsIt = contenttypeInputFields.iterator();
                        while (contenttypeInputFieldsIt.hasNext()) {
                            Element inputField = contenttypeInputFieldsIt.next();
                            String inputFieldName = inputField.getAttribute("name").getValue();
                            String inputFieldType = inputField.getAttribute("type").getValue();
                            //if (("text").equals(inputFieldType) || ("radiobutton").equals(inputFieldType) || ("checkbox").equals(inputFieldType)){ }
                            objectFields.add(inputFieldName);
                            objectFieldType.put(inputFieldName, inputFieldType);
                        }
                        int previewCount = request.getParameter("count") != null ? Integer.parseInt(request.getParameter("count")) : 10;
                        model.put("count", new Integer(previewCount));

                        //Se if any operation are to be performed on existing doctors
                        operations = request.getParameterValues("op");
                        operationsTargets = request.getParameterValues("op-target");

                        replaceInString = request.getParameter("replaceinstringsrc")!=null && request.getParameter("replaceinstringtarget")!=null;
                        if (replaceInString){
                            replaceInStringSrc = request.getParameter("replaceinstringsrc");
                            replaceInStringTarget = request.getParameter("replaceinstringtarget");
                            if (!commitOperationsToDB){
                                model.put("replaceinstringsrc",replaceInStringSrc);
                                model.put("replaceinstringtarget",replaceInStringTarget);
                            }
                        }

                        //Put the operations and operation targets back to model so checkboxes can stay checked on response
                        operationsTargetsList = new ArrayList<String>();
                        if (operationsTargets != null && operationsTargets.length > 0) {
                            operationsTargetsList = Arrays.asList(operationsTargets);
                            model.put("operationsTargets", new HashSet<String>(operationsTargetsList));
                        }
                        if (operations != null && operations.length > 0) {
                            model.put("operations", new HashSet<String>(Arrays.asList(operations)));
                        }

                        //Put the filters back onto the model so checkboxes can stay checked
                        String[] filters = request.getParameterValues("filter");
                        List<String> filtersList = new ArrayList<String>();
                        if (filters != null) {
                            filtersList = Arrays.asList(filters);
                        }
                        model.put("filters", filters);

                        //Get objects from enonic cms. If commitOperationsToDB is selected, get all content - because
                        //all operations always gets applied on all content in a folder.
                        List<Element> objectsElements = getContentByCategory(content_folder_key, commitOperationsToDB ? count : previewCount);

                        Iterator<Element> objectsIterator = objectsElements.iterator();
                        while (objectsIterator.hasNext()) {
                            //Parse through each object and each objects field and put it on the model
                            Element el = objectsIterator.next();

                            anObject = new HashMap<String, String>();
                            aModifiedObject = new HashMap<String, String>();

                            contentKey = el.getAttributeValue("key");
                            contentType = el.getAttributeValue("contenttype");
                            model.put("contenttype", contentType);

                            contentDataInput = new ContentDataInput(contentType);

                            List<Element> elChildren = ((Element) XPath.selectSingleNode(el, "contentdata")).getChildren();
                            Iterator<Element> contentDataChildrenIt = elChildren.iterator();
                            while (contentDataChildrenIt.hasNext()) {
                                handleOneObjectElement(contentDataChildrenIt.next(), "");
                            }

                            anObject.put("contentkey", contentKey);
                            anObject.put("ismodified", "false");
                            someObjects.add(anObject);

                            aModifiedObject.put("contentkey", contentKey);
                            if (aModifiedObject.get("ismodified") == null) {
                                aModifiedObject.put("ismodified", "false");
                            }
                            someModifiedObjects.add(aModifiedObject);
                            Boolean objectIsModified = Boolean.parseBoolean(aModifiedObject.get("ismodified"));

                            if (objectIsModified && commitOperationsToDB != null && commitOperationsToDB) {
                                commitAnObjectToDb(anObject, contentKey, contentType, contentDataInput);

                            }
                        }
                        model.put("h3", "Category has " + count + " content of contenttype '" + model.get("contenttype") + "'");
                        model.put("someObjects", someObjects);

                        //If operations are committed to DB, only show the actual objects on the client
                        if (!commitOperationsToDB) {
                            model.put("someModifiedObjects", someModifiedObjects);
                        }
                        model.put("objectFields", objectFields);
                    }
                }

            } else {
                model.put("h1", "Datarefine plugin");
                model.put("h2", "Please enter category key.");
            }
            model.put("requestUri", request.getRequestURI());
            response.setContentType("text/html");
            this.viewRenderer.render("datarefine.ftl", model, request, response);

        }

    }

    private void handleOneObjectElement(Element element, String parentElementPath) {
        Boolean hasChildElements = false;
        Boolean objectIsModified = false;
        String elementName = (parentElementPath + element.getName()).trim();
        String elementOriginalValue = element.getValue();
        String elementModifiedValue = elementOriginalValue;
        String elementType = objectFieldType.get(elementName);

        //logger.info("handleOneObjectElement name:{} value:{} parentElementPath: {}", elementName, elementModifiedValue, parentElementPath);

        List<Element> elChildrenList = element.getChildren();
        if (elChildrenList != null && elChildrenList.size() > 0) {
            hasChildElements = true;
            //logger.info("Element {} has {} children", elementName, elChildrenList.size());
            Iterator<Element> elChildrenListIt = elChildrenList.iterator();
            while (elChildrenListIt.hasNext()) {
                handleOneObjectElement(elChildrenListIt.next(), elementName + "/");
            }
        } else {
            if (replaceInString || (operations != null && operations.length > 0)) {
                if (operationsTargetsList.contains(elementName)) {
                    //if any operations are selected, perform all operation on all selected operation-targets (input-fields)
                    if (operations!=null){
                        for (String operation : operations) {
                            if ("titlecase".equals(operation)) {
                                elementModifiedValue = WordUtils.capitalizeFully(elementModifiedValue);
                            } else if ("normalizewhitespace".equals(operation)) {
                                elementModifiedValue = StringUtils.normalizeSpace(elementModifiedValue);
                            } else if ("uppercase".equals(operation)) {
                                elementModifiedValue = StringUtils.upperCase(elementModifiedValue);
                            } else if ("lowercase".equals(operation)) {
                                elementModifiedValue = StringUtils.lowerCase(elementModifiedValue);
                            }
                        }
                    }
                    if (replaceInString){
                        elementModifiedValue = elementModifiedValue.replaceAll(replaceInStringSrc, replaceInStringTarget);
                    }

                    if (!elementOriginalValue.equals(elementModifiedValue)) {
                        //only put fields which are actually modified for a modified object on the model
                        objectIsModified = true;
                        aModifiedObject.put(elementName, elementModifiedValue);
                    }
                    logger.info("Element type = " + elementType);
                    logger.info("Original value = " + elementOriginalValue);
                    logger.info("Modified value = " + elementModifiedValue);
                    if (commitOperationsToDB) {
                        if ("text".equals(elementType)){
                            contentDataInput.add(new TextInput(elementName, elementModifiedValue));
                        }else if ("checkbox".equals(elementType)){
                            contentDataInput.add(new BooleanInput(elementName,Boolean.valueOf(elementModifiedValue)));
                        }else if ("radiobutton".equals(elementType)){
                            contentDataInput.add(new SelectorInput(elementName, elementModifiedValue));
                        }
                    }
                }
            }
        }

        if (!hasChildElements) {
            //ignore elements with children, they are not input fields, only ways to group input fields in the contenttype
            if (commitOperationsToDB){
                anObject.put(elementName, elementModifiedValue);
            }else{
                anObject.put(elementName, elementOriginalValue);
            }
            if (objectIsModified) {
                aModifiedObject.put("ismodified", "true");
            }

        }

    }

    private void commitAnObjectToDb(Map<String, String> anObject, String contentKey, String contentType, ContentDataInput contentDataInput) {
        UpdateContentParams updateContentParams = new UpdateContentParams();
        List<Input> inputs = contentDataInput.getInputs();
        StringBuffer changeComment = new StringBuffer();
        changeComment.append(inputs.size() > 1 ? "Fields: " : "Field: ");
        Iterator<Input> inputsIt = inputs.iterator();
        while (inputsIt.hasNext()) {
            changeComment.append("'" + inputsIt.next().getName() + "', ");
        }
        changeComment.append("updated by Datarefine-plugin.");
        updateContentParams.contentData = contentDataInput;
        updateContentParams.contentKey = Integer.parseInt(contentKey);
        updateContentParams.createNewVersion = true;
        updateContentParams.setAsCurrentVersion = true;
        updateContentParams.status = ContentStatus.STATUS_APPROVED;
        updateContentParams.publishFrom = new Date();
        updateContentParams.changeComment = changeComment.toString();
        updateContentParams.updateStrategy = ContentDataInputUpdateStrategy.REPLACE_NEW;
        client.login(Constants.USERNAME, Constants.PASSWORD);
        client.updateContent(updateContentParams);
    }

    private void getContenttype(Integer contenttypekey) {
        GetContentTypeConfigXMLParams params = new GetContentTypeConfigXMLParams();
        params.key = contenttypekey;
        Document document = client.getContentTypeConfigXML(params);
    }

    private List<Element> getContentByCategory(Integer key, Integer count) {
        GetContentByCategoryParams params = getContentByCategoryParams(key, true, 0, 0, count);
        Document document = client.getContentByCategory(params);
        List<Element> content = document.getRootElement().getChildren("content");
        return content;
    }

    private Element getFolderCategoryWithId(Integer content_folder_key, Integer levels, boolean includeContentCount, boolean includeTopCategory) {
        GetCategoriesParams params = getCategoriesParams(content_folder_key, 1, true, true);
        Document document = client.getCategories(params);
        return document.getRootElement().getChild("category");
    }

    private GetCategoriesParams getCategoriesParams(Integer categoryKey, Integer levels, boolean includeContentCount, boolean includeTopCategory) {
        GetCategoriesParams params = new GetCategoriesParams();
        params.categoryKey = categoryKey;
        params.levels = levels;
        params.includeContentCount = includeContentCount;
        params.includeTopCategory = includeTopCategory;
        return params;
    }

    private void serveCssFile(HttpServletResponse response, String requestUri) {
        String css = StringUtils.substringAfterLast(requestUri, "/");
        try {
            Helper.serveCss(css, response);
        } catch (Exception e) {
            logger.warn("Failed to serve css {}", css);
        }
    }

    public static GetContentByCategoryParams getContentByCategoryParams(Integer key, boolean includeData, int parentLevel, int childrenLevel, int count) {
        GetContentByCategoryParams params = new GetContentByCategoryParams();
        params.categoryKeys = new int[]{key};
        params.includeData = includeData;
        params.parentLevel = parentLevel;
        params.childrenLevel = childrenLevel;
        params.count = count;
        return params;
    }
}
