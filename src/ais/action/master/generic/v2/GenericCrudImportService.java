package ais.action.master.generic.v2;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.metadata.ClassMetadata;

/** XLSX dry-run -> explicit confirmation. Job terikat session/user dan memiliki expiry. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GenericCrudImportService {
    private static final String JOB_PREFIX = "genericCrud.import.";
    private static final String HASH_PREFIX = "genericCrud.importHash.";
    private static final long TTL = 30L * 60L * 1000L;
    private final GenericCrudMutationService mutation = new GenericCrudMutationService();

    public void writeTemplate(GenericCrudRequestContext context, HttpServletResponse response) throws Exception {
        requireImport(context);
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            XSSFSheet data = workbook.createSheet("Data");
            Row header = data.createRow(0);
            List fields = importFields(context.getDefinition());
            header.createCell(0).setCellValue(context.getDefinition().getIdentifierProperty());
            for (int i = 0; i < fields.size(); i++) header.createCell(i + 1).setCellValue(((GenericCrudFieldDefinition) fields.get(i)).getProperty());
            header.createCell(fields.size() + 1).setCellValue("delete");
            XSSFSheet help = workbook.createSheet("Petunjuk");
            help.createRow(0).createCell(0).setCellValue("Jangan mengubah nama header. ID kosong = CREATE; ID terisi = UPDATE; delete=TRUE = soft delete.");
            help.createRow(1).createCell(0).setCellValue("Upload selalu dry-run. Tidak ada mutasi sebelum tombol konfirmasi dijalankan.");
            help.createRow(2).createCell(0).setCellValue("Field internal, collection, blob, dan sensitif tidak tersedia pada template.");
            for (int i = 0; i <= fields.size() + 1; i++) data.autoSizeColumn(i);
            response.reset();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=template_" + context.getDefinition().getPageKey() + ".xlsx");
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        } finally { try { workbook.close(); } catch (Exception ignored) { } }
    }

    public GenericCrudResult preview(GenericCrudRequestContext context, String fileName, String encoded) throws Exception {
        requireImport(context);
        if (fileName == null || !fileName.toLowerCase().endsWith(".xlsx")) throw new GenericCrudException(400, "IMPORT_FILE_TYPE", "File harus berformat XLSX.");
        if (encoded == null || encoded.length() == 0) throw new GenericCrudException(400, "IMPORT_FILE_EMPTY", "File upload kosong.");
        int comma = encoded.indexOf(',');
        if (comma >= 0) encoded = encoded.substring(comma + 1);
        byte[] bytes;
        try { bytes = DatatypeConverter.parseBase64Binary(encoded); }
        catch (Exception invalid) { throw new GenericCrudException(400, "IMPORT_FILE_CORRUPT", "Base64 file tidak valid."); }
        if (bytes.length > 10 * 1024 * 1024) throw new GenericCrudException(413, "IMPORT_FILE_OVERSIZE", "Ukuran XLSX maksimal 10 MB.");
        String hash = sha256(bytes);
        HttpSession httpSession = context.getRequest().getSession(true);
        if (httpSession.getAttribute(HASH_PREFIX + hash) != null) throw new GenericCrudException(409, "IMPORT_DUPLICATE_FILE", "File yang sama sudah pernah dikonfirmasi pada sesi ini.");

        ImportJob job = parse(context, bytes, hash);
        httpSession.setAttribute(JOB_PREFIX + job.getJobKey(), job);
        return GenericCrudResult.ok("Dry-run selesai. Periksa ringkasan sebelum konfirmasi.", job.summary());
    }

    public GenericCrudResult confirm(GenericCrudRequestContext context, String jobKey) throws Exception {
        requireImport(context);
        ImportJob job = getJob(context, jobKey);
        if (!"PREVIEW_READY".equals(job.getStatus())) throw new GenericCrudException(409, "IMPORT_JOB_STATE", "Job tidak berada pada status siap konfirmasi.");
        if (job.getExpiresAt() < System.currentTimeMillis()) throw new GenericCrudException(410, "IMPORT_JOB_EXPIRED", "Preview import sudah kedaluwarsa.");
        job.setStatus("RUNNING");
        job.createRows = 0; job.updateRows = 0; job.deleteRows = 0; job.skipRows = 0; job.processedRows = 0;
        ClassMetadata metadata = GenericCrudRuntimeMetadataVerifier.verify(context.getDefinition());
        List rows = job.getRows();
        for (int i = 0; i < rows.size(); i++) {
            Map row = (Map) rows.get(i);
            if (row.get("_error") != null) { job.skipRows++; continue; }
            String operation = String.valueOf(row.remove("_operation"));
            String rawId = (String) row.remove("_id");
            try {
                GenericCrudResult result;
                if ("CREATE".equals(operation)) result = mutation.create(context, row);
                else {
                    Serializable id = (Serializable) GenericCrudValueConverter.convert(rawId, metadata.getIdentifierType().getReturnedClass());
                    if ("DELETE".equals(operation)) result = mutation.softDelete(context, id);
                    else result = mutation.update(context, id, row, null);
                }
                if (!result.isSuccess()) throw new IllegalArgumentException(result.getMessage());
                if ("CREATE".equals(operation)) job.createRows++;
                else if ("UPDATE".equals(operation)) job.updateRows++;
                else job.deleteRows++;
            } catch (Exception error) {
                Map detail = new LinkedHashMap();
                detail.put("row", Integer.valueOf(i + 2));
                detail.put("message", safeMessage(error));
                job.getErrors().add(detail);
                job.errorRows++;
            }
            job.processedRows++;
        }
        job.setStatus(job.errorRows > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED");
        context.getRequest().getSession(true).setAttribute(HASH_PREFIX + job.getFileHash(), new Date());
        return GenericCrudResult.ok("Import selesai.", job.summary());
    }

    public void writeErrors(GenericCrudRequestContext context, String jobKey, HttpServletResponse response) throws Exception {
        requireImport(context);
        ImportJob job = getJob(context, jobKey);
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            XSSFSheet sheet = workbook.createSheet("Errors");
            Row head = sheet.createRow(0); head.createCell(0).setCellValue("row"); head.createCell(1).setCellValue("message");
            for (int i = 0; i < job.getErrors().size(); i++) {
                Map error = (Map) job.getErrors().get(i); Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(String.valueOf(error.get("row")));
                row.createCell(1).setCellValue(String.valueOf(error.get("message")));
            }
            sheet.autoSizeColumn(0); sheet.autoSizeColumn(1);
            response.reset(); response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=import_errors_" + jobKey + ".xlsx");
            workbook.write(response.getOutputStream()); response.getOutputStream().flush();
        } finally { try { workbook.close(); } catch (Exception ignored) { } }
    }

    private ImportJob parse(GenericCrudRequestContext context, byte[] bytes, String hash) throws Exception {
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));
            XSSFSheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null || sheet.getRow(0) == null) throw new GenericCrudException(400, "IMPORT_HEADER_MISSING", "Header XLSX tidak ditemukan.");
            List allowed = importFields(context.getDefinition());
            Map allowedMap = new LinkedHashMap();
            for (int i = 0; i < allowed.size(); i++) allowedMap.put(((GenericCrudFieldDefinition) allowed.get(i)).getProperty(), allowed.get(i));
            Row header = sheet.getRow(0); List columns = new ArrayList(); Set seen = new HashSet();
            DataFormatter formatter = new DataFormatter();
            for (int c = 0; c < header.getLastCellNum(); c++) {
                String name = formatter.formatCellValue(header.getCell(c)).trim();
                if (!(context.getDefinition().getIdentifierProperty().equals(name) || "delete".equalsIgnoreCase(name) || allowedMap.containsKey(name))) {
                    throw new GenericCrudException(400, "IMPORT_HEADER_INVALID", "Header tidak diizinkan: " + name);
                }
                if (!seen.add(name.toLowerCase())) throw new GenericCrudException(400, "IMPORT_HEADER_DUPLICATE", "Header duplikat: " + name);
                columns.add(name);
            }
            if (sheet.getLastRowNum() > context.getDefinition().getMaxImportRows()) throw new GenericCrudException(413, "IMPORT_ROW_LIMIT", "Jumlah baris melewati batas entity.");
            ImportJob job = new ImportJob(UUID.randomUUID().toString().replace("-", ""), hash,
                    GenericCrudColumnPreferenceService.userKey(context),
                    GenericCrudColumnPreferenceService.roleKey(context), context.getDefinition().getEntityKey());
            Set naturalKeys = new HashSet();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row excel = sheet.getRow(r); if (excel == null) continue;
                Map values = new LinkedHashMap(); String id = null; boolean delete = false; boolean nonEmpty = false;
                for (int c = 0; c < columns.size(); c++) {
                    String name = String.valueOf(columns.get(c)); String text = formatter.formatCellValue(excel.getCell(c)).trim();
                    if (text.length() > 0) nonEmpty = true;
                    if (context.getDefinition().getIdentifierProperty().equals(name)) id = text;
                    else if ("delete".equalsIgnoreCase(name)) delete = "true".equalsIgnoreCase(text) || "1".equals(text) || "ya".equalsIgnoreCase(text);
                    else if (text.length() > 0 || ((GenericCrudFieldDefinition) allowedMap.get(name)).getJavaType().equals(String.class.getName())) values.put(name, text);
                }
                if (!nonEmpty) continue;
                String operation = delete ? "DELETE" : (id == null || id.length() == 0 ? "CREATE" : "UPDATE");
                String error = validateRow(context, operation, id, values, allowedMap, naturalKeys);
                values.put("_operation", operation); values.put("_id", id);
                if (error != null) { values.put("_error", error); Map detail = new LinkedHashMap(); detail.put("row", Integer.valueOf(r + 1)); detail.put("message", error); job.errors.add(detail); job.errorRows++; }
                job.rows.add(values);
                if ("CREATE".equals(operation)) job.createRows++;
                else if ("UPDATE".equals(operation)) job.updateRows++;
                else if ("DELETE".equals(operation)) job.deleteRows++;
            }
            job.totalRows = job.rows.size(); job.status = "PREVIEW_READY";
            return job;
        } catch (GenericCrudException e) { throw e; }
        catch (Exception corrupt) { throw new GenericCrudException(400, "IMPORT_FILE_CORRUPT", "XLSX tidak dapat dibaca.", corrupt); }
        finally { try { if (workbook != null) workbook.close(); } catch (Exception ignored) { } }
    }

    private String validateRow(GenericCrudRequestContext context, String operation, String id, Map values,
            Map allowed, Set naturalKeys) {
        if ("DELETE".equals(operation)) {
            if (!context.getDefinition().isImportDeleteEnabled()) return "Import delete belum diaktifkan.";
            if (!context.isCanDelete()) return "Privilege DELETE tidak diberikan.";
            if (id == null || id.length() == 0) return "DELETE memerlukan ID.";
            return null;
        }
        if ("CREATE".equals(operation) && !context.isCanCreate()) return "Privilege CREATE tidak diberikan.";
        if ("UPDATE".equals(operation) && !context.isCanUpdate()) return "Privilege UPDATE tidak diberikan.";
        List fields = context.getDefinition().getFields();
        for (int i = 0; i < fields.size(); i++) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.get(i);
            if (field.isRequired() && "CREATE".equals(operation)) {
                Object value = values.get(field.getProperty()); if (value == null || String.valueOf(value).trim().length() == 0) return field.getLabel() + " wajib diisi.";
            }
            if (values.containsKey(field.getProperty())) {
                if ("CREATE".equals(operation) && !field.isCreateable()) return "Field tidak createable: " + field.getProperty();
                if ("UPDATE".equals(operation) && !field.isUpdateable()) return "Field tidak updateable: " + field.getProperty();
                try { GenericCrudValueConverter.convert(values.get(field.getProperty()), ClassForName.safe(field.getJavaType())); }
                catch (Exception invalid) { return "Tipe tidak valid untuk " + field.getProperty(); }
            }
        }
        List keys = context.getDefinition().getAdapter().getNaturalKeyProperties();
        if (keys != null && !keys.isEmpty()) {
            StringBuilder key = new StringBuilder(); boolean complete = true;
            for (int i = 0; i < keys.size(); i++) { Object value = values.get(String.valueOf(keys.get(i))); if (value == null || String.valueOf(value).length() == 0) complete = false; key.append('|').append(value); }
            if (complete && !naturalKeys.add(key.toString().toLowerCase())) return "Natural key duplikat di dalam file.";
        }
        return null;
    }

    private List importFields(GenericCrudDefinition definition) {
        List result = new ArrayList(); List fields = definition.getFields();
        for (int i = 0; i < fields.size(); i++) { GenericCrudFieldDefinition f = (GenericCrudFieldDefinition) fields.get(i); if (!f.isSensitive() && (f.isCreateable() || f.isUpdateable())) result.add(f); }
        return result;
    }
    private void requireImport(GenericCrudRequestContext context) throws GenericCrudException {
        if (!context.getDefinition().isImportEnabled()) throw new GenericCrudException(403, "IMPORT_DISABLED", "Import belum diaktifkan untuk entity ini.");
        if (!(context.isCanCreate() && context.isCanUpdate() && context.isCanDelete())) {
            throw new GenericCrudException(403, "IMPORT_PRIVILEGE_DENIED", "Import memerlukan CREATE, UPDATE, dan DELETE.");
        }
    }
    private ImportJob getJob(GenericCrudRequestContext context, String key) throws GenericCrudException {
        Object value = context.getRequest().getSession(false) == null ? null : context.getRequest().getSession(false).getAttribute(JOB_PREFIX + key);
        if (!(value instanceof ImportJob)) throw new GenericCrudException(404, "IMPORT_JOB_NOT_FOUND", "Job import tidak ditemukan pada sesi aktif.");
        ImportJob job = (ImportJob) value;
        if (!job.matches(context)) throw new GenericCrudException(403, "IMPORT_JOB_OWNER_DENIED", "Job import bukan milik user, role, atau entity aktif.");
        if (job.getExpiresAt() < System.currentTimeMillis()) throw new GenericCrudException(410, "IMPORT_JOB_EXPIRED", "Job import sudah kedaluwarsa.");
        return job;
    }
    private String sha256(byte[] bytes) throws Exception { byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes); return DatatypeConverter.printHexBinary(digest).toLowerCase(); }
    private String safeMessage(Exception error) { String value = error.getMessage(); return value == null || value.length() == 0 ? error.getClass().getSimpleName() : value; }

    /** Java 7 helper: hanya class field dari registry, tidak pernah class dari request. */
    private static final class ClassForName { static Class safe(String name) throws Exception {
        if ("int".equals(name)) return Integer.TYPE; if ("long".equals(name)) return Long.TYPE;
        if ("boolean".equals(name)) return Boolean.TYPE; if ("double".equals(name)) return Double.TYPE;
        if ("float".equals(name)) return Float.TYPE; if ("short".equals(name)) return Short.TYPE;
        return Class.forName(name);
    } }

    public static class ImportJob implements Serializable {
        private static final long serialVersionUID = 1L;
        private String jobKey, fileHash, ownerUserKey, ownerRoleKey, entityKey, status = "PREVIEWING";
        private long createdAt = System.currentTimeMillis(), expiresAt = createdAt + TTL;
        private List rows = new ArrayList(), errors = new ArrayList();
        private int totalRows, createRows, updateRows, deleteRows, skipRows, errorRows, processedRows;
        ImportJob(String key, String hash, String user, String role, String entity) {
            jobKey = key; fileHash = hash; ownerUserKey = user; ownerRoleKey = role; entityKey = entity;
        }
        public Map summary() { Map m = new LinkedHashMap(); m.put("jobKey", jobKey); m.put("status", status); m.put("totalRows", Integer.valueOf(totalRows)); m.put("createRows", Integer.valueOf(createRows)); m.put("updateRows", Integer.valueOf(updateRows)); m.put("deleteRows", Integer.valueOf(deleteRows)); m.put("skipRows", Integer.valueOf(skipRows)); m.put("errorRows", Integer.valueOf(errorRows)); m.put("processedRows", Integer.valueOf(processedRows)); m.put("expiresAt", Long.valueOf(expiresAt)); m.put("errors", errors); return m; }
        public String getJobKey() { return jobKey; } public String getFileHash() { return fileHash; } public String getStatus() { return status; } public void setStatus(String value) { status = value; }
        public long getExpiresAt() { return expiresAt; } public List getRows() { return rows; } public List getErrors() { return errors; }
        public boolean matches(GenericCrudRequestContext context) {
            return ownerUserKey.equals(GenericCrudColumnPreferenceService.userKey(context))
                    && ownerRoleKey.equals(GenericCrudColumnPreferenceService.roleKey(context))
                    && entityKey.equals(context.getDefinition().getEntityKey());
        }
    }
}
