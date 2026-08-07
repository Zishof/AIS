package ais.action.master.generic.v2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.metadata.ClassMetadata;

import ais.common.Common;

/** Dispatcher Java; JSP hanya binding dan forwarding. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class GenericCrudHttpController {
    private GenericCrudHttpController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("X-Content-Type-Options", "nosniff");
        try {
            String entity = attribute(request, "genericCrudEntityKey");
            String module = attribute(request, "genericCrudModuleKey");
            String page = attribute(request, "genericCrudPageKey");
            if (entity == null || module == null || page == null) {
                throw new GenericCrudException(404, "BINDING_REQUIRED", "Endpoint harus dipanggil melalui alias yang terdaftar.");
            }
            GenericCrudFacade facade = new GenericCrudFacade();
            GenericCrudRequestContext context = facade.context(request, entity, module, page);
            String action = value(request.getParameter("action"), "meta");
            if ("export_xlsx".equals(action)) {
                new GenericCrudExportService().writeXlsx(context, request.getParameter("q"), response);
                return;
            }
            Object payload;
            if ("meta".equals(action)) {
                Map meta = facade.metadata(context);
                meta.put("csrf", GenericCrudCsrf.token(request));
                payload = GenericCrudResult.ok("Metadata berhasil dimuat.", meta);
            } else if ("list".equals(action)) {
                payload = GenericCrudResult.ok("Data berhasil dimuat.", facade.list(context,
                        number(request.getParameter("page"), 1), number(request.getParameter("pageSize"), 10),
                        request.getParameter("q"), filters(request), sort(request)));
            } else if ("get".equals(action)) {
                payload = GenericCrudResult.ok("Data berhasil dimuat.", facade.get(context, identifier(context, request.getParameter("id"))));
            } else if ("create".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = facade.create(context, submitted(context, request));
            } else if ("update".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = facade.update(context, identifier(context, request.getParameter("id")), submitted(context, request), request.getParameter("version"));
            } else if ("delete".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = facade.delete(context, identifier(context, request.getParameter("id")));
            } else if ("revisions".equals(action)) {
                payload = GenericCrudResult.ok("Riwayat berhasil dimuat.", facade.revisions(context,
                        identifier(context, request.getParameter("id")), number(request.getParameter("page"), 1),
                        number(request.getParameter("pageSize"), 10)));
            } else {
                throw new GenericCrudException(400, "ACTION_NOT_ALLOWED", "Action tidak terdaftar pada allow-list.");
            }
            writeJson(response, 200, payload);
        } catch (GenericCrudException e) {
            writeJson(response, e.getStatus(), GenericCrudResult.error(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            writeJson(response, 500, GenericCrudResult.error("INTERNAL_ERROR", "Terjadi kesalahan internal. Referensi telah dicatat."));
        }
    }

    private static Serializable identifier(GenericCrudRequestContext context, String raw) throws Exception {
        if (raw == null || raw.trim().length() == 0) throw new GenericCrudException(400, "ID_REQUIRED", "ID wajib disertakan.");
        ClassMetadata metadata = GenericCrudRuntimeMetadataVerifier.verify(context.getDefinition());
        Object id = GenericCrudValueConverter.convert(raw, metadata.getIdentifierType().getReturnedClass());
        if (!(id instanceof Serializable)) throw new GenericCrudException(400, "INVALID_ID", "Tipe ID tidak serializable.");
        return (Serializable) id;
    }

    private static Map submitted(GenericCrudRequestContext context, HttpServletRequest request) {
        Map result = new LinkedHashMap();
        List fields = context.getDefinition().getFields();
        Map parameters = request.getParameterMap();
        for (int i = 0; i < fields.size(); i++) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.get(i);
            String key = field.getProperty();
            if (parameters.containsKey(key)) result.put(key, request.getParameter(key));
        }
        return result;
    }

    private static List filters(HttpServletRequest request) {
        List result = new ArrayList();
        String field = request.getParameter("filterField");
        if (field != null && field.length() > 0) {
            result.add(new GenericCrudFilter(field, value(request.getParameter("filterOperator"), GenericCrudFilter.EQ), request.getParameter("filterValue")));
        }
        return result;
    }

    private static GenericCrudSort sort(HttpServletRequest request) {
        String field = request.getParameter("sort");
        if (field == null || field.length() == 0) return null;
        return new GenericCrudSort(field, !"DESC".equalsIgnoreCase(request.getParameter("direction")));
    }

    private static String attribute(HttpServletRequest request, String key) {
        Object value = request.getAttribute(key);
        return value == null ? null : String.valueOf(value);
    }
    private static int number(String raw, int fallback) { try { return raw == null ? fallback : Integer.parseInt(raw); } catch (Exception e) { return fallback; } }
    private static String value(String text, String fallback) { return text == null || text.length() == 0 ? fallback : text; }
    private static void writeJson(HttpServletResponse response, int status, Object payload) throws java.io.IOException {
        if (response.isCommitted()) return;
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(GenericCrudJson.toJson(payload));
        response.getWriter().flush();
    }
}
