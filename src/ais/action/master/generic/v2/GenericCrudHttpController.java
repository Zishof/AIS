package ais.action.master.generic.v2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.hibernate.metadata.ClassMetadata;

import ais.common.Common;
import ais.action.master.generic.v2.adapter.GenericCrudPhotoAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudApprovalAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudAttachmentAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudCustomActionProvider;

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
            if ("attachment_download".equals(action)) {
                new GenericCrudAttachmentService().download(context,
                        requiredLong(request.getParameter("attachmentId"), "ATTACHMENT_ID_REQUIRED", "ID lampiran wajib valid."),
                        attachmentAdapter(context), response);
                return;
            }
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
                payload = facade.create(context, submitted(context, request, true));
            } else if ("update".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = facade.update(context, identifier(context, request.getParameter("id")),
                        submitted(context, request, false), request.getParameter("version"));
            } else if ("delete".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = facade.delete(context, identifier(context, request.getParameter("id")));
            } else if ("custom_action".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                if (!(context.getDefinition().getAdapter() instanceof GenericCrudCustomActionProvider))
                    throw new GenericCrudException(403, "CUSTOM_ACTION_DISABLED", "Custom action belum dikonfigurasi.");
                GenericCrudCustomActionProvider customProvider =
                        (GenericCrudCustomActionProvider) context.getDefinition().getAdapter();
                payload = new GenericCrudCustomActionService().execute(context,
                        request.getParameter("actionKey"), identifiers(context, request.getParameterValues("selectedId")),
                        customActionParameters(context, request, customProvider), customProvider);
            } else if ("photo_upload".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                FileItem part = photoPart(request);
                GenericCrudPhotoAdapter adapter = photoAdapter(context);
                String photoUrl = new GenericCrudPhotoService().upload(context,
                        identifier(context, request.getParameter("id")), part.getInputStream(),
                        part.getName(), part.getContentType(), part.getSize(), adapter);
                Map photo = new LinkedHashMap(); photo.put("url", photoUrl);
                payload = GenericCrudResult.ok("Foto mahasiswa berhasil disimpan.", photo);
            } else if ("photo_delete".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                new GenericCrudPhotoService().remove(context,
                        identifier(context, request.getParameter("id")), request.getParameter("reason"),
                        photoAdapter(context));
                payload = GenericCrudResult.ok("Foto mahasiswa berhasil dihapus.", null);
            } else if ("approve".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = new GenericCrudApprovalService().approve(context,
                        identifier(context, request.getParameter("id")), request.getParameter("reason"),
                        approvalAdapter(context));
            } else if ("unapprove".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = new GenericCrudApprovalService().unapprove(context,
                        identifier(context, request.getParameter("id")), request.getParameter("reason"),
                        approvalAdapter(context));
            } else if ("reject".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = new GenericCrudApprovalService().reject(context,
                        identifier(context, request.getParameter("id")), request.getParameter("reason"),
                        approvalAdapter(context));
            } else if ("attachment_list".equals(action)) {
                payload = GenericCrudResult.ok("Lampiran berhasil dimuat.",
                        new GenericCrudAttachmentService().list(context,
                                identifier(context, request.getParameter("id")), attachmentAdapter(context)));
            } else if ("attachment_upload".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                FileItem attachment = attachmentPart(request);
                payload = new GenericCrudAttachmentService().upload(context,
                        identifier(context, request.getParameter("id")), attachment.getName(), attachment.getContentType(),
                        attachment.get(), attachmentAdapter(context));
            } else if ("attachment_delete".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                payload = new GenericCrudAttachmentService().remove(context,
                        requiredLong(request.getParameter("attachmentId"), "ATTACHMENT_ID_REQUIRED", "ID lampiran wajib valid."),
                        attachmentAdapter(context));
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

    private static Map customActionParameters(GenericCrudRequestContext context,
            HttpServletRequest request, GenericCrudCustomActionProvider provider) throws Exception {
        Map result = new LinkedHashMap();
        String key = request.getParameter("actionKey");
        List actions = provider.getActions(context.getDefinition(), context);
        for (int i = 0; actions != null && i < actions.size(); i++) {
            if (!(actions.get(i) instanceof Map)) continue;
            Map action = (Map) actions.get(i);
            if (!String.valueOf(action.get("actionKey")).equals(key)) continue;
            Object names = action.get("parameterNames");
            if (!(names instanceof List)) return result;
            List allowed = (List) names;
            for (int p = 0; p < allowed.size(); p++) {
                String name = String.valueOf(allowed.get(p));
                String value = request.getParameter(name);
                if (value != null) result.put(name, value);
            }
            return result;
        }
        return result;
    }

    private static Serializable identifier(GenericCrudRequestContext context, String raw) throws Exception {
        if (raw == null || raw.trim().length() == 0) throw new GenericCrudException(400, "ID_REQUIRED", "ID wajib disertakan.");
        ClassMetadata metadata = GenericCrudRuntimeMetadataVerifier.verify(context.getDefinition());
        Object id = GenericCrudValueConverter.convert(raw, metadata.getIdentifierType().getReturnedClass());
        if (!(id instanceof Serializable)) throw new GenericCrudException(400, "INVALID_ID", "Tipe ID tidak serializable.");
        return (Serializable) id;
    }

    private static List identifiers(GenericCrudRequestContext context, String[] values) throws Exception {
        List result = new ArrayList(); if (values == null) return result;
        for (int i = 0; i < values.length; i++) result.add(identifier(context, values[i]));
        return result;
    }

    private static GenericCrudPhotoAdapter photoAdapter(GenericCrudRequestContext context) throws GenericCrudException {
        if (context.getDefinition().getAdapter() instanceof GenericCrudPhotoAdapter)
            return (GenericCrudPhotoAdapter) context.getDefinition().getAdapter();
        throw new GenericCrudException(403, "PHOTO_DISABLED", "Photo adapter belum dikonfigurasi.");
    }

    private static GenericCrudApprovalAdapter approvalAdapter(GenericCrudRequestContext context) throws GenericCrudException {
        if (!(context.getDefinition().getAdapter() instanceof GenericCrudApprovalAdapter)) {
            throw new GenericCrudException(403, "APPROVAL_DISABLED", "Approval tidak tersedia untuk entity ini.");
        }
        return (GenericCrudApprovalAdapter) context.getDefinition().getAdapter();
    }

    private static GenericCrudAttachmentAdapter attachmentAdapter(GenericCrudRequestContext context) throws GenericCrudException {
        if (!(context.getDefinition().getAdapter() instanceof GenericCrudAttachmentAdapter)) {
            throw new GenericCrudException(403, "ATTACHMENT_DISABLED", "Lampiran tidak tersedia untuk entity ini.");
        }
        return (GenericCrudAttachmentAdapter) context.getDefinition().getAdapter();
    }

    private static FileItem attachmentPart(HttpServletRequest request) throws Exception {
        if (!ServletFileUpload.isMultipartContent(request))
            throw new GenericCrudException(400, "ATTACHMENT_REQUIRED", "Berkas lampiran wajib dipilih.");
        ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
        upload.setFileSizeMax(20L * 1024L * 1024L); upload.setSizeMax(21L * 1024L * 1024L);
        List parts = upload.parseRequest(request);
        for (int i = 0; i < parts.size(); i++) {
            FileItem item = (FileItem) parts.get(i);
            if (!item.isFormField() && "attachment".equals(item.getFieldName()) && item.getSize() > 0) return item;
        }
        throw new GenericCrudException(400, "ATTACHMENT_REQUIRED", "Berkas lampiran wajib dipilih.");
    }

    private static FileItem photoPart(HttpServletRequest request) throws Exception {
        if (!ServletFileUpload.isMultipartContent(request))
            throw new GenericCrudException(400, "PHOTO_REQUIRED", "Berkas foto wajib dipilih.");
        ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
        upload.setFileSizeMax(5L * 1024L * 1024L); upload.setSizeMax(6L * 1024L * 1024L);
        List parts = upload.parseRequest(request);
        for (int i = 0; i < parts.size(); i++) {
            FileItem item = (FileItem) parts.get(i);
            if (!item.isFormField() && "photo".equals(item.getFieldName()) && item.getSize() > 0) return item;
        }
        throw new GenericCrudException(400, "PHOTO_REQUIRED", "Berkas foto wajib dipilih.");
    }

    private static Map submitted(GenericCrudRequestContext context, HttpServletRequest request,
            boolean create) {
        return GenericCrudSubmittedValues.fromParameters(context.getDefinition(),
                request.getParameterMap(), create);
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
