package nl.mallepetrus.jiptv.dto;

public class ApiResponse {

    private String message;
    private boolean success;
    private Object data;

    // Constructors
    public ApiResponse() {}

    public ApiResponse(String message) {
        this.message = message;
        this.success = true;
    }

    public ApiResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

    public ApiResponse(String message, Object data) {
        this.message = message;
        this.success = true;
        this.data = data;
    }

    public ApiResponse(String message, boolean success, Object data) {
        this.message = message;
        this.success = success;
        this.data = data;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    // Static factory methods
    public static ApiResponse success(String message) {
        return new ApiResponse(message, true);
    }

    public static ApiResponse success(String message, Object data) {
        return new ApiResponse(message, true, data);
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(message, false);
    }

    public static ApiResponse error(String message, Object data) {
        return new ApiResponse(message, false, data);
    }
}