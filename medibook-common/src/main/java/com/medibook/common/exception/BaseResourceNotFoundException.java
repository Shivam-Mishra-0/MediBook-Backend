package com.medibook.common.exception;

public class BaseResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final transient Object fieldValue;

    public BaseResourceNotFoundException(
            String resourceName,
            String fieldName,
            Object fieldValue) {

        super(String.format(
                "%s not found with %s: %s",
                resourceName,
                fieldName,
                fieldValue
        ));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}
