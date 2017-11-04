package com.walmartlabs.concord.project.model;

import com.walmartlabs.concord.common.ConfigurationUtils;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.form.FormDefinition;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class ProjectDefinitionUtils {

    public static ProcessDefinition getFlow(ProjectDefinition project, Collection<String> activeProfiles, String flowName) {
        Map<String, ProcessDefinition> flows = project.getFlows();
        Map<String, Profile> profiles = project.getProfiles();

        Map<String, ProcessDefinition> view = overlay(flows, profiles, activeProfiles, Profile::getFlows);
        return view.get(flowName);
    }

    public static FormDefinition getForm(ProjectDefinition project, Collection<String> activeProfiles, String formName) {
        Map<String, FormDefinition> forms = project.getForms();
        Map<String, Profile> profiles = project.getProfiles();

        Map<String, FormDefinition> view = overlay(forms, profiles, activeProfiles, Profile::getForms);
        return view.get(formName);
    }

    public static Map<String, Object> getVariables(ProjectDefinition project, Collection<String> activeProfiles) {
        Map<String, Object> variables = project.getConfiguration();
        Map<String, Profile> profiles = project.getProfiles();

        Map<String, Object> view = overlay(variables, profiles, activeProfiles, Profile::getConfiguration,
                ConfigurationUtils::deepMerge);
        return view;
    }

    private static <T> Map<String, T> overlay(Map<String, T> initial,
                                              Map<String, Profile> profiles,
                                              Collection<String> activeProfiles,
                                              Function<Profile, Map<String, T>> selector) {
        return overlay(initial, profiles, activeProfiles, selector, (a, b) -> {
            a.putAll(b);
            return a;
        });
    }

    private static <T> Map<String, T> overlay(Map<String, T> initial,
                                              Map<String, Profile> profiles,
                                              Collection<String> activeProfiles,
                                              Function<Profile, Map<String, T>> selector,
                                              BiFunction<Map<String, T>, Map<String, T>, Map<String, T>> mergeFn) {

        Map<String, T> view = new LinkedHashMap<>(initial != null ? initial : Collections.emptyMap());
        if (profiles == null || activeProfiles == null) {
            return view;
        }

        for (String n : activeProfiles) {
            Profile p = profiles.get(n);
            if (p == null) {
                continue;
            }

            Map<String, T> overlays = selector.apply(p);
            if (overlays != null) {
                view = mergeFn.apply(view, overlays);
            }
        }

        return view;
    }

    private ProjectDefinitionUtils() {
    }
}
