package ais.action.master.generic.v2;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Objek data (DTO) hasil satu operasi pada framework CRUD generik ({@code generic/v2}): menandai
 * sukses/gagal, membawa {@code code} ringkas dan {@code message} untuk ditampilkan, data hasil
 * (entitas/daftar) bila sukses, serta peta {@code fieldErrors} (nama field -> pesan galat) untuk
 * menandai kegagalan validasi per kolom formulir. Dibangun lewat method pabrik statis
 * {@link #ok(String, Object)} atau {@link #error(String, String)} agar kombinasi bidang yang
 * konsisten (bukan lewat konstruktor+setter manual).
 */
@SuppressWarnings("rawtypes")
public class GenericCrudResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private String code;
    private String message;
    private Object data;
    private Map fieldErrors = new LinkedHashMap();

    /**
     * @param message pesan sukses untuk ditampilkan ke user
     * @param data    data hasil operasi (entitas tersimpan, daftar, dsb.)
     * @return hasil bertanda sukses dengan {@code code="OK"}
     */
    public static GenericCrudResult ok(String message, Object data) {
        GenericCrudResult result = new GenericCrudResult();
        result.success = true;
        result.code = "OK";
        result.message = message;
        result.data = data;
        return result;
    }

    /**
     * @param code    kode galat ringkas (mis. untuk penanganan khusus di sisi klien)
     * @param message pesan galat untuk ditampilkan ke user
     * @return hasil bertanda gagal, tanpa {@code data}
     */
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
