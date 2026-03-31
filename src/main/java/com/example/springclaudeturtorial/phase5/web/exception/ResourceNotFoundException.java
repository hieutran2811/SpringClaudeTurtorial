package com.example.springclaudeturtorial.phase5.web.exception;

/**
 * Ném ra khi không tìm thấy resource theo ID.
 * Sẽ được map → HTTP 404 bởi GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super("%s not found with id: %s".formatted(resourceName, resourceId));
        this.resourceName = resourceName;
        this.resourceId   = resourceId;
    }

    public String getResourceName() { return resourceName; }
    public Object getResourceId()   { return resourceId; }
}
