package ais.common;

import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * Template CRUD dinamis berbasis JSP.
 *
 * Prinsip desain:
 * - Java ini hanya menangani konfigurasi, render JSP, metadata model, dan operasi data JSON.
 * - HTML, CSS, dan JavaScript diletakkan di file JSP: dynamic_crud_template.jsp.
 * - Endpoint AJAX diletakkan di JSP tipis: dynamic_crud_api.jsp, yang memanggil handleAjax(...).
 *
 * Kompatibel dengan gaya project lama: JDK 1.6/1.7, Hibernate Criteria, org.json, dan servlet JSP.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class DynamicJspCrudGenerator { 

    public static final String SESSION_PREFIX = "AIS_DYNAMIC_CRUD_CONFIG_";
    public static final String DEFAULT_JSP_PATH = "/WEB-INF/baru/modul/common/dynamic_crud_template.jsp";

    /**
     * Nama method dibuat sama dengan generator lama agar mudah migrasi.
     */
    public static String generateWizardTable(JSONArray wizardConfigs) throws Exception {
        return render(wizardConfigs, DEFAULT_JSP_PATH);
    }


    /**
     * Generator otomatis: cukup panggil DynamicJspCrudGenerator.generate(Agama.class).
     * Kolom tabel, filter, dan form akan ditentukan dari metadata Hibernate.
     */
    public static String generate(Class clazz) throws Exception {
        return render(autoConfig(clazz), DEFAULT_JSP_PATH);
    }

    public static String generate(Class clazz, String dataTypeName) throws Exception {
        JSONArray arr = autoConfig(clazz);
        if (arr != null && arr.length() > 0 && !isBlank(dataTypeName)) {
            arr.getJSONObject(0).put("dataTypeName", dataTypeName);
            arr.getJSONObject(0).put("tabTitle", dataTypeName);
        }
        return render(arr, DEFAULT_JSP_PATH);
    }

    public static JSONArray autoConfig(Class clazz) throws Exception {
        String randomID = "rnd" + System.currentTimeMillis();
        JSONObject c = newConfig(clazz, humanize(clazz.getSimpleName()), randomID);
        ClassMetadata cm = HibernateUtil.getClassMetadata(clazz);

        JSONArray table = buildAutoTableColumns(cm);
        JSONArray form = buildAutoFormCols(cm);
        JSONArray search = buildAutoSearchCols(cm, table);

        c.put("tableColumns", table);
        c.put("tableLabels", labelsFor(table));
        JSONArray formOuter = new JSONArray();
        formOuter.put(form);
        c.put("formCols", formOuter);
        JSONArray formLabelOuter = new JSONArray();
        formLabelOuter.put(labelsFor(form));
        c.put("formLabels", formLabelOuter);
        JSONArray stepTitles = new JSONArray();
        stepTitles.put("Data " + humanize(clazz.getSimpleName()));
        c.put("stepTitles", stepTitles);
        c.put("searchCols", search);
        c.put("pageSize", 20);

        JSONArray req = new JSONArray();
        if (hasProperty(cm, "nama")) {
            req.put("nama");
        } else if (hasProperty(cm, "namaSiswa")) {
            req.put("namaSiswa");
        } else if (hasProperty(cm, "kode")) {
            req.put("kode");
        }
        c.put("paramRequired", req);

        JSONArray uniq = new JSONArray();
        if (hasProperty(cm, "kode")) uniq.put("kode");
        else if (hasProperty(cm, "code")) uniq.put("code");
        else if (hasProperty(cm, "mycode")) uniq.put("mycode");
        else if (hasProperty(cm, "nim")) uniq.put("nim");
        c.put("paramTidakBolehSama", uniq);

        JSONArray arr = new JSONArray();
        arr.put(c);
        return arr;
    }

    public static String render(JSONArray wizardConfigs) throws Exception {
        return render(wizardConfigs, DEFAULT_JSP_PATH);
    }

    public static String render(JSONObject wizardConfig) throws Exception {
        JSONArray arr = new JSONArray();
        arr.put(wizardConfig);
        return render(arr, DEFAULT_JSP_PATH);
    }

    public static String render(JSONArray wizardConfigs, String jspPath) throws Exception {
        if (wizardConfigs == null || wizardConfigs.length() == 0) {
            throw new IllegalArgumentException("Konfigurasi CRUD kosong");
        }

        JSONObject master = wizardConfigs.getJSONObject(0);
        String randomID = master.optString("randomID", "");
        if (isBlank(randomID)) {
            try {
                randomID = Common.getGeneratedBarCode(8);
            } catch (Exception e) {
                randomID = "rnd" + System.currentTimeMillis();
            }
            master.put("randomID", randomID);
        }

        normalizeConfigs(wizardConfigs, randomID);

        HttpServletRequest request = RequestContext.get();
        HttpServletResponse response = ResponseContext.get();
        request.getSession().setAttribute(SESSION_PREFIX + randomID, wizardConfigs.toString());

        String jspUrl = jspPath + "?rnd=" + URLEncoder.encode(randomID, "UTF-8");
        return JspRenderer.renderJsp(request, response, jspUrl);
    }

    /**
     * Helper konfigurasi agar index.jsp lebih pendek.
     */
    public static JSONObject newConfig(Class clazz, String dataTypeName, String randomID) throws Exception {
        JSONObject c = new JSONObject();
        c.put("modelClass", clazz.getName());
        c.put("dataTypeName", dataTypeName == null ? clazz.getSimpleName() : dataTypeName);
        if (!isBlank(randomID)) {
            c.put("randomID", randomID);
        }
        return c;
    }

    public static JSONObject putArray(JSONObject c, String key, String[] values) throws Exception {
        JSONArray arr = new JSONArray();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                arr.put(values[i]);
            }
        }
        c.put(key, arr);
        return c;
    }

    public static JSONObject putMatrix(JSONObject c, String key, String[][] values) throws Exception {
        JSONArray outer = new JSONArray();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                JSONArray inner = new JSONArray();
                if (values[i] != null) {
                    for (int j = 0; j < values[i].length; j++) {
                        inner.put(values[i][j]);
                    }
                }
                outer.put(inner);
            }
        }
        c.put(key, outer);
        return c;
    }

    public static JSONObject putSet(JSONObject c, String key, Set values) throws Exception {
        JSONArray arr = new JSONArray();
        if (values != null) {
            Iterator it = values.iterator();
            while (it.hasNext()) {
                arr.put(String.valueOf(it.next()));
            }
        }
        c.put(key, arr);
        return c;
    }

    public static JSONObject putSort(JSONObject c, int index, String field, String order) throws Exception {
        c.put("sort_default_" + index, field == null ? "" : field);
        c.put("order_default_" + index, order == null ? "" : order);
        return c;
    }

    public static JSONObject putRelationToMaster(JSONObject c, String childPropertyToMaster) throws Exception {
        c.put("relCol", childPropertyToMaster == null ? "" : childPropertyToMaster);
        c.put("relasi_ke_tabel_utama", childPropertyToMaster == null ? "" : childPropertyToMaster);
        return c;
    }

    public static String getConfigJson(HttpServletRequest request, String randomID) {
        if (request == null || isBlank(randomID)) {
            return "";
        }
        Object o = request.getSession().getAttribute(SESSION_PREFIX + randomID);
        return o == null ? "" : String.valueOf(o);
    }

    public static JSONObject handleAjax(HttpServletRequest request) {
        JSONObject out = new JSONObject();
        try {
            String rnd = nvl(request.getParameter("rnd"));
            String cfg = getConfigJson(request, rnd);
            if (isBlank(cfg)) {
                return error("Konfigurasi CRUD tidak ditemukan di session. Silakan refresh halaman.");
            }

            JSONArray configs = new JSONArray(cfg);
            String action = nvl(request.getParameter("action"));
            if ("meta".equalsIgnoreCase(action)) {
                return buildMeta(request, configs);
            }
            if ("list".equalsIgnoreCase(action)) {
                return list(request, configs);
            }
            if ("get".equalsIgnoreCase(action)) {
                return get(request, configs);
            }
            if ("save".equalsIgnoreCase(action)) {
                String saveId = nvl(request.getParameter("id"));
                if (isBlank(saveId) && !canCreate(request)) {
                    return error("Anda tidak memiliki hak akses tambah data.");
                }
                if (!isBlank(saveId) && !canEdit(request)) {
                    return error("Anda tidak memiliki hak akses ubah data.");
                }
                return save(request, configs);
            }
            if ("delete".equalsIgnoreCase(action)) {
                if (!canDelete(request)) {
                    return error("Anda tidak memiliki hak akses hapus data.");
                }
                return delete(request, configs);
            }
            if ("options".equalsIgnoreCase(action)) {
                return options(request);
            }
            if ("download".equalsIgnoreCase(action)) {
                return startDownloadExcel(request, configs);
            }
            if ("downloadProgress".equalsIgnoreCase(action)) {
                return downloadProgress(request);
            }
            if ("uploadExcel".equalsIgnoreCase(action)) {
                if (!canUploadExcel(request)) {
                    return error("Anda tidak memiliki hak akses upload. Upload membutuhkan hak Tambah, Ubah, dan Hapus.");
                }
                return uploadExcel(request, configs);
            }
            if ("stats".equalsIgnoreCase(action)) {
                if (!canViewStatistics(request)) {
                    return error("Anda tidak memiliki hak akses melihat statistik data.");
                }
                return startStatistics(request, configs);
            }
            if ("statsProgress".equalsIgnoreCase(action)) {
                if (!canViewStatistics(request)) {
                    return error("Anda tidak memiliki hak akses melihat statistik data.");
                }
                return statisticsProgress(request);
            }

            return error("Action tidak dikenal: " + action);
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DynamicJspCrudGenerator.java:312");
            try {
                out.put("status", "99");
                out.put("message", e.getMessage() == null ? e.toString() : e.getMessage());
            } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:316");
            }
        }
        return out;
    }

    private static JSONObject buildMeta(HttpServletRequest request, JSONArray configs) throws Exception {
        JSONObject result = ok();
        JSONArray configMetas = new JSONArray();

        for (int i = 0; i < configs.length(); i++) {
            JSONObject conf = configs.getJSONObject(i);
            JSONObject cmeta = new JSONObject();
            String modelClass = conf.getString("modelClass");
            Class clazz = Class.forName(modelClass);
            ClassMetadata cm = HibernateUtil.getClassMetadata(clazz);

            cmeta.put("index", i);
            cmeta.put("modelClass", modelClass);
            cmeta.put("dataTypeName", conf.optString("dataTypeName", clazz.getSimpleName()));
            cmeta.put("tabTitle", conf.optString("tabTitle", conf.optString("dataTypeName", clazz.getSimpleName())));
            cmeta.put("idProperty", cm.getIdentifierPropertyName());
            cmeta.put("relCol", getRelationColumn(conf));
            cmeta.put("stepTitles", conf.optJSONArray("stepTitles") == null ? new JSONArray() : conf.optJSONArray("stepTitles"));
            cmeta.put("formCols", conf.optJSONArray("formCols") == null ? new JSONArray() : conf.optJSONArray("formCols"));
            cmeta.put("formLabels", conf.optJSONArray("formLabels") == null ? new JSONArray() : conf.optJSONArray("formLabels"));
            cmeta.put("tableColumns", conf.optJSONArray("tableColumns") == null ? new JSONArray() : conf.optJSONArray("tableColumns"));
            cmeta.put("tableLabels", conf.optJSONArray("tableLabels") == null ? new JSONArray() : conf.optJSONArray("tableLabels"));
            cmeta.put("searchCols", conf.optJSONArray("searchCols") == null ? new JSONArray() : conf.optJSONArray("searchCols"));
            cmeta.put("required", conf.optJSONArray("paramRequired") == null ? new JSONArray() : conf.optJSONArray("paramRequired"));
            cmeta.put("lockedFields", buildLockedFields(request, cm));
            cmeta.put("contextInfo", buildContextInfo(request, cm));
            cmeta.put("canCreate", canCreate(request));
            cmeta.put("canAdd", canCreate(request));
            cmeta.put("canEdit", canEdit(request));
            cmeta.put("canDelete", canDelete(request));
            cmeta.put("canUpload", canUploadExcel(request));
            cmeta.put("canDownload", true);
            cmeta.put("canViewStatistics", canViewStatistics(request));
            cmeta.put("hasProfileImage", hasProfileImageColumn(conf, clazz));

            JSONObject fields = new JSONObject();
            JSONArray allFields = collectAllFieldSpecs(conf);
            addSpecsToMeta(fields, allFields, cm);
            addSpecsToMeta(fields, conf.optJSONArray("tableColumns"), cm);
            addSpecsToMeta(fields, conf.optJSONArray("searchCols"), cm);
            cmeta.put("fields", fields);

            configMetas.put(cmeta);
        }

        result.put("configs", configMetas);
        return result;
    }

    private static void addSpecsToMeta(JSONObject fields, JSONArray specs, ClassMetadata cm) throws Exception {
        if (specs == null) {
            return;
        }
        for (int i = 0; i < specs.length(); i++) {
            Object raw = specs.opt(i);
            if (raw instanceof JSONArray) {
                addSpecsToMeta(fields, (JSONArray) raw, cm);
                continue;
            }
            String spec = specs.optString(i, "");
            String field = cleanField(spec);
            if (isBlank(field) || fields.has(field)) {
                continue;
            }
            fields.put(field, metaForField(cm, spec));
        }
    }

    private static JSONObject metaForField(ClassMetadata cm, String spec) throws Exception {
        JSONObject m = new JSONObject();
        String field = cleanField(spec);
        String uiType = uiType(spec);
        String customType = customType(spec);
        String editor = editorType(spec);

        m.put("field", field);
        m.put("spec", spec == null ? "" : spec);
        m.put("uiType", uiType);
        m.put("customType", customType);
        m.put("editor", editor);

        JSONArray fixed = parseFixedOptions(customType);
        if (fixed != null) {
            m.put("uiType", "select");
            m.put("options", fixed);
        }

        if (field.startsWith("file_")) {
            m.put("uiType", "file");
            m.put("returnedClass", String.class.getName());
            return m;
        }

        try {
            Type type = cm.getPropertyType(field);
            Class rc = type.getReturnedClass();
            String rn = rc == null ? "" : rc.getName();
            m.put("returnedClass", rn);
            if (Date.class.isAssignableFrom(rc)) {
                m.put("uiType", "date");
            } else if (Boolean.class.equals(rc) || Boolean.TYPE.equals(rc)) {
                m.put("uiType", "boolean");
            } else if (Number.class.isAssignableFrom(rc) || Integer.TYPE.equals(rc) || Long.TYPE.equals(rc)
                    || Double.TYPE.equals(rc) || Float.TYPE.equals(rc) || BigDecimal.class.equals(rc)) {
                m.put("uiType", "number");
            } else if (rn.startsWith("ais.database.model")) {
                m.put("uiType", "fk");
                m.put("optionClass", rn);
            } else if ("editor".equalsIgnoreCase(customType) || "editor".equalsIgnoreCase(editor)
                    || "text".equalsIgnoreCase(customType)) {
                m.put("uiType", "textarea");
            }
        } catch (Exception e) {
            m.put("returnedClass", "");
        }
        return m;
    }

    private static JSONObject list(HttpServletRequest request, JSONArray configs) throws Exception {
        JSONObject conf = configs.getJSONObject(0);
        Class clazz = Class.forName(conf.getString("modelClass"));
        ClassMetadata cm = HibernateUtil.getClassMetadata(clazz);
        String sort = nvl(request.getParameter("sort"));
        String order = nvl(request.getParameter("order"));
        int page = parseInt(request.getParameter("page"), 1);
        int size = parseInt(request.getParameter("size"), 20);
        if (size <= 0) {
            size = 20;
        }
        if (size > 200) {
            size = 200;
        }

        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            Criteria countCriteria = session.createCriteria(clazz);
            applyFilters(countCriteria, request, conf, cm, true, session);
            Number totalNumber = (Number) countCriteria.setProjection(Projections.rowCount()).uniqueResult();
            long total = totalNumber == null ? 0 : totalNumber.longValue();

            Criteria dataCriteria = session.createCriteria(clazz);
            applyFilters(dataCriteria, request, conf, cm, false, session);
            applyOrders(dataCriteria, sort, order, conf, cm);
            dataCriteria.setFirstResult((page - 1) * size);
            dataCriteria.setMaxResults(size);

            List rows = dataCriteria.list();
            JSONArray records = new JSONArray();
            JSONArray tableCols = conf.optJSONArray("tableColumns");
            if (tableCols == null) {
                tableCols = new JSONArray();
            }
            for (int i = 0; rows != null && i < rows.size(); i++) {
                Object obj = rows.get(i);
                JSONObject row = objectToJson(obj, cm, tableCols);
                row.put("_id", identifierToString(cm, obj));
                if (hasProfileImageColumn(conf, clazz)) {
                    putProfileImageUrls(row, obj);
                }
                records.put(row);
            }

            JSONObject result = ok();
            result.put("records", records);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("pages", total == 0 ? 1 : ((total + size - 1) / size));
            return result;
        } finally {
            close(session);
        }
    }

    private static JSONObject get(HttpServletRequest request, JSONArray configs) throws Exception {
        String id = nvl(request.getParameter("id"));
        if (isBlank(id)) {
            return error("ID kosong");
        }

        JSONObject result = ok();
        JSONArray models = new JSONArray();
        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            JSONObject masterConf = configs.getJSONObject(0);
            Class masterClazz = Class.forName(masterConf.getString("modelClass"));
            ClassMetadata masterCm = HibernateUtil.getClassMetadata(masterClazz);
            Object masterObj = session.get(masterClazz, toIdentifier(masterCm, id));
            if (masterObj == null) {
                return error("Data utama tidak ditemukan");
            }

            for (int i = 0; i < configs.length(); i++) {
                JSONObject conf = configs.getJSONObject(i);
                Class clazz = Class.forName(conf.getString("modelClass"));
                ClassMetadata cm = HibernateUtil.getClassMetadata(clazz);
                Object obj = null;
                if (i == 0) {
                    obj = masterObj;
                } else {
                    String relCol = getRelationColumn(conf);
                    if (!isBlank(relCol)) {
                        Criteria c = session.createCriteria(clazz);
                        addRelationRestriction(c, cm, relCol, masterObj, masterCm, id, session);
                        c.addOrder(Order.desc(cm.getIdentifierPropertyName()));
                        c.setMaxResults(1);
                        List list = c.list();
                        if (list != null && !list.isEmpty()) {
                            obj = list.get(0);
                        }
                    }
                }

                JSONArray specs = collectAllFieldSpecs(conf);
                JSONObject data = obj == null ? new JSONObject() : objectToJson(obj, cm, specs);
                if (obj != null) {
                    data.put("_id", identifierToString(cm, obj));
                }
                JSONObject model = new JSONObject();
                model.put("index", i);
                model.put("data", data);
                models.put(model);
            }
        } finally {
            close(session);
        }
        result.put("models", models);
        return result;
    }

    private static JSONObject save(HttpServletRequest request, JSONArray configs) throws Exception {
        String masterId = nvl(request.getParameter("id"));
        Session session = null;
        Object masterObj = null;
        ClassMetadata masterCm = null;

        try {
            session = HibernateUtil.currentNativeSession();
            session.getTransaction().begin();

            for (int i = 0; i < configs.length(); i++) {
                JSONObject conf = configs.getJSONObject(i);
                Class clazz = Class.forName(conf.getString("modelClass"));
                ClassMetadata cm = HibernateUtil.getClassMetadata(clazz);
                String rowId = i == 0 ? masterId : nvl(request.getParameter("id_C" + i));
                Object obj = null;

                if (!isBlank(rowId)) {
                    obj = session.get(clazz, toIdentifier(cm, rowId));
                }
                if (obj == null && i > 0 && masterObj != null) {
                    String relCol = getRelationColumn(conf);
                    if (!isBlank(relCol)) {
                        Criteria c = session.createCriteria(clazz);
                        addRelationRestriction(c, cm, relCol, masterObj, masterCm, masterId, session);
                        c.setMaxResults(1);
                        List list = c.list();
                        if (list != null && !list.isEmpty()) {
                            obj = list.get(0);
                        }
                    }
                }
                if (obj == null) {
                    obj = clazz.newInstance();
                }

                if (i > 0 && masterObj != null) {
                    setRelationValue(obj, cm, getRelationColumn(conf), masterObj, masterCm, masterId, session);
                }

                setUserContextValues(obj, cm, request);

                JSONArray fields = collectAllFieldSpecs(conf);
                for (int f = 0; f < fields.length(); f++) {
                    String spec = fields.optString(f, "");
                    String field = cleanField(spec);
                    if (isBlank(field) || field.startsWith("file_")) {
                        continue;
                    }
                    try {
                        if (isLockedByUserContext(field, cm, request)) {
                            continue;
                        }
                        Type type = cm.getPropertyType(field);
                        String paramName = "C" + i + "_" + field;
                        String value = request.getParameter(paramName);
                        if (value == null && Boolean.class.equals(type.getReturnedClass())) {
                            value = "false";
                        }
                        Object converted = convertValue(type, value, session);
                        cm.setPropertyValue(obj, field, converted, EntityMode.POJO);
                    } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:615");
                        // Kolom non persistent / virtual dilewati supaya template tetap fleksibel.
                    }
                }

                setUserContextValues(obj, cm, request);

                validateRequired(request, conf, i);
                validateUnique(session, clazz, cm, obj, conf, i, request);

                if (obj instanceof GeneralValueObject) {
                    Common.refreshUpdate(session, (GeneralValueObject) obj);
                } else {
                    session.saveOrUpdate(obj);
                }

                if (i == 0) {
                    masterObj = obj;
                    masterCm = cm;
                    masterId = identifierToString(cm, obj);
                }
            }

            session.getTransaction().commit();
            JSONObject result = ok();
            result.put("id", masterId);
            result.put("message", "Data berhasil disimpan");
            return result;
        } catch (Exception e) {
            if (session != null && session.getTransaction() != null) {
                try {
                    session.getTransaction().rollback();
                } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:647");
                }
            }
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DynamicJspCrudGenerator.java:650");
            return error(e.getMessage() == null ? e.toString() : e.getMessage());
        } finally {
            close(session);
        }
    }

    private static JSONObject delete(HttpServletRequest request, JSONArray configs) throws Exception {
        String id = nvl(request.getParameter("id"));
        if (isBlank(id)) {
            return error("ID kosong");
        }
        JSONObject conf = configs.getJSONObject(0);
        Class clazz = Class.forName(conf.getString("modelClass"));
        ClassMetadata cm = HibernateUtil.getClassMetadata(clazz);
        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            Object obj = session.get(clazz, toIdentifier(cm, id));
            if (obj == null) {
                return error("Data tidak ditemukan");
            }
            session.getTransaction().begin();
            if (obj instanceof GeneralValueObject) {
                Common.refreshDelete(session, (GeneralValueObject) obj);
            } else {
                session.delete(obj);
            }
            session.getTransaction().commit();
            JSONObject result = ok();
            result.put("message", "Data berhasil dihapus");
            return result;
        } catch (Exception e) {
            if (session != null && session.getTransaction() != null) {
                try {
                    session.getTransaction().rollback();
                } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:686");
                }
            }
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DynamicJspCrudGenerator.java:689");
            return error(e.getMessage() == null ? e.toString() : e.getMessage());
        } finally {
            close(session);
        }
    }

    private static JSONObject options(HttpServletRequest request) throws Exception {
        String className = nvl(request.getParameter("className"));
        String q = nvl(request.getParameter("q"));
        int max = parseInt(request.getParameter("max"), 30);
        if (max <= 0 || max > 100) {
            max = 30;
        }
        Class clazz = Class.forName(className);
        ClassMetadata cm = HibernateUtil.getClassMetadata(clazz);
        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            Criteria c = session.createCriteria(clazz);
            applyUserContextRestrictions(c, cm, request, session);
            applyDependencyRestrictions(c, cm, request, session);

            if (!isBlank(q)) {
                Criterion cr = buildSearchCriterion(cm, q);
                if (cr != null) {
                    c.add(cr);
                }
            }
            applyBestOrder(c, cm);
            c.setMaxResults(max);
            List list = c.list();
            JSONArray opts = new JSONArray();
            JSONArray columns = buildLookupColumns(cm);
            for (int i = 0; list != null && i < list.size(); i++) {
                Object obj = list.get(i);
                JSONObject opt = new JSONObject();
                opt.put("id", identifierToString(cm, obj));
                opt.put("nama", displayValue(obj));
                opt.put("label", displayValue(obj));
                JSONObject row = objectToJson(obj, cm, columns);
                opt.put("row", row);
                opts.put(opt);
            }
            JSONObject result = ok();
            result.put("options", opts);
            result.put("columns", columns);
            result.put("columnLabels", labelsFor(columns));
            result.put("contextInfo", buildContextInfo(request, cm));
            return result;
        } finally {
            close(session);
        }
    }


    private static final String STATS_JOB_PREFIX = "AIS_DYNAMIC_CRUD_STATS_";

    private static JSONObject startStatistics(HttpServletRequest request, JSONArray configs) throws Exception {
        JSONObject conf = configs.getJSONObject(0);
        final String cfgText = configs.toString();
        final String jobId = String.valueOf(System.currentTimeMillis()) + "_" + String.valueOf(Math.round(Math.random() * 1000000));
        final HttpSession httpSession = request.getSession();
        final String baseKey = STATS_JOB_PREFIX + jobId + "_";
        final CrudRequestSnapshot snapshot = new CrudRequestSnapshot(request);

        setStatisticsStatus(httpSession, baseKey, "running", 1, "Menyiapkan statistik data...", null);

        Thread worker = new Thread(new Runnable() {
            public void run() {
                try {
                    JSONArray cfgs = new JSONArray(cfgText);
                    JSONObject cfg = cfgs.getJSONObject(0);
                    JSONObject data = generateStatisticsData(snapshot, httpSession, baseKey, cfg);
                    setStatisticsStatus(httpSession, baseKey, "done", 100, "Statistik selesai diproses.", data);
                } catch (Exception ex) {
                    ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/DynamicJspCrudGenerator.java:765");
                    setStatisticsStatus(httpSession, baseKey, "error", 100,
                            ex.getMessage() == null ? ex.toString() : ex.getMessage(), null);
                }
            }
        });
        worker.setDaemon(true);
        worker.start();

        JSONObject result = ok();
        result.put("jobId", jobId);
        result.put("message", "Proses statistik dimulai.");
        result.put("percent", 1);
        result.put("title", "Statistik " + conf.optString("dataTypeName", "Data"));
        return result;
    }

    private static JSONObject statisticsProgress(HttpServletRequest request) throws Exception {
        String jobId = nvl(request.getParameter("jobId"));
        if (isBlank(jobId)) {
            return error("Job statistik tidak ditemukan.");
        }
        HttpSession session = request.getSession();
        String baseKey = STATS_JOB_PREFIX + jobId + "_";
        JSONObject result = ok();
        result.put("jobId", jobId);
        result.put("statusJob", nvl((String) session.getAttribute(baseKey + "status")));
        result.put("message", nvl((String) session.getAttribute(baseKey + "message")));
        Object p = session.getAttribute(baseKey + "percent");
        result.put("percent", p == null ? 0 : Integer.parseInt(String.valueOf(p)));
        Object data = session.getAttribute(baseKey + "data");
        if (data instanceof JSONObject) {
            result.put("data", data);
        }
        return result;
    }

    private static void setStatisticsStatus(HttpSession session, String baseKey, String status, int percent, String message, JSONObject data) {
        try {
            session.setAttribute(baseKey + "status", status);
            session.setAttribute(baseKey + "percent", String.valueOf(percent < 0 ? 0 : (percent > 100 ? 100 : percent)));
            session.setAttribute(baseKey + "message", message == null ? "" : message);
            if (data != null) {
                session.setAttribute(baseKey + "data", data);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:810");
        }
    }

    private static JSONObject generateStatisticsData(HttpServletRequest request, HttpSession httpSession, String baseKey, JSONObject conf) throws Exception {
        Class clazz = Class.forName(conf.getString("modelClass"));
        ClassMetadata cm = HibernateUtil.getClassMetadata(clazz);
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            JSONObject data = new JSONObject();
            data.put("title", conf.optString("dataTypeName", clazz.getSimpleName()));
            data.put("modelClass", clazz.getName());

            setStatisticsStatus(httpSession, baseKey, "running", 5, "Menghitung total data aktif sesuai filter...", null);
            long total = countForStatistics(session, clazz, cm, request, conf);
            JSONArray cards = new JSONArray();
            JSONObject cTotal = new JSONObject();
            cTotal.put("label", "Total Data Aktif");
            cTotal.put("value", total);
            cTotal.put("icon", "fas fa-database");
            cards.put(cTotal);
            if (hasProperty(cm, "aktif")) {
                JSONObject cAktif = new JSONObject();
                cAktif.put("label", "Filter Aktif");
                cAktif.put("value", "Aktif / null");
                cAktif.put("icon", "fas fa-check-circle");
                cards.put(cAktif);
            }
            data.put("cards", cards);

            JSONArray candidates = buildStatisticsCandidates(cm);
            JSONArray groups = new JSONArray();
            int maxGroups = 10;
            int processed = 0;
            for (int i = 0; i < candidates.length(); i++) {
                String field = candidates.optString(i, "");
                if (isBlank(field) || !hasProperty(cm, field)) {
                    continue;
                }
                processed++;
                int pct = 10 + (int) Math.round((processed * 58.0) / Math.max(1, candidates.length()));
                setStatisticsStatus(httpSession, baseKey, "running", pct,
                        "Menganalisis statistik " + humanize(field) + " (" + processed + " dari " + candidates.length() + ")...", null);
                try {
                    JSONObject g = buildGroupStatistic(session, clazz, cm, request, conf, field);
                    if (g != null && g.optJSONArray("rows") != null && g.optJSONArray("rows").length() > 0) {
                        groups.put(g);
                        if (groups.length() >= maxGroups) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DynamicJspCrudGenerator.java:863");
                }
            }
            if (groups.length() == 0 && total > 0 && candidates.length() > 0) {
                setStatisticsStatus(httpSession, baseKey, "running", 70,
                        "Metode statistik cepat belum menemukan grup. Membaca data terfilter untuk analisis manual...", null);
                JSONArray fallbackGroups = buildManualStatisticsGroups(session, clazz, cm, request, conf, candidates,
                        total, httpSession, baseKey);
                for (int fg = 0; fallbackGroups != null && fg < fallbackGroups.length() && groups.length() < maxGroups; fg++) {
                    groups.put(fallbackGroups.getJSONObject(fg));
                }
            }
            if (groups.length() == 0 && total > 0) {
                setStatisticsStatus(httpSession, baseKey, "running", 91,
                        "Menyusun statistik dasar agar dashboard tetap informatif...", null);
                JSONArray guaranteedGroups = buildGuaranteedStatisticsGroups(session, clazz, cm, request, conf, total);
                for (int gg = 0; guaranteedGroups != null && gg < guaranteedGroups.length() && groups.length() < maxGroups; gg++) {
                    groups.put(guaranteedGroups.getJSONObject(gg));
                }
            }
            if (groups.length() == 0) {
                String note = total <= 0
                        ? "Tidak ada data aktif sesuai filter saat ini."
                        : "Belum ditemukan field statistik dari " + candidates.length()
                                + " kandidat. Field unik seperti nama, kode, keterangan, email, dan nomor unik otomatis dilewati.";
                data.put("statisticsNote", note);
            }
            data.put("groups", groups);

            setStatisticsStatus(httpSession, baseKey, "running", 76, "Membuat grafik tren perubahan data...", null);
            JSONObject trend = buildTanggalDirubahTrend(session, clazz, cm, request, conf, total, httpSession, baseKey);
            if (trend != null) {
                data.put("trend", trend);
            }
            setStatisticsStatus(httpSession, baseKey, "running", 96, "Menyusun tampilan statistik...", null);
            return data;
        } finally {
            close(session);
        }
    }

    private static long countForStatistics(Session session, Class clazz, ClassMetadata cm, HttpServletRequest request, JSONObject conf) throws Exception {
        Criteria c = session.createCriteria(clazz);
        applyFilters(c, request, conf, cm, true, session);
        applyActiveOnlyForStatistics(c, cm);
        Number n = (Number) c.setProjection(Projections.rowCount()).uniqueResult();
        return n == null ? 0 : n.longValue();
    }

    private static void applyActiveOnlyForStatistics(Criteria c, ClassMetadata cm) {
        try {
            if (hasProperty(cm, "aktif")) {
                Class rc = cm.getPropertyType("aktif").getReturnedClass();
                if (Boolean.TYPE.equals(rc)) {
                    c.add(Restrictions.eq("aktif", Boolean.TRUE));
                } else {
                    c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:922");
        }
    }

    private static JSONArray buildStatisticsCandidates(ClassMetadata cm) throws Exception {
        JSONArray arr = new JSONArray();
        String[] props = cm.getPropertyNames();
        for (int i = 0; i < props.length; i++) {
            String p = props[i];
            if (isExcludedStatisticField(p)) {
                continue;
            }
            try {
                Class rc = cm.getPropertyType(p).getReturnedClass();
                if (rc == null) {
                    continue;
                }
                if (isModelClass(rc) || Boolean.class.equals(rc) || Boolean.TYPE.equals(rc)
                        || Integer.class.equals(rc) || Integer.TYPE.equals(rc) || Long.class.equals(rc) || Long.TYPE.equals(rc)
                        || String.class.equals(rc)) {
                    arr.put(p);
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:944");
            }
        }
        return arr;
    }

    private static boolean isExcludedStatisticField(String p) {
        if (isBlank(p)) return true;
        String x = p.toLowerCase();
        if ("id".equals(x) || "nama".equals(x) || "namasiswa".equals(x) || "namalengkap".equals(x)
                || "kode".equals(x) || "code".equals(x) || "mycode".equals(x) || "nim".equals(x)
                || "noktp".equals(x) || "ktp".equals(x) || "nokk".equals(x) || "noregistrasi".equals(x)
                || "noujian".equals(x) || "email".equals(x) || "alamat".equals(x) || "keterangan".equals(x)
                || "deskripsi".equals(x) || "idregpd".equals(x) || "lockid".equals(x)
                || "pass".equals(x) || "password".equals(x) || "token".equals(x) || "olehid".equals(x)
                || "oleh".equals(x) || "tanggal_dirubah".equals(x)) {
            return true;
        }
        if (x.indexOf("password") >= 0 || x.indexOf("token") >= 0 || x.indexOf("uuid") >= 0
                || x.indexOf("facebook") >= 0 || x.indexOf("google") >= 0 || x.indexOf("twitter") >= 0
                || x.indexOf("linkedin") >= 0 || x.indexOf("socialmedia") >= 0) {
            return true;
        }
        return false;
    }

    private static JSONObject buildGroupStatistic(Session session, Class clazz, ClassMetadata cm, HttpServletRequest request, JSONObject conf, String field) throws Exception {
        Class rc = cm.getPropertyType(field).getReturnedClass();
        Number distinctNumber = null;
        try {
            Criteria dc = session.createCriteria(clazz);
            applyFilters(dc, request, conf, cm, true, session);
            applyActiveOnlyForStatistics(dc, cm);
            distinctNumber = (Number) dc.setProjection(Projections.countDistinct(field)).uniqueResult();
        } catch (Exception e) {
            distinctNumber = null;
        }
        long distinct = distinctNumber == null ? -1 : distinctNumber.longValue();
        if (distinct > 100) {
            return null;
        }
        if (!isModelClass(rc) && distinct >= 0 && distinct <= 1 && !isSingleValueStatisticAllowed(field, rc)) {
            return null;
        }

        Criteria c = session.createCriteria(clazz);
        applyFilters(c, request, conf, cm, true, session);
        applyActiveOnlyForStatistics(c, cm);
        c.setProjection(Projections.projectionList().add(Projections.groupProperty(field)).add(Projections.rowCount()));
        c.setMaxResults(120);
        List list = c.list();
        if (list == null || list.isEmpty()) {
            return null;
        }
        List rows = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            Object row = list.get(i);
            Object key = null;
            Number count = null;
            if (row instanceof Object[]) {
                Object[] a = (Object[]) row;
                key = a.length > 0 ? a[0] : null;
                count = a.length > 1 && a[1] instanceof Number ? (Number) a[1] : null;
            }
            long val = count == null ? 0 : count.longValue();
            if (val <= 0) continue;
            rows.add(new StatRow(statLabel(key), val));
        }
        return createStatisticGroup(field, humanize(field), isModelClass(rc) ? "relasi" : "field", rows);
    }

    /**
     * Fallback statistik manual. Ini sengaja disiapkan karena pada beberapa mapping Hibernate lama,
     * countDistinct/groupProperty terhadap many-to-one atau property inherited dapat gagal diam-diam.
     * Dengan fallback ini tab statistik tetap menghasilkan rekap untuk field yang relevan.
     */
    private static JSONArray buildManualStatisticsGroups(Session session, Class clazz, ClassMetadata cm,
            HttpServletRequest request, JSONObject conf, JSONArray candidates, long total,
            HttpSession httpSession, String baseKey) throws Exception {
        JSONArray result = new JSONArray();
        Criteria scan = session.createCriteria(clazz);
        applyFilters(scan, request, conf, cm, true, session);
        applyActiveOnlyForStatistics(scan, cm);
        String idp = cm.getIdentifierPropertyName();
        if (!isBlank(idp)) {
            try { scan.addOrder(Order.asc(idp)); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:1029");}
        }
        int limit = total > 50000L ? 50000 : (int) total;
        if (limit <= 0) {
            return result;
        }
        scan.setMaxResults(limit);
        List list = scan.list();
        int totalRows = list == null ? 0 : list.size();
        if (totalRows == 0) {
            return result;
        }

        for (int i = 0; i < candidates.length() && result.length() < 10; i++) {
            String field = candidates.optString(i, "");
            if (isBlank(field) || !hasProperty(cm, field)) {
                continue;
            }
            Class rc = null;
            try { rc = cm.getPropertyType(field).getReturnedClass(); } catch (Exception e) { rc = null; }
            if (rc == null) {
                continue;
            }

            setStatisticsStatus(httpSession, baseKey, "running", 70 + (int) Math.round((i * 20.0) / Math.max(1, candidates.length())),
                    "Menghitung statistik manual " + humanize(field) + " dari " + totalRows
                            + (total > totalRows ? " sample" : "") + " data...", null);

            java.util.LinkedHashMap counts = new java.util.LinkedHashMap();
            boolean tooMany = false;
            for (int r = 0; r < totalRows; r++) {
                Object obj = list.get(r);
                Object val = null;
                try {
                    val = cm.getPropertyValue(obj, field, EntityMode.POJO);
                } catch (Exception e) {
                    val = null;
                }
                String label = normalizeStatLabel(statLabel(val));
                Long old = (Long) counts.get(label);
                counts.put(label, Long.valueOf(old == null ? 1L : old.longValue() + 1L));
                if (counts.size() > 100) {
                    tooMany = true;
                    break;
                }
                if (r % 1000 == 0 || r + 1 == totalRows) {
                    int pct = 70 + (int) Math.round(((i + 1) * 20.0) / Math.max(1, candidates.length()));
                    setStatisticsStatus(httpSession, baseKey, "running", pct,
                            "Memproses " + humanize(field) + " " + (r + 1) + " dari " + totalRows + " data...", null);
                }
            }
            if (tooMany || counts.isEmpty()) {
                continue;
            }
            if (!isModelClass(rc) && counts.size() <= 1 && !isSingleValueStatisticAllowed(field, rc)) {
                continue;
            }
            List rows = new ArrayList();
            Iterator it = counts.keySet().iterator();
            while (it.hasNext()) {
                String label = String.valueOf(it.next());
                Long count = (Long) counts.get(label);
                if (count != null && count.longValue() > 0) {
                    rows.add(new StatRow(label, count.longValue()));
                }
            }
            JSONObject group = createStatisticGroup(field, humanize(field), isModelClass(rc) ? "relasi" : "field", rows);
            if (group != null) {
                if (total > totalRows) {
                    group.put("note", "Dihitung dari " + totalRows + " sample pertama dari " + total + " data.");
                }
                result.put(group);
            }
        }
        return result;
    }


    /**
     * Fallback paling aman. Beberapa tabel master hanya punya kode/nama/keterangan/aktif,
     * sedangkan kode/nama/keterangan memang sengaja dilewati karena cenderung unik.
     * Agar tab statistik tidak kosong, buat rekap dasar dari field yang masih relevan
     * seperti aktif, feeder, tahun*, semester*, status*, jenis*, program*, dan relasi model.
     */
    private static JSONArray buildGuaranteedStatisticsGroups(Session session, Class clazz, ClassMetadata cm,
            HttpServletRequest request, JSONObject conf, long total) throws Exception {
        JSONArray result = new JSONArray();
        JSONArray safeFields = new JSONArray();
        try {
            if (hasProperty(cm, "aktif")) {
                safeFields.put("aktif");
            }
            if (hasProperty(cm, "feeder")) {
                safeFields.put("feeder");
            }
            String[] props = cm.getPropertyNames();
            for (int i = 0; props != null && i < props.length; i++) {
                String p = props[i];
                if (isBlank(p) || "aktif".equalsIgnoreCase(p) || "feeder".equalsIgnoreCase(p)
                        || isExcludedStatisticField(p)) {
                    continue;
                }
                Class rc = null;
                try { rc = cm.getPropertyType(p).getReturnedClass(); } catch (Exception e) { rc = null; }
                if (rc == null) {
                    continue;
                }
                if (isModelClass(rc) || isSingleValueStatisticAllowed(p, rc)) {
                    safeFields.put(p);
                }
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:1140");}

        for (int i = 0; i < safeFields.length() && result.length() < 6; i++) {
            String field = safeFields.optString(i, "");
            if (isBlank(field) || !hasProperty(cm, field)) {
                continue;
            }
            try {
                JSONObject g = buildManualSingleStatisticGroup(session, clazz, cm, request, conf, field, total);
                if (g != null) {
                    result.put(g);
                }
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DynamicJspCrudGenerator.java:1153");
            }
        }

        // Bila semua field tetap gagal, tampilkan ringkasan total data aktif sebagai grafik sederhana.
        if (result.length() == 0 && total > 0) {
            List rows = new ArrayList();
            rows.add(new StatRow("Data aktif sesuai filter", total));
            JSONObject g = createStatisticGroup("total", "Ringkasan Data", "ringkasan", rows);
            if (g != null) {
                g.put("note", "Statistik dasar ditampilkan karena field group lain tidak dapat dihitung oleh mapping Hibernate saat ini.");
                result.put(g);
            }
        }
        return result;
    }

    private static JSONObject buildManualSingleStatisticGroup(Session session, Class clazz, ClassMetadata cm,
            HttpServletRequest request, JSONObject conf, String field, long total) throws Exception {
        Criteria scan = session.createCriteria(clazz);
        applyFilters(scan, request, conf, cm, true, session);
        applyActiveOnlyForStatistics(scan, cm);
        scan.setMaxResults(total > 50000L ? 50000 : (int) total);
        List list = scan.list();
        if (list == null || list.isEmpty()) {
            return null;
        }
        Class rc = null;
        try { rc = cm.getPropertyType(field).getReturnedClass(); } catch (Exception e) { rc = null; }
        java.util.LinkedHashMap counts = new java.util.LinkedHashMap();
        for (int r = 0; r < list.size(); r++) {
            Object obj = list.get(r);
            Object val = null;
            try {
                val = cm.getPropertyValue(obj, field, EntityMode.POJO);
            } catch (Exception e) {
                val = null;
            }
            String label = normalizeStatLabel(statLabel(val));
            Long old = (Long) counts.get(label);
            counts.put(label, Long.valueOf(old == null ? 1L : old.longValue() + 1L));
            if (counts.size() > 100) {
                return null;
            }
        }
        if (counts.isEmpty()) {
            return null;
        }
        // Untuk field aktif, meskipun hanya satu nilai, tetap tampilkan karena user perlu tahu filter aktif berlaku.
        if (!"aktif".equalsIgnoreCase(field) && !isModelClass(rc) && counts.size() <= 1
                && !isSingleValueStatisticAllowed(field, rc)) {
            return null;
        }
        List rows = new ArrayList();
        Iterator it = counts.keySet().iterator();
        while (it.hasNext()) {
            String label = String.valueOf(it.next());
            Long count = (Long) counts.get(label);
            if (count != null && count.longValue() > 0) {
                rows.add(new StatRow(label, count.longValue()));
            }
        }
        JSONObject g = createStatisticGroup(field, humanize(field), isModelClass(rc) ? "relasi" : "field", rows);
        if (g != null && total > list.size()) {
            g.put("note", "Dihitung dari " + list.size() + " sample pertama dari " + total + " data.");
        }
        return g;
    }

    private static JSONObject createStatisticGroup(String field, String label, String type, List rows) throws Exception {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        Collections.sort(rows, new java.util.Comparator() {
            public int compare(Object o1, Object o2) {
                StatRow a = (StatRow) o1;
                StatRow b = (StatRow) o2;
                return a.count == b.count ? a.label.compareToIgnoreCase(b.label) : (a.count < b.count ? 1 : -1);
            }
        });
        JSONArray jr = new JSONArray();
        JSONArray labels = new JSONArray();
        JSONArray values = new JSONArray();
        for (int i = 0; i < rows.size() && i < 30; i++) {
            StatRow sr = (StatRow) rows.get(i);
            JSONObject r = new JSONObject();
            r.put("label", sr.label);
            r.put("value", sr.count);
            jr.put(r);
            if (i < 12) {
                labels.put(sr.label);
                values.put(sr.count);
            }
        }
        if (jr.length() == 0) return null;
        JSONObject g = new JSONObject();
        g.put("field", field);
        g.put("label", label);
        g.put("rows", jr);
        g.put("chartLabels", labels);
        g.put("chartValues", values);
        g.put("type", type);
        return g;
    }

    private static boolean isSingleValueStatisticAllowed(String field, Class rc) {
        if (isBlank(field)) return false;
        String x = field.toLowerCase();
        return Boolean.class.equals(rc) || Boolean.TYPE.equals(rc)
                || x.indexOf("tahun") >= 0 || x.indexOf("semester") >= 0 || x.indexOf("status") >= 0
                || x.indexOf("jenis") >= 0 || x.indexOf("kelompok") >= 0 || x.indexOf("program") >= 0
                || x.indexOf("kelas") >= 0 || x.indexOf("kelamin") >= 0 || x.indexOf("warga") >= 0
                || x.indexOf("negara") >= 0 || x.indexOf("jalur") >= 0 || x.indexOf("kategori") >= 0
                || x.indexOf("tipe") >= 0 || x.indexOf("level") >= 0 || x.indexOf("waktu") >= 0;
    }

    private static String normalizeStatLabel(String label) {
        if (label == null) return "Tidak diisi";
        String s = label.trim();
        if (s.length() == 0 || "null".equalsIgnoreCase(s)) return "Tidak diisi";
        if (s.length() > 90) {
            s = s.substring(0, 87) + "...";
        }
        return s;
    }

    private static JSONObject buildTanggalDirubahTrend(Session session, Class clazz, ClassMetadata cm, HttpServletRequest request,
            JSONObject conf, long total, HttpSession httpSession, String baseKey) throws Exception {
        if (!hasProperty(cm, "tanggal_dirubah")) {
            return null;
        }
        Criteria c = session.createCriteria(clazz);
        applyFilters(c, request, conf, cm, true, session);
        applyActiveOnlyForStatistics(c, cm);
        c.setProjection(Projections.property("tanggal_dirubah"));
        c.setMaxResults(50000);
        List dates = c.list();
        Map map = new TreeMap();
        SimpleDateFormat ym = new SimpleDateFormat("yyyy-MM");
        int totalDate = dates == null ? 0 : dates.size();
        for (int i = 0; dates != null && i < dates.size(); i++) {
            Object d = dates.get(i);
            if (d instanceof Date) {
                String key = ym.format((Date) d);
                Long old = (Long) map.get(key);
                map.put(key, Long.valueOf(old == null ? 1L : old.longValue() + 1L));
            }
            if (totalDate > 0 && (i % 500 == 0 || i + 1 == totalDate)) {
                int pct = 78 + (int) Math.round(((i + 1) * 15.0) / totalDate);
                setStatisticsStatus(httpSession, baseKey, "running", pct,
                        "Mengelompokkan tren tanggal_dirubah " + (i + 1) + " dari " + totalDate + " data...", null);
            }
        }
        if (map.isEmpty()) {
            return null;
        }
        JSONArray rows = new JSONArray();
        JSONArray labels = new JSONArray();
        JSONArray values = new JSONArray();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = String.valueOf(it.next());
            Long val = (Long) map.get(key);
            JSONObject r = new JSONObject();
            r.put("label", key);
            r.put("value", val.longValue());
            rows.put(r);
            labels.put(key);
            values.put(val.longValue());
        }
        JSONObject trend = new JSONObject();
        trend.put("field", "tanggal_dirubah");
        trend.put("label", "Tren Perubahan Data per Bulan");
        trend.put("rows", rows);
        trend.put("chartLabels", labels);
        trend.put("chartValues", values);
        return trend;
    }

    private static String statLabel(Object key) {
        if (key == null) return "Tidak diisi";
        if (key instanceof Date) {
            try { return new SimpleDateFormat("dd-MM-yyyy").format((Date) key); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:1335");}
        }
        if (key instanceof Boolean) return ((Boolean) key).booleanValue() ? "Ya" : "Tidak";
        if (isModelClass(key.getClass())) return displayValue(key);
        String s = String.valueOf(key);
        return isBlank(s) || "null".equalsIgnoreCase(s) ? "Tidak diisi" : s;
    }

    /**
     * Tipe implementasi bersarang {@link StatRow} milik {@link DynamicJspCrudGenerator}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * DynamicJspCrudGenerator}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
     * dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String label}, {@code long count}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see DynamicJspCrudGenerator
     */
    private static class StatRow {
        String label;
        long count;
        StatRow(String label, long count) { this.label = label == null ? "" : label; this.count = count; }
    }


    private static final String DOWNLOAD_JOB_PREFIX = "AIS_DYNAMIC_CRUD_DOWNLOAD_";

    /**
     * Memulai proses download Excel secara asynchronous agar request JSP tetap JSON.
     * File dibuat di /tmp webapp, lalu frontend melakukan download dari URL statis.
     */
    private static JSONObject startDownloadExcel(HttpServletRequest request, JSONArray configs) throws Exception {
        JSONObject conf = configs.getJSONObject(0);
        final String cfgText = configs.toString();
        final String jobId = String.valueOf(System.currentTimeMillis()) + "_" + String.valueOf(Math.round(Math.random() * 1000000));
        final HttpSession httpSession = request.getSession();
        final String baseKey = DOWNLOAD_JOB_PREFIX + jobId + "_";
        final CrudRequestSnapshot snapshot = new CrudRequestSnapshot(request);

        setDownloadStatus(httpSession, baseKey, "running", 1, "Menyiapkan proses download Excel...", null, null);

        Thread worker = new Thread(new Runnable() {
            public void run() {
                try {
                    JSONArray cfgs = new JSONArray(cfgText);
                    JSONObject cfg = cfgs.getJSONObject(0);
                    generateDownloadExcelFile(snapshot, httpSession, baseKey, cfg);
                } catch (Exception ex) {
                    ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/DynamicJspCrudGenerator.java:1373");
                    setDownloadStatus(httpSession, baseKey, "error", 100,
                            ex.getMessage() == null ? ex.toString() : ex.getMessage(), null, null);
                }
            }
        });
        worker.setDaemon(true);
        worker.start();

        JSONObject result = ok();
        result.put("jobId", jobId);
        result.put("message", "Proses download Excel dimulai.");
        result.put("percent", 1);
        result.put("title", "Download " + conf.optString("dataTypeName", "Data"));
        return result;
    }

    private static JSONObject downloadProgress(HttpServletRequest request) throws Exception {
        String jobId = nvl(request.getParameter("jobId"));
        if (isBlank(jobId)) {
            return error("Job download tidak ditemukan.");
        }
        HttpSession session = request.getSession();
        String baseKey = DOWNLOAD_JOB_PREFIX + jobId + "_";
        JSONObject result = ok();
        result.put("jobId", jobId);
        result.put("statusJob", nvl((String) session.getAttribute(baseKey + "status")));
        result.put("message", nvl((String) session.getAttribute(baseKey + "message")));
        Object p = session.getAttribute(baseKey + "percent");
        result.put("percent", p == null ? 0 : Integer.parseInt(String.valueOf(p)));
        String url = nvl((String) session.getAttribute(baseKey + "url"));
        if (!isBlank(url)) {
            result.put("downloadUrl", url);
        }
        String fileName = nvl((String) session.getAttribute(baseKey + "fileName"));
        if (!isBlank(fileName)) {
            result.put("fileName", fileName);
        }
        return result;
    }

    private static void generateDownloadExcelFile(HttpServletRequest request, HttpSession httpSession, String baseKey, JSONObject conf) throws Exception {
        Class clazz = Class.forName(conf.getString("modelClass"));
        ClassMetadata cm = HibernateUtil.getClassMetadata(clazz);
        JSONArray excelCols = buildExcelColumns(conf, cm);
        Session session = null;
        XSSFWorkbook workbook = null;
        FileOutputStream fileOut = null;
        try {
            setDownloadStatus(httpSession, baseKey, "running", 5, "Mengambil data sesuai filter aktif...", null, null);
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = session.createCriteria(clazz);
            applyFilters(criteria, request, conf, cm, false, session);
            applyOrders(criteria, nvl(request.getParameter("sort")), nvl(request.getParameter("order")), conf, cm);
            criteria.setMaxResults(1048576);
            List rows = criteria.list();
            int total = rows == null ? 0 : rows.size();
            setDownloadStatus(httpSession, baseKey, "running", 20,
                    "Data ditemukan " + total + " baris. Menyiapkan workbook...", null, null);

            workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet(clazz.getSimpleName());
            sheet.setDefaultColumnWidth(20);

            XSSFRow head = sheet.createRow(0);
            for (int i = 0; i < excelCols.length(); i++) {
                XSSFCell cell = head.createCell(i);
                cell.setCellValue(excelCols.optString(i));
            }
            int deleteCol = excelCols.length();
            XSSFCell deleteCell = head.createCell(deleteCol);
            deleteCell.setCellValue("Apakah Hapus?");

            for (int r = 0; rows != null && r < rows.size(); r++) {
                Object obj = rows.get(r);
                XSSFRow row = sheet.createRow(r + 1);
                for (int c = 0; c < excelCols.length(); c++) {
                    String field = cleanField(excelCols.optString(c, ""));
                    XSSFCell cell = row.createCell(c);
                    Object val = getExportValue(obj, cm, field);
                    setExcelCellValue(cell, val);
                }
                XSSFCell del = row.createCell(deleteCol);
                del.setCellValue("FALSE");

                if (total > 0 && (r % 20 == 0 || r + 1 == total)) {
                    int percent = 25 + (int) Math.round(((r + 1) * 60.0) / total);
                    setDownloadStatus(httpSession, baseKey, "running", percent,
                            "Menulis data Excel " + (r + 1) + " dari " + total + " baris...", null, null);
                }
                if (r > 0 && r % 200 == 0 && session != null) {
                    try { session.flush(); session.clear(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:1464");}
                }
            }

            setDownloadStatus(httpSession, baseKey, "running", 88, "Merapikan ukuran kolom Excel...", null, null);
            for (int i = 0; i <= excelCols.length(); i++) {
                try { sheet.autoSizeColumn(i); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:1470");}
            }

            String fileName = "Daftar_" + clazz.getSimpleName() + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".xlsx";
            File tmpDir = new File(httpSession.getServletContext().getRealPath("/tmp"));
            if (!tmpDir.exists()) {
                tmpDir.mkdirs();
            }
            File outFile = new File(tmpDir, fileName);
            setDownloadStatus(httpSession, baseKey, "running", 94, "Menyimpan file Excel...", null, null);
            fileOut = new FileOutputStream(outFile);
            workbook.write(fileOut);
            fileOut.flush();

            String url = request.getContextPath() + "/tmp/" + URLEncoder.encode(fileName, "UTF-8");
            setDownloadStatus(httpSession, baseKey, "done", 100, "Download siap.", url, fileName);
        } finally {
            try { if (fileOut != null) fileOut.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:1487");}
            close(session);
        }
    }

    private static void setDownloadStatus(HttpSession session, String baseKey, String status, int percent, String message, String url, String fileName) {
        try {
            session.setAttribute(baseKey + "status", status);
            session.setAttribute(baseKey + "percent", String.valueOf(percent < 0 ? 0 : (percent > 100 ? 100 : percent)));
            session.setAttribute(baseKey + "message", message == null ? "" : message);
            if (url != null) session.setAttribute(baseKey + "url", url);
            if (fileName != null) session.setAttribute(baseKey + "fileName", fileName);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:1499");}
    }

    /**
     * Tipe implementasi bersarang {@link CrudRequestSnapshot} milik {@link DynamicJspCrudGenerator}. Kelas ini
     * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * DynamicJspCrudGenerator}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
     * dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Map params}, {@code HttpSession
     * session}, {@code String contextPath}; operasi lokal: {@code getParameter()}, {@code getParameterValues()},
     * {@code getParameterMap()}, {@code getParameterNames()}, {@code getSession()}, {@code getSession()}, {@code
     * getContextPath}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see DynamicJspCrudGenerator
     */
    private static class CrudRequestSnapshot extends HttpServletRequestWrapper {
        private Map params = new HashMap();
        private HttpSession session;
        private String contextPath;

        CrudRequestSnapshot(HttpServletRequest request) {
            super(request);
            this.session = request.getSession();
            this.contextPath = request.getContextPath();
            try {
                Map m = request.getParameterMap();
                Iterator it = m.keySet().iterator();
                while (it.hasNext()) {
                    Object key = it.next();
                    Object value = m.get(key);
                    if (value instanceof String[]) {
                        String[] arr = (String[]) value;
                        String[] copy = new String[arr.length];
                        System.arraycopy(arr, 0, copy, 0, arr.length);
                        params.put(String.valueOf(key), copy);
                    } else if (value != null) {
                        params.put(String.valueOf(key), new String[] { String.valueOf(value) });
                    }
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:1526");}
        }

        public String getParameter(String name) {
            String[] v = (String[]) params.get(name);
            return v == null || v.length == 0 ? null : v[0];
        }

        public String[] getParameterValues(String name) {
            return (String[]) params.get(name);
        }

        public Map getParameterMap() {
            return params;
        }

        public Enumeration getParameterNames() {
            return Collections.enumeration(params.keySet());
        }

        public HttpSession getSession() {
            return session;
        }

        public HttpSession getSession(boolean create) {
            return session;
        }

        public String getContextPath() {
            return contextPath == null ? "" : contextPath;
        }
    }



    /**
     * Download Excel untuk data utama. Data yang diunduh mengikuti filter yang sedang aktif di tabel.
     */
    public static void downloadExcel(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String rnd = nvl(request.getParameter("rnd"));
        String cfg = getConfigJson(request, rnd);
        if (isBlank(cfg)) {
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().print("Konfigurasi CRUD tidak ditemukan di session. Silakan refresh halaman.");
            return;
        }

        JSONArray configs = new JSONArray(cfg);
        JSONObject conf = configs.getJSONObject(0);
        Class clazz = Class.forName(conf.getString("modelClass"));
        ClassMetadata cm = HibernateUtil.getClassMetadata(clazz);
        JSONArray excelCols = buildExcelColumns(conf, cm);

        Session session = null;
        XSSFWorkbook workbook = null;
        try {
            session = HibernateUtil.currentNativeSession();
            Criteria criteria = session.createCriteria(clazz);
            applyFilters(criteria, request, conf, cm, false, session);
            applyOrders(criteria, nvl(request.getParameter("sort")), nvl(request.getParameter("order")), conf, cm);
            criteria.setMaxResults(1048576);
            List rows = criteria.list();

            workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet(clazz.getSimpleName());
            sheet.setDefaultColumnWidth(20);

            XSSFRow head = sheet.createRow(0);
            for (int i = 0; i < excelCols.length(); i++) {
                XSSFCell cell = head.createCell(i);
                cell.setCellValue(excelCols.optString(i));
            }
            int deleteCol = excelCols.length();
            XSSFCell deleteCell = head.createCell(deleteCol);
            deleteCell.setCellValue("Apakah Hapus?");

            for (int r = 0; rows != null && r < rows.size(); r++) {
                Object obj = rows.get(r);
                XSSFRow row = sheet.createRow(r + 1);
                for (int c = 0; c < excelCols.length(); c++) {
                    String field = cleanField(excelCols.optString(c, ""));
                    XSSFCell cell = row.createCell(c);
                    Object val = getExportValue(obj, cm, field);
                    setExcelCellValue(cell, val);
                }
                XSSFCell del = row.createCell(deleteCol);
                del.setCellValue("FALSE");
            }

            for (int i = 0; i <= excelCols.length(); i++) {
                try { sheet.autoSizeColumn(i); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:1616");}
            }

            String fn = "Daftar_" + clazz.getSimpleName() + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fn + "\"");
            OutputStream os = response.getOutputStream();
            workbook.write(os);
            os.flush();
        } finally {
            close(session);
        }
    }

    /**
     * Upload Excel sederhana berbasis file hasil download dari template ini.
     * Membutuhkan hak tambah + ubah + hapus, dicek sebelum method ini dipanggil.
     */
    private static JSONObject uploadExcel(HttpServletRequest request, JSONArray configs) throws Exception {
        JSONObject conf = configs.getJSONObject(0);
        Class clazz = Class.forName(conf.getString("modelClass"));
        ClassMetadata cm = HibernateUtil.getClassMetadata(clazz);
        JSONArray excelCols = buildExcelColumns(conf, cm);
        String fileName = nvl(request.getParameter("fileName"));
        String fileData = nvl(request.getParameter("fileData"));
        if (isBlank(fileData)) {
            return error("File upload kosong.");
        }
        if (!isBlank(fileName) && !fileName.toLowerCase().endsWith(".xlsx")) {
            return error("File yang diupload harus berformat .xlsx.");
        }
        int comma = fileData.indexOf(',');
        if (comma >= 0) {
            fileData = fileData.substring(comma + 1);
        }

        byte[] bytes = decodeBase64(fileData);
        XSSFWorkbook workbook = null;
        Session session = null;
        List warnings = new ArrayList();
        int saved = 0;
        int deleted = 0;
        try {
            workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));
            XSSFSheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return error("Sheet Excel tidak ditemukan.");
            }
            String sheetName = sheet.getSheetName() == null ? "" : sheet.getSheetName();
            if (!clazz.getSimpleName().toLowerCase().startsWith(sheetName.toLowerCase())
                    && !sheetName.toLowerCase().startsWith(clazz.getSimpleName().toLowerCase())) {
                return error("Upload data gagal. File upload tidak sesuai, harus dari menu/class " + clazz.getSimpleName() + ".");
            }

            List columns = readHeaderColumns(sheet, excelCols);
            int hapusCol = findColumnIndex(sheet, "Apakah Hapus?");
            session = HibernateUtil.currentNativeSession();
            int rowCount = sheet.getLastRowNum() + 1;
            for (int r = 1; r < rowCount; r++) {
                XSSFRow row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                try {
                    String idValue = getCellString(row, 0);
                    Object obj = findUploadTarget(session, clazz, cm, idValue, row, columns, request);
                    boolean isDelete = hapusCol >= 0 && parseBoolean(getCellString(row, hapusCol)).booleanValue();
                    if (obj != null && isDelete) {
                        session.getTransaction().begin();
                        if (obj instanceof GeneralValueObject) {
                            Common.refreshDelete(session, (GeneralValueObject) obj);
                        } else {
                            session.delete(obj);
                        }
                        session.getTransaction().commit();
                        deleted++;
                        continue;
                    }
                    if (obj == null) {
                        obj = clazz.newInstance();
                    }

                    setUserContextValues(obj, cm, request);
                    for (int c = 0; c < columns.size(); c++) {
                        String field = cleanField(String.valueOf(columns.get(c)));
                        if (isBlank(field) || "id".equalsIgnoreCase(field) || field.startsWith("file_") || !hasProperty(cm, field)) {
                            continue;
                        }
                        if (isLockedByUserContext(field, cm, request)) {
                            continue;
                        }
                        try {
                            Type type = cm.getPropertyType(field);
                            String value = getCellString(row, c);
                            Object converted = convertExcelValue(type, value, session);
                            cm.setPropertyValue(obj, field, converted, EntityMode.POJO);
                        } catch (Exception ex) {
                            warnings.add("Baris " + (r + 1) + ", kolom " + field + ": " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
                        }
                    }
                    setUserContextValues(obj, cm, request);
                    session.getTransaction().begin();
                    if (obj instanceof GeneralValueObject) {
                        Common.refreshUpdate(session, (GeneralValueObject) obj);
                    } else {
                        session.saveOrUpdate(obj);
                    }
                    session.getTransaction().commit();
                    saved++;
                } catch (Exception exRow) {
                    try { if (session != null && session.getTransaction() != null) session.getTransaction().rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:1726");}
                    warnings.add("Baris " + (r + 1) + ": " + (exRow.getMessage() == null ? exRow.toString() : exRow.getMessage()));
                }
            }
        } finally {
            close(session);
        }

        JSONObject result = ok();
        result.put("message", "Upload selesai. Disimpan: " + saved + ", dihapus: " + deleted + (warnings.isEmpty() ? "" : ", peringatan: " + warnings.size()));
        JSONArray warn = new JSONArray();
        for (int i = 0; i < warnings.size(); i++) {
            warn.put(String.valueOf(warnings.get(i)));
        }
        result.put("warnings", warn);
        return result;
    }

    private static void validateRequired(HttpServletRequest request, JSONObject conf, int idx) throws Exception {
        JSONArray required = conf.optJSONArray("paramRequired");
        if (required == null) {
            return;
        }
        for (int i = 0; i < required.length(); i++) {
            String f = cleanField(required.optString(i, ""));
            if (isBlank(f)) {
                continue;
            }
            String value = nvl(request.getParameter("C" + idx + "_" + f));
            if (isBlank(value)) {
                throw new Exception("Field wajib belum diisi: " + f);
            }
        }
    }

    private static void validateUnique(Session session, Class clazz, ClassMetadata cm, Object obj, JSONObject conf, int idx,
            HttpServletRequest request) throws Exception {
        JSONArray uniques = conf.optJSONArray("paramTidakBolehSama");
        if (uniques == null) {
            return;
        }
        Serializable currentId = null;
        try {
            currentId = cm.getIdentifier(obj, EntityMode.POJO);
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:1770");
        }

        for (int i = 0; i < uniques.length(); i++) {
            String f = cleanField(uniques.optString(i, ""));
            if (isBlank(f) || !hasProperty(cm, f)) {
                continue;
            }
            String value = nvl(request.getParameter("C" + idx + "_" + f));
            if (isBlank(value)) {
                continue;
            }
            Criteria c = session.createCriteria(clazz).add(Restrictions.eq(f, value));
            List list = c.setMaxResults(2).list();
            for (int j = 0; list != null && j < list.size(); j++) {
                Object other = list.get(j);
                Serializable otherId = cm.getIdentifier(other, EntityMode.POJO);
                if (currentId == null || !String.valueOf(currentId).equals(String.valueOf(otherId))) {
                    throw new Exception("Data dengan nilai " + f + " = " + value + " sudah ada");
                }
            }
        }
    }

    private static void applyFilters(Criteria criteria, HttpServletRequest request, JSONObject conf, ClassMetadata cm,
            boolean forCount, Session session) throws Exception {
        String sqlWhere = conf.optString("sqlWhere", "");
        if (!isBlank(sqlWhere)) {
            criteria.add(Restrictions.sqlRestriction("(" + sqlWhere + ")"));
        }

        applyUserContextRestrictions(criteria, cm, request, session);

        JSONArray searchCols = conf.optJSONArray("searchCols");
        String keyword = nvl(request.getParameter("keyword"));
        if (!isBlank(keyword) && searchCols != null) {
            Criterion or = null;
            Set aliases = new HashSet();
            for (int i = 0; i < searchCols.length(); i++) {
                String f = cleanField(searchCols.optString(i, ""));
                if (isBlank(f) || !hasProperty(cm, f)) {
                    continue;
                }
                Class rc = cm.getPropertyType(f).getReturnedClass();
                Criterion cr = null;
                if (String.class.equals(rc)) {
                    cr = Restrictions.ilike(f, keyword, MatchMode.ANYWHERE);
                } else if (Number.class.isAssignableFrom(rc)) {
                    try {
                        cr = Restrictions.eq(f, Integer.valueOf(keyword));
                    } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:1820");
                    }
                } else if (rc != null && rc.getName().startsWith("ais.database.model")) {
                    String alias = "a_" + f;
                    if (!aliases.contains(alias)) {
                        criteria.createAlias(f, alias);
                        aliases.add(alias);
                    }
                    cr = Restrictions.ilike(alias + ".nama", keyword, MatchMode.ANYWHERE);
                }
                if (cr != null) {
                    or = or == null ? cr : Restrictions.or(or, cr);
                }
            }
            if (or != null) {
                criteria.add(or);
            }
        }

        if (searchCols != null) {
            for (int i = 0; i < searchCols.length(); i++) {
                String spec = searchCols.optString(i, "");
                String f = cleanField(spec);
                String value = nvl(request.getParameter("f_" + f));
                if (isBlank(f) || isBlank(value) || !hasProperty(cm, f)) {
                    continue;
                }
                Class rc = cm.getPropertyType(f).getReturnedClass();
                if (String.class.equals(rc)) {
                    criteria.add(Restrictions.ilike(f, value, MatchMode.ANYWHERE));
                } else if (Boolean.class.equals(rc) || Boolean.TYPE.equals(rc)) {
                    criteria.add(Restrictions.eq(f, parseBoolean(value)));
                } else if (Date.class.isAssignableFrom(rc)) {
                    Date d = parseDateSafe(value);
                    if (d != null) {
                        criteria.add(Restrictions.eq(f, d));
                    }
                } else if (Number.class.isAssignableFrom(rc) || Integer.TYPE.equals(rc) || Long.TYPE.equals(rc)) {
                    Object n = convertNumber(rc, value);
                    if (n != null) {
                        criteria.add(Restrictions.eq(f, n));
                    }
                } else if (rc != null && rc.getName().startsWith("ais.database.model")) {
                    Object fk = session.get(rc, toIdentifier(HibernateUtil.getClassMetadata(rc), value));
                    if (fk != null) {
                        criteria.add(Restrictions.eq(f, fk));
                    }
                }
            }
        }
    }

    private static void applyOrders(Criteria criteria, String sort, String order, JSONObject conf, ClassMetadata cm) {
        boolean ordered = false;
        if (!isBlank(sort) && hasProperty(cm, sort)) {
            criteria.addOrder("desc".equalsIgnoreCase(order) ? Order.desc(sort) : Order.asc(sort));
            ordered = true;
        }
        for (int i = 1; i <= 5; i++) {
            String s = conf.optString("sort_default_" + i, "");
            String o = conf.optString("order_default_" + i, "asc");
            if (!isBlank(s) && hasProperty(cm, s)) {
                criteria.addOrder("desc".equalsIgnoreCase(o) ? Order.desc(s) : Order.asc(s));
                ordered = true;
            }
        }
        if (!ordered) {
            String idp = cm.getIdentifierPropertyName();
            if (!isBlank(idp)) {
                criteria.addOrder(Order.desc(idp));
            }
        }
    }

    private static boolean hasProfileImageColumn(JSONObject conf, Class clazz) {
        try {
            if (conf != null && conf.has("showProfileImage")) {
                return conf.optBoolean("showProfileImage", false);
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:1899");
        }
        return isImageSupportedClass(clazz);
    }

    private static boolean isImageSupportedClass(Class clazz) {
        if (clazz == null) {
            return false;
        }
        String[] supported = new String[] {
                "ais.database.model.Mahasiswa",
                "ais.database.model.sekolah.Siswa",
                "ais.database.model.sekolah.CalonSiswa",
                "ais.database.model.Dosen",
                "ais.database.model.sekolah.Guru",
                "ais.database.model.BiodataCalonMahasiswa",
                "ais.database.model.Pegawai",
                "ais.database.model.Tbmuser",
                "ais.database.model.asset.PenyediaAsset",
                "ais.database.model.koperasi.AnggotaKoperasi" };
        for (int i = 0; i < supported.length; i++) {
            try {
                Class sc = Class.forName(supported[i]);
                if (sc.isAssignableFrom(clazz)) {
                    return true;
                }
            } catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:1925");
            }
        }
        return false;
    }

    private static void putProfileImageUrls(JSONObject row, Object obj) {
        try {
            if (obj instanceof GeneralValueObject) {
                String small = ProfileImageUtil.getUrlFotoDariObject((GeneralValueObject) obj, true);
                String big = ProfileImageUtil.getUrlFotoDariObject((GeneralValueObject) obj, false);
                row.put("_imageUrl", small == null ? "" : small);
                row.put("_imagePreviewUrl", big == null ? "" : big);
            }
        } catch (Exception ignored) {
            try {
                row.put("_imageUrl", "");
                row.put("_imagePreviewUrl", "");
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:1943");
            }
        }
    }

    private static JSONObject objectToJson(Object obj, ClassMetadata cm, JSONArray specs) throws Exception {
        JSONObject row = new JSONObject();
        if (obj == null) {
            return row;
        }
        if (specs == null) {
            specs = new JSONArray();
        }
        try {
            row.put("id", identifierToString(cm, obj));
        } catch (Throwable ignored) {
            row.put("id", "");
        }
        for (int i = 0; i < specs.length(); i++) {
            Object raw = specs.opt(i);
            if (raw instanceof JSONArray) {
                JSONObject child = objectToJson(obj, cm, (JSONArray) raw);
                Iterator keys = child.keys();
                while (keys.hasNext()) {
                    String key = String.valueOf(keys.next());
                    row.put(key, child.opt(key));
                }
                continue;
            }
            String spec = specs.optString(i, "");
            String f = cleanField(spec);
            if (isBlank(f) || f.startsWith("file_")) {
                continue;
            }
            try {
                Object val = cm.getPropertyValue(obj, f, EntityMode.POJO);
                putValue(row, f, val);
            } catch (Throwable ignored) {
                row.put(f, "");
            }
        }
        return row;
    }

    private static void putValue(JSONObject row, String f, Object val) throws Exception {
        if (val == null) {
            row.put(f, "");
            return;
        }
        if (val instanceof Date) {
            row.put(f, new SimpleDateFormat("dd/MM/yyyy").format((Date) val));
            row.put(f + "_iso", new SimpleDateFormat("yyyy-MM-dd").format((Date) val));
            return;
        }
        if (val instanceof Boolean) {
            row.put(f, ((Boolean) val).booleanValue());
            row.put(f + "_label", ((Boolean) val).booleanValue() ? "Ya" : "Tidak");
            return;
        }
        if (val instanceof Number) {
            row.put(f, val);
            return;
        }
        if (val instanceof GeneralValueObject || val.getClass().getName().startsWith("ais.database.model")) {
            try {
                ClassMetadata vcm = HibernateUtil.getClassMetadata(val.getClass());
                row.put(f + "_id", identifierToString(vcm, val));
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2005");
            }
            row.put(f + "_nama", displayValue(val));
            row.put(f, displayValue(val));
            return;
        }
        row.put(f, String.valueOf(val));
    }

    private static Object convertValue(Type type, String value, Session session) throws Exception {
        Class rc = type.getReturnedClass();
        if (value != null) {
            value = value.trim();
        }
        if (String.class.equals(rc)) {
            return value == null ? "" : value;
        }
        if (isBlank(value)) {
            return null;
        }
        if (Date.class.isAssignableFrom(rc)) {
            return parseDateSafe(value);
        }
        if (Boolean.class.equals(rc) || Boolean.TYPE.equals(rc)) {
            return parseBoolean(value);
        }
        if (Integer.class.equals(rc) || Integer.TYPE.equals(rc)) {
            return Integer.valueOf(value);
        }
        if (Long.class.equals(rc) || Long.TYPE.equals(rc)) {
            return Long.valueOf(value);
        }
        if (Double.class.equals(rc) || Double.TYPE.equals(rc)) {
            return Double.valueOf(value);
        }
        if (Float.class.equals(rc) || Float.TYPE.equals(rc)) {
            return Float.valueOf(value);
        }
        if (BigDecimal.class.equals(rc)) {
            return new BigDecimal(value);
        }
        if (rc != null && rc.getName().startsWith("ais.database.model")) {
            ClassMetadata fkMeta = HibernateUtil.getClassMetadata(rc);
            return session.get(rc, toIdentifier(fkMeta, value));
        }
        return value;
    }

    private static Date parseDateSafe(String val) {
        if (isBlank(val)) {
            return null;
        }
        String[] patterns = new String[] { "yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "dd/MM/yy", "dd-MM-yy",
                "yyyy-MM-dd HH:mm:ss", "dd/MM/yyyy HH:mm", "dd-MM-yyyy HH:mm" };
        for (int i = 0; i < patterns.length; i++) {
            try {
                return new SimpleDateFormat(patterns[i]).parse(val);
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2062");
            }
        }
        try {
            return Common.timeFormat2.get().parse(val);
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2067");
        }
        try {
            return Common.timeFormat.get().parse(val);
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2071");
        }
        try {
            return Common.dateFormat1.get().parse(val);
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2075");
        }
        return null;
    }

    private static void addRelationRestriction(Criteria c, ClassMetadata childMeta, String relCol, Object masterObj,
            ClassMetadata masterMeta, String masterId, Session session) throws Exception {
        if (isBlank(relCol) || !hasProperty(childMeta, relCol)) {
            return;
        }
        Class rc = childMeta.getPropertyType(relCol).getReturnedClass();
        if (rc != null && rc.getName().startsWith("ais.database.model")) {
            c.add(Restrictions.eq(relCol, masterObj));
        } else {
            Object v = convertValue(childMeta.getPropertyType(relCol), masterId, session);
            c.add(Restrictions.eq(relCol, v));
        }
    }

    private static void setRelationValue(Object obj, ClassMetadata childMeta, String relCol, Object masterObj,
            ClassMetadata masterMeta, String masterId, Session session) {
        if (isBlank(relCol) || !hasProperty(childMeta, relCol)) {
            return;
        }
        try {
            Class rc = childMeta.getPropertyType(relCol).getReturnedClass();
            if (rc != null && rc.getName().startsWith("ais.database.model")) {
                childMeta.setPropertyValue(obj, relCol, masterObj, EntityMode.POJO);
            } else {
                Object v = convertValue(childMeta.getPropertyType(relCol), masterId, session);
                childMeta.setPropertyValue(obj, relCol, v, EntityMode.POJO);
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2107");
        }
    }

    private static Serializable toIdentifier(ClassMetadata cm, String value) throws Exception {
        Class rc = cm.getIdentifierType().getReturnedClass();
        if (String.class.equals(rc)) {
            return value;
        }
        if (Long.class.equals(rc) || Long.TYPE.equals(rc)) {
            return Long.valueOf(value);
        }
        if (Integer.class.equals(rc) || Integer.TYPE.equals(rc)) {
            return Integer.valueOf(value);
        }
        if (Short.class.equals(rc) || Short.TYPE.equals(rc)) {
            return Short.valueOf(value);
        }
        if (BigDecimal.class.equals(rc)) {
            return new BigDecimal(value);
        }
        return value;
    }

    private static Object convertNumber(Class rc, String value) {
        try {
            if (Integer.class.equals(rc) || Integer.TYPE.equals(rc)) {
                return Integer.valueOf(value);
            }
            if (Long.class.equals(rc) || Long.TYPE.equals(rc)) {
                return Long.valueOf(value);
            }
            if (Double.class.equals(rc) || Double.TYPE.equals(rc)) {
                return Double.valueOf(value);
            }
            if (Float.class.equals(rc) || Float.TYPE.equals(rc)) {
                return Float.valueOf(value);
            }
            if (BigDecimal.class.equals(rc)) {
                return new BigDecimal(value);
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2148");
        }
        return null;
    }

    private static String identifierToString(ClassMetadata cm, Object obj) {
        try {
            Serializable id = cm.getIdentifier(obj, EntityMode.POJO);
            return id == null ? "" : String.valueOf(id);
        } catch (Exception e) {
            return "";
        }
    }

    private static String displayValue(Object obj) {
        if (obj == null) {
            return "";
        }
        String[] methods = new String[] { "getNama", "getNamaSiswa", "getNamaLengkap", "getNamaMahasiswa",
                "getNamaPegawai", "getNamaDosen", "getKode", "getCode", "getMycode", "getNim", "getNis",
                "getNip", "getNoRegistrasi", "getNoUjian", "getJudul", "getTitle", "getKeterangan",
                "getDeskripsi", "getId", "getUserId", "getRoleId" };
        for (int i = 0; i < methods.length; i++) {
            try {
                Method m = obj.getClass().getMethod(methods[i], new Class[0]);
                Object v = m.invoke(obj, new Object[0]);
                if (v != null && !isBlank(String.valueOf(v)) && !"null".equalsIgnoreCase(String.valueOf(v))) {
                    return String.valueOf(v);
                }
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2177");
            }
        }
        return "";
    }




    private static JSONArray buildExcelColumns(JSONObject conf, ClassMetadata cm) throws Exception {
        LinkedHashSet set = new LinkedHashSet();
        String idp = cm.getIdentifierPropertyName();
        if (!isBlank(idp)) {
            set.add(idp);
        } else {
            set.add("id");
        }
        JSONArray explicit = conf.optJSONArray("excelColumns");
        if (explicit != null) {
            for (int i = 0; i < explicit.length(); i++) {
                String f = cleanField(explicit.optString(i, ""));
                if (!isBlank(f) && ("id".equalsIgnoreCase(f) || hasProperty(cm, f))) {
                    set.add(f);
                }
            }
        }
        addJsonArrayToSet(set, conf.optJSONArray("tableColumns"), cm);
        addJsonArrayToSet(set, collectAllFieldSpecs(conf), cm);
        addIfHas(cm, set, "kode"); addIfHas(cm, set, "code"); addIfHas(cm, set, "mycode");
        addIfHas(cm, set, "nama"); addIfHas(cm, set, "namaSiswa"); addIfHas(cm, set, "keterangan");
        addIfHas(cm, set, "aktif"); addIfHas(cm, set, "feeder");
        return setToArray(set);
    }

    private static void addJsonArrayToSet(LinkedHashSet set, JSONArray arr, ClassMetadata cm) {
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            Object raw = arr.opt(i);
            if (raw instanceof JSONArray) {
                addJsonArrayToSet(set, (JSONArray) raw, cm);
                continue;
            }
            String f = cleanField(arr.optString(i, ""));
            if (!isBlank(f) && !f.startsWith("file_") && ("id".equalsIgnoreCase(f) || hasProperty(cm, f))) {
                set.add(f);
            }
        }
    }

    private static Object getExportValue(Object obj, ClassMetadata cm, String field) {
        if (obj == null || isBlank(field)) return "";
        try {
            if (field.equals(cm.getIdentifierPropertyName()) || "id".equalsIgnoreCase(field)) {
                return identifierToString(cm, obj);
            }
            Object val = cm.getPropertyValue(obj, field, EntityMode.POJO);
            if (val == null) return "";
            if (val instanceof Date) return new SimpleDateFormat("dd/MM/yyyy").format((Date) val);
            if (val instanceof GeneralValueObject || isModelClass(val.getClass())) return displayValue(val);
            return val;
        } catch (Exception e) {
            return "";
        }
    }

    private static void setExcelCellValue(XSSFCell cell, Object val) {
        if (val == null) {
            cell.setCellValue("");
            return;
        }
        try {
            if (val instanceof Integer) cell.setCellValue(((Integer) val).doubleValue());
            else if (val instanceof Long) cell.setCellValue(((Long) val).doubleValue());
            else if (val instanceof Double) cell.setCellValue(((Double) val).doubleValue());
            else if (val instanceof Float) cell.setCellValue(((Float) val).doubleValue());
            else if (val instanceof BigDecimal) cell.setCellValue(((BigDecimal) val).doubleValue());
            else if (val instanceof Boolean) cell.setCellValue(((Boolean) val).booleanValue() ? "TRUE" : "FALSE");
            else cell.setCellValue(String.valueOf(val));
        } catch (Exception e) {
            cell.setCellValue(String.valueOf(val));
        }
    }

    private static byte[] decodeBase64(String data) throws Exception {
        try {
            Class c = Class.forName("java.util.Base64");
            Object decoder = c.getMethod("getDecoder", new Class[0]).invoke(null, new Object[0]);
            return (byte[]) decoder.getClass().getMethod("decode", new Class[] { String.class }).invoke(decoder, new Object[] { data });
        } catch (Exception e) {
            Class c = Class.forName("sun.misc.BASE64Decoder");
            Object decoder = c.newInstance();
            return (byte[]) c.getMethod("decodeBuffer", new Class[] { String.class }).invoke(decoder, new Object[] { data });
        }
    }

    private static List readHeaderColumns(XSSFSheet sheet, JSONArray fallback) {
        List cols = new ArrayList();
        try {
            XSSFRow head = sheet.getRow(0);
            int count = head == null ? 0 : head.getLastCellNum();
            for (int i = 0; i < count; i++) {
                String h = getCellString(head, i);
                if (!isBlank(h) && !"Apakah Hapus?".equalsIgnoreCase(h)) {
                    cols.add(h.trim());
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2283");}
        if (cols.isEmpty() && fallback != null) {
            for (int i = 0; i < fallback.length(); i++) cols.add(fallback.optString(i, ""));
        }
        return cols;
    }

    private static int findColumnIndex(XSSFSheet sheet, String name) {
        try {
            XSSFRow head = sheet.getRow(0);
            int count = head == null ? 0 : head.getLastCellNum();
            for (int i = 0; i < count; i++) {
                if (name.equalsIgnoreCase(getCellString(head, i))) return i;
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2297");}
        return -1;
    }

    private static String getCellString(XSSFRow row, int col) {
        if (row == null || col < 0) return "";
        try {
            XSSFCell cell = row.getCell(col);
            if (cell == null) return "";
            try { return cell.getStringCellValue(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2306");}
            try { return String.valueOf((long) cell.getNumericCellValue()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2307");}
            try { return String.valueOf(cell.getBooleanCellValue()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2308");}
            return String.valueOf(cell);
        } catch (Exception e) { return ""; }
    }

    private static Object findUploadTarget(Session session, Class clazz, ClassMetadata cm, String idValue, XSSFRow row,
            List columns, HttpServletRequest request) throws Exception {
        if (!isBlank(idValue)) {
            try {
                Criteria c = session.createCriteria(clazz);
                applyUserContextRestrictions(c, cm, request, session);
                c.add(Restrictions.idEq(toIdentifier(cm, idValue)));
                Object obj = c.setMaxResults(1).uniqueResult();
                if (obj != null) return obj;
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2322");}
        }
        String[] uniqueFields = new String[] { "kode", "code", "mycode", "nim", "noRegistrasi", "noUjian" };
        for (int u = 0; u < uniqueFields.length; u++) {
            String uf = uniqueFields[u];
            if (!hasProperty(cm, uf)) continue;
            int ix = indexOfColumn(columns, uf);
            if (ix < 0) continue;
            String val = getCellString(row, ix);
            if (isBlank(val)) continue;
            try {
                Criteria c = session.createCriteria(clazz);
                applyUserContextRestrictions(c, cm, request, session);
                c.add(Restrictions.eq(uf, convertExcelValue(cm.getPropertyType(uf), val, session)));
                Object obj = c.setMaxResults(1).uniqueResult();
                if (obj != null) return obj;
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2338");}
        }
        return null;
    }

    private static int indexOfColumn(List columns, String field) {
        if (columns == null) return -1;
        for (int i = 0; i < columns.size(); i++) {
            String c = cleanField(String.valueOf(columns.get(i)));
            if (field.equalsIgnoreCase(c)) return i;
        }
        return -1;
    }

    private static Object convertExcelValue(Type type, String value, Session session) throws Exception {
        if (value != null) value = value.trim();
        Class rc = type.getReturnedClass();
        if (rc != null && rc.getName().startsWith("ais.database.model")) {
            if (isBlank(value)) return null;
            Object obj = findModelByText(session, rc, value);
            if (obj != null) return obj;
        }
        return convertValue(type, value, session);
    }

    private static Object findModelByText(Session session, Class rc, String value) {
        try {
            ClassMetadata cm = HibernateUtil.getClassMetadata(rc);
            Criteria c = session.createCriteria(rc);
            String[] fields = new String[] { "kode", "code", "mycode", "nim", "noRegistrasi", "noUjian", "nama", "namaSiswa" };
            Criterion or = null;
            for (int i = 0; i < fields.length; i++) {
                String f = fields[i];
                if (hasProperty(cm, f)) {
                    try {
                        Criterion one = Restrictions.eq(f, value);
                        or = or == null ? one : Restrictions.or(or, one);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2375");}
                }
            }
            if (or != null) {
                c.add(or);
                return c.setMaxResults(1).uniqueResult();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2382");}
        return null;
    }

    /**
     * Membuat halaman multi-tab. Setiap elemen tabDefs adalah JSONObject dengan:
     * - tabTitle (String): judul tab
     * - tabIcon  (String, opsional): kelas icon CSS, misal "fas fa-user"
     * - type     (String): "crud", "links", atau "html"
     * Untuk type="crud"  : wizardConfigs (JSONArray) — sama seperti input generateWizardTable
     * Untuk type="links" : links (JSONArray) berisi {title, url, icon, description}
     *                       dan panelTitle (String, opsional)
     * Untuk type="html"  : html (String) konten mentah HTML
     */
    public static String generateTabPanel(JSONArray tabDefs) throws Exception {
        if (tabDefs == null || tabDefs.length() == 0) {
            throw new IllegalArgumentException("Tab definitions kosong");
        }
        String pid = "atp" + System.currentTimeMillis();
        List tabContents = new ArrayList();
        for (int i = 0; i < tabDefs.length(); i++) {
            JSONObject td = tabDefs.getJSONObject(i);
            String type = td.optString("type", "links");
            String content = "";
            try {
                if ("crud".equalsIgnoreCase(type)) {
                    JSONArray wc = td.optJSONArray("wizardConfigs");
                    if (wc != null && wc.length() > 0) {
                        content = render(wc);
                    }
                } else if ("links".equalsIgnoreCase(type)) {
                    content = buildLinksPanel(td.optString("panelTitle", td.optString("tabTitle", "")), td.optJSONArray("links"));
                } else if ("html".equalsIgnoreCase(type)) {
                    content = td.optString("html", "");
                }
            } catch (Exception e) {
                content = "<div style='padding:14px;color:#b91c1c;background:#fee2e2;border-radius:10px;margin:8px 0;'>Gagal memuat konten tab: "
                        + htmlEscape(e.getMessage() == null ? e.toString() : e.getMessage()) + "</div>";
            }
            tabContents.add(content);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<style>");
        sb.append("#").append(pid).append("{font-family:Inter,system-ui,sans-serif;margin-bottom:18px;}");
        sb.append("#").append(pid).append(" .atp-nav{display:flex;gap:0;flex-wrap:wrap;margin-bottom:16px;border-bottom:2px solid #e2e8f0;}");
        sb.append("#").append(pid).append(" .atp-tab{padding:11px 18px;border:0;background:transparent;font-size:13px;font-weight:700;cursor:pointer;color:#64748b;border-bottom:3px solid transparent;margin-bottom:-2px;white-space:nowrap;transition:.16s;}");
        sb.append("#").append(pid).append(" .atp-tab:hover{color:#1d4ed8;background:#f0f6ff;}");
        sb.append("#").append(pid).append(" .atp-tab.atp-on{color:var(--theme-primary,#2563eb);border-bottom-color:var(--theme-primary,#2563eb);background:#eff6ff;}");
        sb.append("#").append(pid).append(" .atp-pane{display:none;}");
        sb.append("#").append(pid).append(" .atp-pane.atp-on{display:block;}");
        sb.append("#").append(pid).append(" .atp-link-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(185px,1fr));gap:12px;padding:6px 0;}");
        sb.append("#").append(pid).append(" .atp-lc{background:#fff;border:1.5px solid #e2e8f0;border-radius:18px;padding:18px 16px;text-decoration:none;color:#1e293b;display:flex;flex-direction:column;gap:7px;transition:.17s;box-shadow:0 2px 8px rgba(15,23,42,.05);}");
        sb.append("#").append(pid).append(" .atp-lc:hover{transform:translateY(-2px);box-shadow:0 12px 30px rgba(15,23,42,.13);border-color:var(--theme-primary,#93c5fd);}");
        sb.append("#").append(pid).append(" .atp-li{font-size:26px;color:var(--theme-primary,#2563eb);margin-bottom:2px;}");
        sb.append("#").append(pid).append(" .atp-lt{font-weight:800;font-size:14px;line-height:1.3;}");
        sb.append("#").append(pid).append(" .atp-ld{font-size:12px;color:#64748b;line-height:1.4;}");
        sb.append("#").append(pid).append(" .atp-pt{font-size:16px;font-weight:900;color:#0f172a;margin:4px 0 14px;padding-bottom:10px;border-bottom:1px solid #e2e8f0;}");
        sb.append("</style>");

        sb.append("<div id=\"").append(pid).append("\">");
        sb.append("<div class=\"atp-nav\">");
        for (int i = 0; i < tabDefs.length(); i++) {
            JSONObject td = tabDefs.getJSONObject(i);
            String ti = td.optString("tabIcon", "");
            sb.append("<button class=\"atp-tab").append(i == 0 ? " atp-on" : "").append("\" onclick=\"atpSw_").append(pid).append("(").append(i).append(")\">");
            if (!isBlank(ti)) { sb.append("<i class=\"").append(ti).append("\"></i> "); }
            sb.append(htmlEscape(td.optString("tabTitle", "Tab " + (i + 1)))).append("</button>");
        }
        sb.append("</div>");

        for (int i = 0; i < tabContents.size(); i++) {
            sb.append("<div id=\"").append(pid).append("_p").append(i).append("\" class=\"atp-pane").append(i == 0 ? " atp-on" : "").append("\">");
            sb.append((String) tabContents.get(i));
            sb.append("</div>");
        }
        sb.append("</div>");

        sb.append("<script>(function(){function sw(idx){var el=document.getElementById('").append(pid)
          .append("');el.querySelectorAll('.atp-tab').forEach(function(b,i){b.classList.toggle('atp-on',i===idx);});")
          .append("el.querySelectorAll('.atp-pane').forEach(function(p,i){p.classList.toggle('atp-on',i===idx);});}");
        sb.append("window['atpSw_").append(pid).append("']=sw;}());</script>");

        return sb.toString();
    }

    private static String buildLinksPanel(String title, JSONArray links) throws Exception {
        StringBuilder sb = new StringBuilder();
        if (!isBlank(title)) {
            sb.append("<div class=\"atp-pt\">").append(htmlEscape(title)).append("</div>");
        }
        sb.append("<div class=\"atp-link-grid\">");
        if (links != null) {
            for (int i = 0; i < links.length(); i++) {
                JSONObject lnk = links.getJSONObject(i);
                String url  = lnk.optString("url", "#");
                String lt   = lnk.optString("title", "");
                String icon = lnk.optString("icon", "fas fa-link");
                String desc = lnk.optString("description", lnk.optString("desc", ""));
                sb.append("<a href=\"").append(url).append("\" class=\"atp-lc\">");
                sb.append("<div class=\"atp-li\"><i class=\"").append(icon).append("\"></i></div>");
                sb.append("<div class=\"atp-lt\">").append(htmlEscape(lt)).append("</div>");
                if (!isBlank(desc)) {
                    sb.append("<div class=\"atp-ld\">").append(htmlEscape(desc)).append("</div>");
                }
                sb.append("</a>");
            }
        }
        sb.append("</div>");
        return sb.toString();
    }

    private static String htmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static boolean canCreate(HttpServletRequest request) {
        Tbmuser u = currentUser(request);
        try { return u != null && (CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE, u) || Common.getApakahAdminLain(u)); } catch (Exception e) { return false; }
    }
    private static boolean canEdit(HttpServletRequest request) {
        Tbmuser u = currentUser(request);
        try { return u != null && (CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, u) || Common.getApakahAdminLain(u)); } catch (Exception e) { return false; }
    }
    private static boolean canDelete(HttpServletRequest request) {
        Tbmuser u = currentUser(request);
        try { return u != null && (CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, u) || Common.getApakahAdminLain(u)); } catch (Exception e) { return false; }
    }
    private static boolean canUploadExcel(HttpServletRequest request) {
        return canCreate(request) && canEdit(request) && canDelete(request);
    }

    private static boolean canViewStatistics(HttpServletRequest request) {
        Tbmuser u = currentUser(request);
        if (u == null) {
            return false;
        }
        try {
            if (Common.getApakahAdminLain(u)) {
                return true;
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2524");
        }
        try {
            Object role = u.hakAkses();
            String roleId = null;
            if (role != null) {
                Method m = role.getClass().getMethod("getRoleId", new Class[0]);
                Object v = m.invoke(role, new Object[0]);
                roleId = v == null ? null : String.valueOf(v);
            }
            String nilai = Common.getKonfigurasi("daftar_hak_akses_yg_bisa_lihat_statistik_data",
                    "am,admfak,admprd,Akademik,amp").getNilai();
            String[] daftar = nilai == null ? new String[0] : nilai.split(",");
            for (int i = 0; i < daftar.length; i++) {
                if (roleId != null && daftar[i] != null && daftar[i].trim().equalsIgnoreCase(roleId.trim())) {
                    return true;
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2542");
        }
        return false;
    }

    private static Tbmuser currentUser(HttpServletRequest request) {
        try { return Common.getCurrentUser(request); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2548");}
        try { return Common.getCurrentUser(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2549");}
        return null;
    }

    private static JSONArray buildAutoTableColumns(ClassMetadata cm) throws Exception {
        LinkedHashSet set = new LinkedHashSet();
        String idp = cm.getIdentifierPropertyName();
        if (!isBlank(idp)) set.add(idp);
        addIfHas(cm, set, "kode"); addIfHas(cm, set, "code"); addIfHas(cm, set, "mycode");
        addIfHas(cm, set, "nim"); addIfHas(cm, set, "noRegistrasi"); addIfHas(cm, set, "noUjian");
        addIfHas(cm, set, "nama"); addIfHas(cm, set, "namaSiswa");
        addIfHas(cm, set, "tahunAngkatan"); addIfHas(cm, set, "tahunangkatan");
        addContextProps(cm, set);
        addIfHas(cm, set, "aktif"); addIfHas(cm, set, "keterangan"); addIfHas(cm, set, "deskripsi");
        String[] props = cm.getPropertyNames();
        for (int i = 0; i < props.length && set.size() < 9; i++) {
            String p = props[i];
            if (isAutoSkipField(p)) continue;
            try {
                Class rc = cm.getPropertyType(p).getReturnedClass();
                if (isSimpleForAuto(rc) || isModelClass(rc)) set.add(p);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2570");}
        }
        return setToArray(set);
    }

    private static JSONArray buildAutoFormCols(ClassMetadata cm) throws Exception {
        LinkedHashSet set = new LinkedHashSet();
        addIfHas(cm, set, "kode"); addIfHas(cm, set, "code"); addIfHas(cm, set, "mycode");
        addIfHas(cm, set, "nim"); addIfHas(cm, set, "nama"); addIfHas(cm, set, "namaSiswa");
        String[] props = cm.getPropertyNames();
        for (int i = 0; i < props.length; i++) {
            String p = props[i];
            if (isAutoSkipField(p)) continue;
            try {
                Class rc = cm.getPropertyType(p).getReturnedClass();
                if (isSimpleForAuto(rc) || isModelClass(rc)) set.add(p);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2586");}
        }
        addIfHas(cm, set, "aktif"); addIfHas(cm, set, "keterangan"); addIfHas(cm, set, "deskripsi");
        return setToArray(set);
    }

    private static JSONArray buildAutoSearchCols(ClassMetadata cm, JSONArray table) throws Exception {
        LinkedHashSet set = new LinkedHashSet();
        addIfHas(cm, set, "kode"); addIfHas(cm, set, "code"); addIfHas(cm, set, "mycode");
        addIfHas(cm, set, "nim"); addIfHas(cm, set, "noRegistrasi"); addIfHas(cm, set, "noUjian");
        addIfHas(cm, set, "nama"); addIfHas(cm, set, "namaSiswa");
        addContextProps(cm, set);
        addIfHas(cm, set, "aktif");
        if (set.isEmpty() && table != null) {
            for (int i = 0; i < table.length() && i < 4; i++) set.add(cleanField(table.optString(i)));
        }
        return setToArray(set);
    }

    private static JSONArray labelsFor(JSONArray arr) throws Exception {
        JSONArray labels = new JSONArray();
        if (arr != null) for (int i = 0; i < arr.length(); i++) labels.put(humanize(cleanField(arr.optString(i))));
        return labels;
    }

    private static JSONArray buildLookupColumns(ClassMetadata cm) throws Exception {
        LinkedHashSet set = new LinkedHashSet();
        addIfHas(cm, set, "kode"); addIfHas(cm, set, "code"); addIfHas(cm, set, "mycode");
        addIfHas(cm, set, "nim"); addIfHas(cm, set, "noRegistrasi"); addIfHas(cm, set, "noUjian");
        addIfHas(cm, set, "nama"); addIfHas(cm, set, "namaSiswa");
        addIfHas(cm, set, "keterangan"); addIfHas(cm, set, "deskripsi");
        String[] props = cm.getPropertyNames();
        for (int i = 0; i < props.length && set.size() < 5; i++) {
            String p = props[i];
            if (isAutoSkipField(p)) continue;
            try {
                Class rc = cm.getPropertyType(p).getReturnedClass();
                if (isSimpleForAuto(rc)) set.add(p);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2624");}
        }
        return setToArray(set);
    }

    private static Criterion buildSearchCriterion(ClassMetadata cm, String q) {
        String[] fields = new String[] { "kode", "code", "mycode", "nim", "noRegistrasi", "noUjian", "nama", "namaSiswa" };
        Criterion cr = null;
        for (int i = 0; i < fields.length; i++) {
            try {
                if (hasProperty(cm, fields[i]) && String.class.equals(cm.getPropertyType(fields[i]).getReturnedClass())) {
                    Criterion one = Restrictions.ilike(fields[i], q, MatchMode.ANYWHERE);
                    cr = cr == null ? one : Restrictions.or(cr, one);
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2638");}
        }
        return cr;
    }

    private static void applyBestOrder(Criteria c, ClassMetadata cm) {
        if (hasProperty(cm, "nama")) c.addOrder(Order.asc("nama"));
        else if (hasProperty(cm, "namaSiswa")) c.addOrder(Order.asc("namaSiswa"));
        else if (hasProperty(cm, "kode")) c.addOrder(Order.asc("kode"));
        else c.addOrder(Order.asc(cm.getIdentifierPropertyName()));
    }

    private static void applyDependencyRestrictions(Criteria c, ClassMetadata cm, HttpServletRequest request, Session session) {
        try {
            String fakultasId = nvl(request.getParameter("dep_fakultas"));
            if (!isBlank(fakultasId) && hasProperty(cm, "fakultas")) {
                Class rc = cm.getPropertyType("fakultas").getReturnedClass();
                Object fk = session.get(rc, toIdentifier(HibernateUtil.getClassMetadata(rc), fakultasId));
                if (fk != null) c.add(Restrictions.eq("fakultas", fk));
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2658");}
        try {
            String yayasanId = nvl(request.getParameter("dep_yayasan"));
            if (!isBlank(yayasanId) && hasProperty(cm, "yayasan")) {
                Class rc = cm.getPropertyType("yayasan").getReturnedClass();
                Object fk = session.get(rc, toIdentifier(HibernateUtil.getClassMetadata(rc), yayasanId));
                if (fk != null) c.add(Restrictions.eq("yayasan", fk));
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2666");}
    }

    private static void applyUserContextRestrictions(Criteria criteria, ClassMetadata cm, HttpServletRequest request, Session session) {
        List contexts = getUserContexts(request);
        Set aliases = new HashSet();
        for (int i = 0; i < contexts.size(); i++) {
            UserContext ctx = (UserContext) contexts.get(i);
            if (ctx == null || ctx.value == null) continue;
            String[] props = cm.getPropertyNames();
            for (int p = 0; p < props.length; p++) {
                try {
                    Class rc = cm.getPropertyType(props[p]).getReturnedClass();
                    if (sameModelClass(rc, ctx.value.getClass())) {
                        criteria.add(Restrictions.eq(props[p], ctx.value));
                    }
                    if ("Fakultas".equals(ctx.simpleName) && "jurusan".equalsIgnoreCase(props[p]) && isModelClass(rc)) {
                        ClassMetadata jm = HibernateUtil.getClassMetadata(rc);
                        if (hasProperty(jm, "fakultas")) {
                            String al = "ctx_jurusan_" + p;
                            if (!aliases.contains(al)) { criteria.createAlias(props[p], al); aliases.add(al); }
                            criteria.add(Restrictions.eq(al + ".fakultas", ctx.value));
                        }
                    }
                    if ("Yayasan".equals(ctx.simpleName) && "sekolah".equalsIgnoreCase(props[p]) && isModelClass(rc)) {
                        ClassMetadata sm = HibernateUtil.getClassMetadata(rc);
                        if (hasProperty(sm, "yayasan")) {
                            String al2 = "ctx_sekolah_" + p;
                            if (!aliases.contains(al2)) { criteria.createAlias(props[p], al2); aliases.add(al2); }
                            criteria.add(Restrictions.eq(al2 + ".yayasan", ctx.value));
                        }
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2698");}
            }
        }
    }

    private static JSONObject buildLockedFields(HttpServletRequest request, ClassMetadata cm) throws Exception {
        JSONObject o = new JSONObject();
        List contexts = getUserContexts(request);
        String[] props = cm.getPropertyNames();
        for (int p = 0; p < props.length; p++) {
            for (int i = 0; i < contexts.size(); i++) {
                UserContext ctx = (UserContext) contexts.get(i);
                try {
                    Class rc = cm.getPropertyType(props[p]).getReturnedClass();
                    if (ctx != null && ctx.value != null && sameModelClass(rc, ctx.value.getClass())) {
                        ClassMetadata vcm = HibernateUtil.getClassMetadata(ctx.value.getClass());
                        JSONObject l = new JSONObject();
                        l.put("id", identifierToString(vcm, ctx.value));
                        l.put("label", displayValue(ctx.value));
                        l.put("message", humanize(props[p]) + " terkunci pada " + displayValue(ctx.value));
                        o.put(props[p], l);
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2720");}
            }
        }
        return o;
    }

    private static JSONArray buildContextInfo(HttpServletRequest request, ClassMetadata cm) throws Exception {
        JSONArray a = new JSONArray();
        JSONObject locked = buildLockedFields(request, cm);
        Iterator it = locked.keys();
        while (it.hasNext()) {
            String k = String.valueOf(it.next());
            Object vo = locked.opt(k);
            JSONObject v = vo instanceof JSONObject ? (JSONObject) vo : null;
            if (v != null) a.put(v.optString("message"));
        }
        return a;
    }

    private static void setUserContextValues(Object obj, ClassMetadata cm, HttpServletRequest request) {
        try {
            List contexts = getUserContexts(request);
            String[] props = cm.getPropertyNames();
            for (int p = 0; p < props.length; p++) {
                for (int i = 0; i < contexts.size(); i++) {
                    UserContext ctx = (UserContext) contexts.get(i);
                    try {
                        Class rc = cm.getPropertyType(props[p]).getReturnedClass();
                        if (ctx != null && ctx.value != null && sameModelClass(rc, ctx.value.getClass())) {
                            cm.setPropertyValue(obj, props[p], ctx.value, EntityMode.POJO);
                        }
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2751");}
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2754");}
    }

    private static boolean isLockedByUserContext(String field, ClassMetadata cm, HttpServletRequest request) {
        try {
            JSONObject o = buildLockedFields(request, cm);
            return o.has(field);
        } catch (Exception e) { return false; }
    }

    private static List getUserContexts(HttpServletRequest request) {
        List list = new ArrayList();
        try {
            Object user = null;
            try { user = Common.getCurrentUser(request); } catch (Exception e) { user = Common.getCurrentUser(); }
            if (user == null) return list;
            addContext(list, user, "getSiswa", "Siswa");
            addContext(list, user, "getMahasiswa", "Mahasiswa");
            addContext(list, user, "ambilFakultas", "Fakultas");
            addContext(list, user, "ambilJurusan", "Jurusan");
            addContext(list, user, "ambilSekolah", "Sekolah");
            addContext(list, user, "ambilYayasan", "Yayasan");
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2776");}
        return list;
    }

    private static void addContext(List list, Object user, String methodName, String simpleName) {
        try {
            Method m = user.getClass().getMethod(methodName, new Class[0]);
            Object v = m.invoke(user, new Object[0]);
            if (v != null && hasNonNullId(v)) list.add(new UserContext(simpleName, v));
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2785");}
    }

    private static boolean hasNonNullId(Object v) {
        try {
            Method m = v.getClass().getMethod("getId", new Class[0]);
            Object id = m.invoke(v, new Object[0]);
            return id != null;
        } catch (Exception e) { return false; }
    }

    private static boolean sameModelClass(Class a, Class b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
        if (a.getName().equals(b.getName())) return true;
        if (a.isAssignableFrom(b) || b.isAssignableFrom(a)) return true;
        return false;
    }

    private static boolean isModelClass(Class rc) {
        return rc != null && rc.getName().startsWith("ais.database.model");
    }

    private static boolean isSimpleForAuto(Class rc) {
        if (rc == null) return false;
        return String.class.equals(rc) || Date.class.isAssignableFrom(rc) || Boolean.class.equals(rc) || Boolean.TYPE.equals(rc)
                || Number.class.isAssignableFrom(rc) || Integer.TYPE.equals(rc) || Long.TYPE.equals(rc) || Double.TYPE.equals(rc)
                || Float.TYPE.equals(rc) || BigDecimal.class.equals(rc);
    }

    private static boolean isAutoSkipField(String p) {
        if (isBlank(p)) return true;
        String x = p.toLowerCase();
        return "id".equals(x) || "oleh".equals(x) || "olehid".equals(x) || "tanggal_dirubah".equals(x)
                || "copydari".equals(x) || "class".equals(x) || x.indexOf("password") >= 0 || x.indexOf("pass") == 0
                || x.indexOf("token") >= 0 || x.indexOf("initdata") >= 0;
    }

    private static void addContextProps(ClassMetadata cm, LinkedHashSet set) {
        String[] names = new String[] { "siswa", "mahasiswa", "fakultas", "jurusan", "sekolah", "yayasan" };
        for (int i = 0; i < names.length; i++) addIfHas(cm, set, names[i]);
    }

    private static void addIfHas(ClassMetadata cm, LinkedHashSet set, String p) {
        if (hasProperty(cm, p)) set.add(p);
    }

    private static JSONArray setToArray(LinkedHashSet set) {
        JSONArray a = new JSONArray();
        Iterator it = set.iterator();
        while (it.hasNext()) a.put(String.valueOf(it.next()));
        return a;
    }

    private static String humanize(String s) {
        if (s == null) return "";
        String x = s.replace('_', ' ');
        x = x.replaceAll("([a-z])([A-Z])", "$1 $2");
        if (x.length() == 0) return x;
        return x.substring(0, 1).toUpperCase() + x.substring(1);
    }

    /**
     * Tipe implementasi bersarang {@link UserContext} milik {@link DynamicJspCrudGenerator}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * DynamicJspCrudGenerator}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
     * dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String simpleName}, {@code Object
     * value}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see DynamicJspCrudGenerator
     */
    private static class UserContext {
        String simpleName;
        Object value;
        UserContext(String simpleName, Object value) { this.simpleName = simpleName; this.value = value; }
    }

    private static JSONArray collectAllFieldSpecs(JSONObject conf) {
        JSONArray result = new JSONArray();
        JSONArray formCols = conf.optJSONArray("formCols");
        if (formCols == null) {
            return result;
        }
        for (int i = 0; i < formCols.length(); i++) {
            Object o = formCols.opt(i);
            if (o instanceof JSONArray) {
                JSONArray inner = (JSONArray) o;
                for (int j = 0; j < inner.length(); j++) {
                    result.put(inner.optString(j, ""));
                }
            } else {
                result.put(formCols.optString(i, ""));
            }
        }
        return result;
    }

    private static JSONArray parseFixedOptions(String customType) {
        if (isBlank(customType)) {
            return null;
        }
        String t = customType.trim();
        if (t.startsWith("[") && t.endsWith("]")) {
            try {
                return new JSONArray(t);
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:2881");
            }
        }
        return null;
    }

    private static void normalizeConfigs(JSONArray configs, String randomID) throws Exception {
        for (int i = 0; i < configs.length(); i++) {
            JSONObject c = configs.getJSONObject(i);
            if (i == 0) {
                c.put("randomID", randomID);
            }
            if (!c.has("tabTitle")) {
                c.put("tabTitle", c.optString("dataTypeName", "Data " + (i + 1)));
            }
            if (!c.has("relCol") && c.has("relasi_ke_tabel_utama")) {
                c.put("relCol", c.optString("relasi_ke_tabel_utama", ""));
            }
            if (!c.has("pageSize")) {
                c.put("pageSize", 20);
            }
        }
    }

    private static String getRelationColumn(JSONObject conf) {
        String rel = conf.optString("relCol", "");
        if (isBlank(rel)) {
            rel = conf.optString("relasi_ke_tabel_utama", "");
        }
        return rel;
    }

    private static String cleanField(String spec) {
        if (spec == null) {
            return "";
        }
        String[] p = spec.split(";", 3);
        return p.length > 0 ? p[0].trim() : spec.trim();
    }

    private static String customType(String spec) {
        if (spec == null) {
            return "";
        }
        String[] p = spec.split(";", 3);
        return p.length > 1 ? p[1].trim() : "";
    }

    private static String editorType(String spec) {
        if (spec == null) {
            return "";
        }
        String[] p = spec.split(";", 3);
        return p.length > 2 ? p[2].trim() : "";
    }

    private static String uiType(String spec) {
        String f = cleanField(spec);
        String t = customType(spec);
        if (f.startsWith("file_")) {
            return "file";
        }
        if ("editor".equalsIgnoreCase(t) || "text".equalsIgnoreCase(t) || "textarea".equalsIgnoreCase(t)) {
            return "textarea";
        }
        String lf = f == null ? "" : f.toLowerCase();
        if (lf.indexOf("keterangan") >= 0 || lf.indexOf("deskripsi") >= 0 || lf.indexOf("alamat") >= 0) {
            return "textarea";
        }
        return "text";
    }

    private static boolean hasProperty(ClassMetadata cm, String property) {
        if (isBlank(property) || cm == null) {
            return false;
        }
        if (property.equals(cm.getIdentifierPropertyName())) {
            return true;
        }
        try {
            cm.getPropertyType(property);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Boolean parseBoolean(String value) {
        if (value == null) {
            return Boolean.FALSE;
        }
        String v = value.trim();
        return Boolean.valueOf("true".equalsIgnoreCase(v) || "on".equalsIgnoreCase(v) || "1".equals(v)
                || "ya".equalsIgnoreCase(v) || "y".equalsIgnoreCase(v));
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().length() == 0;
    }

    private static JSONObject ok() throws Exception {
        JSONObject o = new JSONObject();
        o.put("status", "00");
        o.put("message", "OK");
        return o;
    }

    private static JSONObject error(String message) {
        JSONObject o = new JSONObject();
        try {
            o.put("status", "99");
            o.put("message", message == null ? "Terjadi kesalahan" : message);
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:3005");
        }
        return o;
    }

    private static void close(Session session) {
        try {
            if (session != null && session.isOpen()) {
                session.disconnect();
                session.close();
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:3016");
        }
        try {
            HibernateUtil.closeSession();
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DynamicJspCrudGenerator.java:3020");
        }
    }
}
