<html doctype="html">
<head>
    <title>Datarefine plugin</title>
    <link rel="stylesheet" href="http://yui.yahooapis.com/pure/0.3.0/pure-min.css">
    <link rel="stylesheet" type="text/css" href="${requestUri}/datarefine-plugin/datarefine.css"/>
</head>
<body>
    <header>
        <#if h1??><h1>${h1}</h1></#if>
        <#if h2??><h2>${h2}</h2></#if>
        <#if h3??><h3>${h3}</h3></#if>
        <#if h4??><h4>${h4}</h4></#if>
        <#if h5??><h5>${h5}</h5></#if>
        <#if warning??><button class="pure-button pure-button-primary">${warning}</button></#if>
    </header>
    <section>
         <form class="pure-form pure-form-stacked" name="datarefine-configuration-form" action="${requestUri}" method="post">
            <fieldset>
                <legend>Configuration form</legend>
                <label for="key">Category key:</label>
                <input required  name="content_folder_key" id="key" type="number" min="0" step="1" pattern="\d+" value="<#if content_folder_key??>${content_folder_key}</#if>"/>
                <#if someObjects??>
                    <label for="count">Count:</label>
                    <input name="count" id="count"  type="number" min="0" max="99000" step="1" pattern="\d+" value="<#if count??>${count?c}</#if>"/>
                </#if>
             </fieldset>
            <#if someObjects??>
                 <fieldset>
                     <legend>Retrieve data from cache / database</legend>
                     <label for="cache-true" class="pure-radio">
                         <input id="cache-true" type="radio" name="cache" value="true" checked>
                         Use cached data (faster).
                     </label>
                     <label for="cache-false" class="pure-radio">
                         <input id="cache-false" type="radio" name="cache" value="false">
                         Fetch data from api (if data has changed on server)
                     </label>
                 </fieldset>
                <fieldset>
                     <legend>Commit changes to database</legend>
                     <label for="commit-op-to-db-false" class="pure-radio">
                         <input id="commit-op-to-db-false" type="radio" name="commit-op-to-db" value="false" checked>
                         No, only preview result in browser.
                     </label>

                     <label for="commit-op-to-db-true" class="pure-radio">
                         <input id="commit-op-to-db-true" type="radio" name="commit-op-to-db" value="true">
                         Yes, commit changes to database.
                     </label>
                 </fieldset>
                <fieldset>
                    <legend>Operations</legend>
                    <label for="op-nocase" class="pure-radio">
                        <input id="op-nocase" name="op" type="radio" value="nocase" ${(operations?? && operations?seq_contains("titlecase"))?string("checked","")}>
                        Do not perform any case operations
                    </label>
                    <label for="op-titlecase" class="pure-radio">
                        <input id="op-titlecase" name="op" type="radio" value="titlecase" ${(operations?? && operations?seq_contains("titlecase"))?string("checked","")}>
                         Convert names to Full Title Case
                    </label>
                    <label for="op-uppercase" class="pure-radio">
                        <input id="op-uppercase" name="op" type="radio" value="uppercase" ${(operations?? && operations?seq_contains("uppercase"))?string("checked","")}>
                        Convert names to uppercase
                    </label>
                    <label for="op-lowercase" class="pure-radio">
                        <input id="op-lowercase" name="op" type="radio" value="lowercase" ${(operations?? && operations?seq_contains("lowercase"))?string("checked","")}>
                        Convert names to lowercase
                    </label>
                    <label for="op-normalizewhitespace" class="pure-checkbox">
                        <input id="op-normalizewhitespace" name="op" type="checkbox" value="normalizewhitespace" ${(operations?? && operations?seq_contains("normalizewhitespace"))?string("checked","")}>
                        Normalize whitespace
                    </label>
                </fieldset>
                <fieldset class="pure-group">
                    <legend>Replace in string</legend>
                    <input placeholder="Source string" class="pure-input-1-6" id="replaceinstringsrc" name="replaceinstringsrc" type="text" value="<#if replaceinstringsrc??>${replaceinstringsrc}</#if>">
                    <input placeholder="Target string" class="pure-input-1-6" id="replaceinstringtarget" name="replaceinstringtarget" type="text" value="<#if replaceinstringtarget??>${replaceinstringtarget}</#if>">
                </fieldset>
                <fieldset>
                    <legend>Operations targets</legend>
                    <div class="pure-g-r">
                        <#list objectFields as objectField>
                            <div class="pure-u-1-6">
                                <label for="op-target-${objectField}" class="pure-checkbox">
                                    <input id="op-target-${objectField}" name="op-target" type="checkbox" value="${objectField}" ${(operationsTargets?? && operationsTargets?seq_contains("${objectField}"))?string("checked","")}>
                                    ${objectField}
                                </label>
                            </div>
                        </#list>
                    </div>
                </fieldset>
                <fieldsset>
                    <legend>Filters</legend>
                    <label for="filter-display-modified-rows-only" class="pure-checkbox">
                        <input id="filter-display-modified-rows-only" name="filter" value="display-modified-rows-only" type="checkbox" ${(filters?? && filters?seq_contains("display-modified-rows-only"))?string("checked","")}>
                        Display modified rows only
                    </label>
                </fieldsset>
             </#if>
             <button type="submit" class="pure-button pure-button-primary">Apply</button>
        </form>
    </section>
    <#if someObjects?? && count??>
        <section class="someObjects">
            <h1>Displaying up to ${count} instances of contenttype '${contenttype}' from contentfolder '${contentfoldertitle}'</h1>
            <table class="pure-table pure-table-horizontal pure-table-striped">
                <thead>
                    <th>#</th>
                    <#list objectFields as objectField>
                            <th>${objectField}</th>
                    </#list>
                </thead>
                <#assign objectCounter = 0>
                <#list someObjects as someObject>
                    <#assign isModified = "false"/>
                    <#if someModifiedObjects??>
                        <#assign isModified = someModifiedObjects[objectCounter]["ismodified"]/>
                    </#if>
                    <#if filters?? && filters?seq_contains("display-modified-rows-only") && isModified="false">

                    <#else>
                        <#assign objectKey = someObject["contentkey"]/>
                        <tr class="someObject">
                            <td>
                                ${objectCounter}
                            </td>
                            <#list objectFields as objectField>
                                <#if someObject[objectField]??>
                                    <td class="<#if someModifiedObjects?? && someModifiedObjects[objectCounter][objectField]?? && someModifiedObjects[objectCounter][objectField]!=someObject[objectField]>modifiedValue</#if>" id="${contenttype}-${objectKey}-${objectField}">${someObject[objectField]}</td>
                                <#else>
                                    <td/>
                                </#if>
                            </#list>
                        </tr>
                        <#if someModifiedObjects??>
                            <#assign someModifiedObject = someModifiedObjects[objectCounter]/>
                            <#if someModifiedObject["ismodified"]="true">
                                <tr class="someModifiedObject">
                                    <td>${objectCounter}</td>
                                    <#list objectFields as objectField>
                                        <#if someModifiedObject[objectField]??>
                                             <td class="<#if someModifiedObject[objectField]!=someObject[objectField]>modifiedValue</#if>" id="mod-${contenttype}-${objectKey}-${objectField}">
                                                ${someModifiedObject[objectField]}
                                                <#if someModifiedObject[objectField]!=someObject[objectField]> (+)</#if>
                                             </td>
                                        <#else>
                                            <td/>
                                        </#if>
                                    </#list>
                                </tr>
                            </#if>
                        </#if>
                    </#if>
                    <#assign objectCounter = objectCounter+1>
                </#list>
            </table>
        </section>
    </#if>

</body>
</html>