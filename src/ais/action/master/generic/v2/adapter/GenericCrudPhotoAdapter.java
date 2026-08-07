package ais.action.master.generic.v2.adapter;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudRequestContext;

@SuppressWarnings("rawtypes")
public interface GenericCrudPhotoAdapter {
    Map validate(String fileName, String contentType, long length, GenericCrudRequestContext context) throws Exception;
    String store(Serializable id, InputStream input, String fileName, String contentType, GenericCrudRequestContext context) throws Exception;
    void remove(Serializable id, String reason, GenericCrudRequestContext context) throws Exception;
}
