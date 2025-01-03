package com.yourorganization.maven_sample;

import java.util.HashSet;

public class ClassInfo {

    private String className;
    private HashSet<String> fields;

    private HashSet<String> compositions;
    private HashSet<String> aggregations;
    private HashSet<String> associations;
    private HashSet<String> extendTypes;
    private HashSet<String> implementors;
    private HashSet<String> dependency;

    public ClassInfo(String className) {
        this.className = className;
        compositions = new HashSet<>();
        aggregations = new HashSet<>();
        associations = new HashSet<>();
        extendTypes = new HashSet<>();
        implementors = new HashSet<>();
        dependency = new HashSet<>();
    }

    public String getClassName() {
        return className;
    }

    public void AddComposition(String composition) {
        compositions.add(composition);
    }
    public void AddAggregation(String aggregation) {
        aggregations.add(aggregation);
    }
    public void AddAssociation(String association) {
        associations.add(association);
    }

    public void AddExtendType(String extendType) {
        extendTypes.add(extendType);
    }

    public void AddImplementor(String implementor) {
        implementors.add(implementor);
    }

    public void CheckAndAddDependency(String dependencyType) {
        if (aggregations.contains(dependencyType)) {
            return;
        }
        dependency.add(dependencyType);
    }

    public void addField(String field) {
        fields.add(field);
    }

    public HashSet<String> getCompositions() {
        return compositions;
    }

    public HashSet<String> getAggregations() {
        return aggregations;
    }

    public HashSet<String> getAssociations() {
        return associations;
    }

    public HashSet<String> getExtendTypes() {
        return extendTypes;
    }

    public HashSet<String> getImplementors() {
        return implementors;
    }

    public HashSet<String> getFields() {
        return fields;
    }

    public HashSet<String> getDependency() {
        return dependency;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Class: ").append(className).append("\n");
        sb.append("Aggregations: ").append(aggregations.toString()).append("\n");
        sb.append("Associations: ").append(associations).append("\n");
        sb.append("Compositions: ").append(compositions).append("\n");
        sb.append("Dependencies: ").append(dependency).append("\n");
        sb.append("ExtendTypes: ").append(extendTypes).append("\n");
        sb.append("Implementors: ").append(implementors).append("\n");
        return sb.toString();
    }
}
