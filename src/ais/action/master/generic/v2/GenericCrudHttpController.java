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
                new GenericCrudExportService().writeXlsx(context, request.getParameter("q"), filters(request), sort(request), response);
                return;
            }
            if ("export_pdf".equals(action) || "export_docx".equals(action) || "export_pptx".equals(action)) {
                new GenericCrudDocumentExportService().write(context, action.substring("export_".length()), request.getParameter("q"), filters(request), sort(request), response);
                return;
            }
            if ("import_template".equals(action)) {
                new GenericCrudImportService().writeTemplate(context, response);
                return;
            }
            if ("import_errors".equals(action)) {
                new GenericCrudImportService().writeErrors(context, request.getParameter("jobKey"), response);
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
            } else if ("global_revisions".equals(action)) {
                payload = GenericCrudResult.ok("Audit global berhasil dimuat.", facade.globalRevisions(context,
                        number(request.getParameter("page"), 1), number(request.getParameter("pageSize"), 10)));
            } else if ("compare".equals(action)) {
                payload = GenericCrudResult.ok("Perbandingan revisi berhasil dimuat.", facade.compare(context,
                        identifier(context, request.getParameter("id")), requiredNumber(request.getParameter("left")),
                        requiredNumber(request.getParameter("right"))));
            } else if ("restore_field".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = new GenericCrudRestoreService().restoreField(context,
                        identifier(context, request.getParameter("id")), requiredNumber(request.getParameter("revision")),
                        request.getParameter("property"), request.getParameter("version"), request.getParameter("reason"));
            } else if ("restore_revision".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = new GenericCrudRestoreService().restoreRevision(context,
                        identifier(context, request.getParameter("id")), requiredNumber(request.getParameter("revision")),
                        "true".equalsIgnoreCase(request.getParameter("deep")), request.getParameter("version"),
                        request.getParameter("reason"));
            } else if ("admin_delete_preflight".equals(action)) {
                payload = GenericCrudResult.ok("Preflight penghapusan berhasil dimuat.",
                        new GenericCrudPermanentDeleteService().preflight(context, identifier(context, request.getParameter("id"))));
            } else if ("admin_delete_confirm".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = new GenericCrudPermanentDeleteService().deleteActiveRow(context,
                        identifier(context, request.getParameter("id")), request.getParameter("typedConfirmation"),
                        request.getParameter("reason"));
            } else if ("lookup".equals(action)) {
                payload = new GenericCrudLookupService().lookup(context, request.getParameter("q"),
                        number(request.getParameter("page"), 1), number(request.getParameter("pageSize"), 20));
            } else if ("relation_lookup".equals(action)) {
                payload = new GenericCrudRelationLookupService().lookup(context, request.getParameter("field"),
                        request.getParameter("q"), number(request.getParameter("page"), 1),
                        number(request.getParameter("pageSize"), 30));
            } else if ("preference_load".equals(action)) {
                payload = GenericCrudResult.ok("Preferensi dimuat.", new GenericCrudColumnPreferenceService().load(context));
            } else if ("preference_save".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = new GenericCrudColumnPreferenceService().save(context, jsonMap(request.getParameter("preferenceJson")));
            } else if ("preference_reset".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = new GenericCrudColumnPreferenceService().reset(context);
            } else if ("saved_view_list".equals(action)) {
                payload = GenericCrudResult.ok("Saved view dimuat.", new GenericCrudSavedViewService().list(context));
            } else if ("saved_view_save".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = new GenericCrudSavedViewService().save(context, request.getParameter("name"), request.getParameter("description"), jsonMap(request.getParameter("viewJson")));
            } else if ("saved_view_delete".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = new GenericCrudSavedViewService().remove(context, requiredLong(request.getParameter("viewId"),
                        "VIEW_ID_REQUIRED", "ID saved view wajib valid."));
            } else if ("import_preview".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = new GenericCrudImportService().preview(context, request.getParameter("fileName"), request.getParameter("fileData"));
            } else if ("import_confirm".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = new GenericCrudImportService().confirm(context, request.getParameter("jobKey"));
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
        String[] fields = request.getParameterValues("filterField");
        String[] operators = request.getParameterValues("filterOperator");
        String[] values = request.getParameterValues("filterValue");
        if (fields != null) {
            for (int i = 0; i < fields.length && i < 20; i++) {
                if (fields[i] == null || fields[i].length() == 0) continue;
                String operator = operators != null && i < operators.length ? operators[i] : GenericCrudFilter.EQ;
                String filterValue = values != null && i < values.length ? values[i] : null;
                result.add(new GenericCrudFilter(fields[i], operator, filterValue));
            }
        }
        return result;
    }

    private static GenericCrudSort sort(HttpServletRequest request) {
        String field = request.getParameter("sort");
        if (field == null || field.length() == 0) return null;
        String direction = value(request.getParameter("direction"), "ASC");
        if (!("ASC".equalsIgnoreCase(direction) || "DESC".equalsIgnoreCase(direction))) return new GenericCrudSort("__invalid_direction__", true);
        return new GenericCrudSort(field, "ASC".equalsIgnoreCase(direction));
    }

    private static String attribute(HttpServletRequest request, String key) {
        Object value = request.getAttribute(key);
        return value == null ? null : String.valueOf(value);
    }
    private static int number(String raw, int fallback) { try { return raw == null ? fallback : Integer.parseInt(raw); } catch (Exception e) { return fallback; } }
    private static Number requiredNumber(String raw) throws GenericCrudException { try { return Long.valueOf(raw); } catch (Exception e) { throw new GenericCrudException(400, "REVISION_REQUIRED", "Nomor revisi wajib valid."); } }
    private static Long requiredLong(String raw, String code, String message) throws GenericCrudException { try { return Long.valueOf(raw); } catch (Exception e) { throw new GenericCrudException(400, code, message); } }
    private static String value(String text, String fallback) { return text == null || text.length() == 0 ? fallback : text; }
    private static Map jsonMap(String json) throws GenericCrudException { try { Object value = GenericCrudJson.fromJson(json == null ? "{}" : json, Map.class); if (value instanceof Map) return (Map) value; } catch (Exception ignored) { } throw new GenericCrudException(400, "JSON_INVALID", "Payload konfigurasi tidak valid."); }
    private static void writeJson(HttpServletResponse response, int status, Object payload) throws java.io.IOException {
        if (response.isCommitted()) return;
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(GenericCrudJson.toJson(payload));
        response.getWriter().flush();
    }
}
