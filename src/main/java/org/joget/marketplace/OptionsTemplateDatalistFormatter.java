package org.joget.marketplace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.lang.StringUtils;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;
import org.joget.apps.form.model.FormBinder;
import org.joget.apps.form.model.FormLoadBinder;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.plugin.base.PluginManager;
import jakarta.servlet.http.HttpServletRequest;
import org.joget.workflow.util.WorkflowUtil;

public class OptionsTemplateDatalistFormatter extends DataListColumnFormatDefault {

    private static final String MESSAGE_PATH = "messages/OptionsTemplateDatalistFormatter";

    Map<String, String> optionMap = null;

    @Override
    public String getName() {
        return "Options Template Datalist Formatter";
    }

    @Override
    public String getVersion() {
        return Activator.VERSION;
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage("org.joget.marketplace.OptionsTemplateDatalistFormatter.desc", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage("org.joget.marketplace.OptionsTemplateDatalistFormatter.label", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/OptionsTemplateDatalistFormatter.json", null, true, MESSAGE_PATH);
    }

    @Override
    public String format(DataList dataList, DataListColumn column, Object row, Object value) {
        String header = "";
        
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        if (request != null && request.getAttribute(getClassName()) == null) {
            String customHeader = getPropertyString("customHeader");
            
            if (customHeader != null && !customHeader.isEmpty()) {
                header += customHeader;
            }
            
            request.setAttribute(getClassName(), true);
        }

        String template = getPropertyString("template");
        if (template == null || template.isEmpty()) {
            template = "{label}";
        }

        String separator = getPropertyString("separator");
        if (separator == null) {
            separator = "";
        }

        String[] values = value.toString().split(";");
        List<String> results = new ArrayList<String>();
        for (String v : values) {
            v = v.trim();
            if (v.isEmpty()) {
                continue;
            }
            String label = getOptionMap().containsKey(v) ? getOptionMap().get(v) : v;
            String rendered = template.replace("{id}", v).replace("{label}", label);
            
            results.add(rendered);
        }

        return header + StringUtils.join(results, separator);
    }

    protected Map<String, String> getOptionMap() {
        if (optionMap != null) {
            return optionMap;
        }

        optionMap = new ListOrderedMap();

        // load from static "options" grid property
        Object[] options = (Object[]) getProperty(FormUtil.PROPERTY_OPTIONS);
        if (options != null) {
            for (Object o : options) {
                Map option = (HashMap) o;
                Object optValue = option.get(FormUtil.PROPERTY_VALUE);
                Object optLabel = option.get(FormUtil.PROPERTY_LABEL);
                if (optValue != null && optLabel != null) {
                    optionMap.put(optValue.toString(), optLabel.toString());
                }
            }
        }

        // load from binder if configured
        Map optionsBinderProperties = (Map) getProperty("optionsBinder");
        if (optionsBinderProperties != null
                && optionsBinderProperties.get("className") != null
                && !optionsBinderProperties.get("className").toString().isEmpty()) {
            PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
            FormBinder optionBinder = (FormBinder) pluginManager.getPlugin(optionsBinderProperties.get("className").toString());
            if (optionBinder != null) {
                optionBinder.setProperties((Map) optionsBinderProperties.get("properties"));
                FormRowSet rowSet = ((FormLoadBinder) optionBinder).load(null, null, null);
                if (rowSet != null) {
                    optionMap = new ListOrderedMap();
                    for (FormRow formRow : rowSet) {
                        Iterator<String> it = formRow.stringPropertyNames().iterator();
                        String rowValue = formRow.getProperty(FormUtil.PROPERTY_VALUE);
                        if (rowValue == null && it.hasNext()) {
                            rowValue = formRow.getProperty(it.next());
                        }
                        String rowLabel = formRow.getProperty(FormUtil.PROPERTY_LABEL);
                        if (rowLabel == null && it.hasNext()) {
                            rowLabel = formRow.getProperty(it.next());
                        }
                        if (rowValue != null && rowLabel != null) {
                            optionMap.put(rowValue, rowLabel);
                        }
                    }
                }
            }
        }

        return optionMap;
    }
}
