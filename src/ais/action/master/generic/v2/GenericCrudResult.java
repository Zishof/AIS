package ais.action.master.generic.v2;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class GenericCrudResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private String code;
    private String message;
    private Object data;
    private Map fieldErrors = new LinkedHashMap();

    public static GenericCrudResult ok(String message, Object data) {
        GenericCrudResult result = new GenericCrudResult();
        result.success = true;
        result.code = "OK";
        result.message = message;
        result.data = data;
        return result;
    }

    public static GenericCrudResult error(String code, String message) {
        GenericCrudResult result = new GenericCrudResult();
        result.success = false;
        result.code = code;
        result.message = message;
        return result;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public Map getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(Map errors) { fieldErrors = errors == null ? new LinkedHashMap() : errors; }
}
