package ais.common;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.KrsMahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Skripsi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FotoAdmin;
import ais.database.model.file.FotoBiodataCalonMahasiswa;
import ais.database.model.file.FotoCalonPegawai;
import ais.database.model.file.FotoCalonSiswa;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.FotoGambarProduk;
import ais.database.model.file.FotoGuru;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.FotoMahasiswaLulus;
import ais.database.model.file.FotoPegawai;
import ais.database.model.file.FotoSiswa;
import ais.database.model.file.LampiranBeasiswaMahasiswa;
import ais.database.model.file.LampiranKknMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.LampiranLainBiodataCalonMahasiswa;
import ais.database.model.file.LampiranLainMahasiswa;
import ais.database.model.file.LampiranPklMahasiswa;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.file.TugasFileContent;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;

public class CommonJSONUtil {

    // Optimasi: Set statis untuk pencarian cepat (O(1))
    public static final Set<String> STREAMING_CLASSES = new HashSet<String>(Arrays.asList(
            PertemuanFileContent.class.getName(), TugasFileContent.class.getName(), LampiranLain.class.getName(),
            FotoCalonPegawai.class.getName(), FotoGambarProduk.class.getName(), FotoCalonSiswa.class.getName(),
            FotoBiodataCalonMahasiswa.class.getName(), FotoMahasiswaLulus.class.getName(),
            LampiranLainBiodataCalonMahasiswa.class.getName(), LampiranLainMahasiswa.class.getName(),
            FotoMahasiswa.class.getName(), FotoDosen.class.getName(), FotoPegawai.class.getName(),
            FotoGuru.class.getName(), FotoSiswa.class.getName(), FotoAdmin.class.getName(),
            LampiranBeasiswaMahasiswa.class.getName(), LampiranPklMahasiswa.class.getName(),
            LampiranKknMahasiswa.class.getName(), VideoPertemuan.class.getName(), AudioPertemuan.class.getName()
    ));

    // ==========================================
    // SECTION: CONVERT TO LIST / SET
    // ==========================================

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static List convertToList(JSONArray array, Class clazz) {
        List data = new ArrayList();
        if (array == null) return data;

        int s = array.length();
        for (int i = 0; i < s; i++) {
            try {
                JSONObject json = array.getJSONObject(i);
                Class targetClass = clazz;
                // Jika clazz null, coba cari dari properti "class" di JSON
                if (targetClass == null && !json.isNull("class")) {
                    targetClass = Class.forName(json.getString("class"));
                }
                
                if (targetClass != null) {
                    data.add(convertToObject(json, targetClass));
                }
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:93");
            }
        }
        return data;
    }

    @SuppressWarnings("rawtypes")
    public static List convertToList(JSONArray array) {
        return convertToList(array, null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Set convertToSet(JSONArray array, Class clazz) {
        Set data = new HashSet();
        if (array == null) return data;

        int s = array.length();
        for (int i = 0; i < s; i++) {
            try {
                data.add(convertToObject(array.getJSONObject(i), clazz));
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:114");
            }
        }
        return data;
    }

    // ==========================================
    // SECTION: CONVERT TO OBJECT (DESERIALIZATION)
    // ==========================================

    public static GeneralValueObject convertToObject(JSONObject json) {
        return convertToObject(json, null, 0);
    }

    @SuppressWarnings("rawtypes")
    public static GeneralValueObject convertToObject(JSONObject json, Class clazz) {
        return convertToObject(json, clazz, 0);
    }

    @SuppressWarnings("rawtypes")
    public static GeneralValueObject convertToObject(JSONObject json, Class clazz, int depth) {
        if (json == null) return null;

        GeneralValueObject obj = null;
        try {
            // Resolusi Class
            if (clazz == null && !json.isNull("class")) {
                clazz = Class.forName(json.getString("class"));
            }
            if (clazz == null) return null;

            // 1. Coba load existing object dari Database
            obj = loadExistingObject(json, clazz);

            // 2. Jika tidak ada, buat instance baru
            if (obj == null) {
                obj = (GeneralValueObject) clazz.newInstance();
                populateBasicIds(obj, json);
            }

            // 3. Ambil Metadata Hibernate
            ClassMetadata classMetadata = STREAMING_CLASSES.contains(clazz.getName())
                    ? StreamingHibernateUtil.getInstance().getClassMetadata(clazz)
                    : HibernateUtil.getClassMetadata(clazz);

            if (classMetadata == null) return obj;

            // 4. Isi property object
            populateProperties(obj, json, classMetadata, depth);

            // 5. Finalisasi (Custom method legacy)
            try {
                GeneralValueObject.masukkanData(clazz, obj);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonJSONUtil.java:167"); /* Ignore */ }

        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:170");
        }

        return obj;
    }

    // Helper: Load object dari DB
    @SuppressWarnings("rawtypes")
    private static GeneralValueObject loadExistingObject(JSONObject json, Class clazz) {
        try {
            if (!ConstantValues.classExist(clazz)) return null;

            GeneralValueObject obj = null;
            if (!json.isNull("id")) {
                obj = ConstantValues.ambil(clazz.getName(), ais.common.CommonJSONUtil.ambilLong(json,"id"));
            } else if (!json.isNull("userId") && clazz.getName().equals(Tbmuser.class.getName())) {
                obj = ConstantValues.ambil(clazz.getName(), json.getString("userId"));
            } else if (!json.isNull("roleId") && clazz.getName().equals(Tbmrole.class.getName())) {
                obj = ConstantValues.ambil(clazz.getName(), json.getString("roleId"));
            }
            
            // Jika object ditemukan, refresh data
            if (obj != null) {
                GeneralValueObject.masukkanData(clazz, obj);
            }
            return obj;
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:197");
            return null;
        }
    }

    // Helper: Set ID/User/Role untuk object baru
    private static void populateBasicIds(GeneralValueObject obj, JSONObject json) {
        try {
            if (obj instanceof Tbmuser && !json.isNull("userId")) {
                ((Tbmuser) obj).setUserId(json.getString("userId"));
            } else if (obj instanceof Tbmrole && !json.isNull("roleId")) {
                ((Tbmrole) obj).setRoleId(json.getString("roleId"));
            } else if (!json.isNull("id")) {
                obj.setId(ais.common.CommonJSONUtil.ambilLong(json,"id"));
            } 
        } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:212"); }
    }
    
    public static Long ambilLong(JSONObject json, String key) {
//    	try {
//    		return Long.parseLong((json.opt(key)+"").trim());
//    	}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonJSONUtil.java:218");
//			return null;
//		}
    	
    	if (json == null || json.isNull(key)) {
            return null;
        }
        try {
            Object value = json.opt(key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            // Fallback untuk String atau tipe lain
            String strVal = String.valueOf(value).trim();
            if (strVal.isEmpty() || strVal.equalsIgnoreCase("null")) {
                return null;
            }
            return Long.parseLong(strVal);
        } catch (Exception e) {
            return null;
        }
    }

    // Helper: Loop semua property
    private static void populateProperties(GeneralValueObject obj, JSONObject json, ClassMetadata meta, int depth) {
        String[] properties = meta.getPropertyNames();
        for (String key : properties) {
            if ("fileLocation".equalsIgnoreCase(key) || "copyDari".equalsIgnoreCase(key)|| "foto".equalsIgnoreCase(key)) continue;
            if (json.isNull(key)) continue;

            try {
                // Prioritas 1: Sub Object (Relasi)
                if (populateSubObject(obj, json, key, meta, depth)) continue;

                // Prioritas 2: Primitive & Date
                populatePrimitiveOrDate(obj, json, key, meta);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonJSONUtil.java:254");
                // Error 1 field jangan mematikan proses loading field lain
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static boolean populateSubObject(GeneralValueObject obj, JSONObject json, String key, ClassMetadata meta, int depth) throws Exception {
        // FIX PropertyAccessException/ClassCastException: sebagian field HANYA berupa Long id
        // biasa (bukan relasi @ManyToOne), mis. TugasFileContent.mahasiswa. Kalau JSON-nya (ditulis
        // serializer generik) tetap menyertakan "<key>_class"/"<key>.id" seolah relasi, jangan
        // paksa set entity penuh ke field yang tipe deklarasinya bukan GeneralValueObject --
        // biarkan populatePrimitiveOrDate() yang menangani sbg Long/String biasa.
        Type propertyType = meta.getPropertyType(key);
        if (propertyType != null && propertyType.getReturnedClass() != null
                && !GeneralValueObject.class.isAssignableFrom(propertyType.getReturnedClass())) {
            return false;
        }

        GeneralValueObject subobj = null;
        String classKey = key + "_class";

        // Cek ID Relasi
        if (!json.isNull(key + ".id")) {
            String clsName = json.optString(classKey);
            if (clsName != null && !clsName.isEmpty()) {
                subobj = ConstantValues.ambil(clsName, ais.common.CommonJSONUtil.ambilLong(json,key + ".id"));
            }
        } else if (!json.isNull(key + ".userId")) {
            subobj = ConstantValues.ambil(json.getString(classKey), json.getString(key + ".userId"));
        } else if (!json.isNull(key + ".roleId")) {
            subobj = ConstantValues.ambil(json.getString(classKey), json.getString(key + ".roleId"));
        } 
        // Cek File Reference (Rekursif)
        else if (!json.isNull(key + ".file_reference")) {
            File fileRef = new File(json.getString(key + ".file_reference"));
            if (fileRef.exists() && depth < 25) {
                Class clazzData = Class.forName(json.getString(classKey));
                // Baca file JSON referensi
                JSONObject jsonref = new JSONObject(ais.common.BacaTulisUtil.baca(fileRef));
                subobj = convertToObject(jsonref, clazzData, depth + 1);
            }
        }
        // Fallback: key langsung
        else if (!json.isNull(classKey) && ConstantValues.classExist(json.getString(classKey))) {
            Serializable serializable;
            try {
                serializable = ais.common.CommonJSONUtil.ambilLong(json,key);
            } catch (Exception e) {
                serializable = json.getString(key);
            }
            subobj = ConstantValues.ambil(json.getString(classKey), serializable);
        }

        if (subobj != null) {
            meta.setPropertyValue(obj, key, subobj, EntityMode.POJO);
            return true;
        }

        // Object Baru (ID ada tapi di DB tidak ada)
        if (!json.isNull(key + ".id")) {
             Class clazzData = Class.forName(json.getString(classKey));
             subobj = (GeneralValueObject) clazzData.newInstance();
             subobj.setId(ais.common.CommonJSONUtil.ambilLong(json,key + ".id"));
             meta.setPropertyValue(obj, key, subobj, EntityMode.POJO);
             return true;
        }

        return false;
    }

    private static void populatePrimitiveOrDate(GeneralValueObject obj, JSONObject json, String key, ClassMetadata meta) throws Exception {
        String classKey = key + "_class";
        // Default string jika class tidak ada
        if (json.isNull(classKey)) {
             meta.setPropertyValue(obj, key, json.getString(key), EntityMode.POJO);
             return;
        }
        
        String typeName = json.getString(classKey);

        if (typeName.equals(Integer.class.getName())) {
            meta.setPropertyValue(obj, key, json.getInt(key), EntityMode.POJO);
        } else if (typeName.equals(Double.class.getName())) {
            meta.setPropertyValue(obj, key, json.getDouble(key), EntityMode.POJO);
        } else if (typeName.equals(Boolean.class.getName())) {
            meta.setPropertyValue(obj, key, json.getBoolean(key), EntityMode.POJO);
        } else if (typeName.equals(Long.class.getName())) {
            meta.setPropertyValue(obj, key, ais.common.CommonJSONUtil.ambilLong(json,key), EntityMode.POJO);
        } else if (typeName.equals(Date.class.getName())) {
            Date d = parseDate(json, key, obj);
            meta.setPropertyValue(obj, key, d, EntityMode.POJO);
        } else if (isEntityRelationType(typeName)) {
            // Properti relasi (mis. FormatNilai.kunci -> Tbmuser) yang lolos dari populateSubObject
            // (tidak ada key+".id"/".userId"/".roleId": Tbmuser.getId() selalu null krn PK aslinya
            // userId, jadi saat serialisasi handleSubObjectToJson hanya menulis key+"_class" & key
            // mentah = userId via putIdUserRoleForKey). Resolusi sama seperti fallback di
            // populateSubObject: pakai Long id jika numerik, kalau tidak pakai String (co.
            // userId/roleId). Sebelumnya jatuh ke branch String default di bawah dan meng-set
            // object relasi dengan raw String -> ClassCastException dari Hibernate setter.
            Serializable idVal = ais.common.CommonJSONUtil.ambilLong(json, key);
            if (idVal == null) {
                idVal = json.getString(key);
            }
            // FIX PropertyAccessException/ClassCastException (mis. TugasFileContent.mahasiswa):
            // "<key>_class" bisa menandai tipe entity (data lama/skema beda) padahal properti
            // TUJUAN sekarang deklarasinya Long biasa (bukan relasi). Cek tipe deklarasi properti
            // dulu -- kalau bukan turunan GeneralValueObject, cukup set id numeriknya saja,
            // jangan paksa set objek entity yang akan ditolak Hibernate setter.
            Type propertyTypeRel = meta.getPropertyType(key);
            if (propertyTypeRel != null && propertyTypeRel.getReturnedClass() != null
                    && !GeneralValueObject.class.isAssignableFrom(propertyTypeRel.getReturnedClass())) {
                if (idVal instanceof Long) {
                    meta.setPropertyValue(obj, key, idVal, EntityMode.POJO);
                }
                return;
            }
            Object subobj = ConstantValues.ambil(typeName, idVal);
            if (subobj != null) {
                meta.setPropertyValue(obj, key, subobj, EntityMode.POJO);
            }
        } else {
            meta.setPropertyValue(obj, key, json.getString(key), EntityMode.POJO);
        }
    }

    private static boolean isEntityRelationType(String typeName) {
        try {
            Class<?> c = Class.forName(typeName);
            return GeneralValueObject.class.isAssignableFrom(c);
        } catch (Exception e) {
            return false;
        }
    }

    private static Date parseDate(JSONObject json, String key, GeneralValueObject obj) {
        try {
            if (!json.isNull(key + "_milis_str")) {
                return new Date(Long.parseLong(json.getString(key + "_milis_str")));
            }
            
            String dateStr = json.getString(key);
            boolean isDateFormat1 = checkTanggalAtauTimeStamp(obj, key);
            
            try {
                return isDateFormat1 ? Common.dateFormat1.get().parse(dateStr) : Common.dateFormat3.get().parse(dateStr);
            } catch (Exception e) {
                 return isDateFormat1 ? Common.dateFormat3.get().parse(dateStr) : Common.dateFormat1.get().parse(dateStr);
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:355");
            return null;
        }
    }

    // ==========================================
    // SECTION: CONVERT TO JSON (SERIALIZATION)
    // ==========================================

    @SuppressWarnings("rawtypes")
    public static JSONArray convertToJson(Collection obj, String... clazzPengecualian) {
        JSONArray array = new JSONArray();
        if (obj == null) return array;

        for (Object o : obj) {
            try {
                array.put(convertToJsonObject((GeneralValueObject) o, clazzPengecualian));
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:373");
            }
        }
        return array;
    }

    public static JSONObject convertToJsonObject(GeneralValueObject obj, String... clazzPengecualian) {
        return convertToJsonObject(0, obj, clazzPengecualian);
    }

    public static JSONObject convertToJsonObject(Integer indexke, GeneralValueObject obj, String... clazzPengecualian) {
        JSONObject ob = new JSONObject();
        if (obj == null) return ob;

        // 1. Jika Master Data / Class sederhana, ambil ID saja
        if (ConstantValues.classExist(obj.getClass())) {
            putIdUserRole(ob, obj);
            return ob;
        }

        // 2. Metadata Check
        boolean isStreaming = STREAMING_CLASSES.contains(obj.getClass().getName());
        ClassMetadata classMetadata = isStreaming
                ? StreamingHibernateUtil.getInstance().getClassMetadata(obj.getClass())
                : HibernateUtil.getClassMetadata(obj.getClass());

        // 3. Handle FileFoto (URL Gdrive/Local)
        if (isStreaming && obj instanceof FileFoto) {
            try {
                FileFoto fileFoto = (FileFoto) obj;
                String path = (fileFoto.getGdrive() != null) ? fileFoto.downloadGDriveUrl() : fileFoto.ambilFile().getAbsolutePath();
                ob.put("lokasi_file_absolut", path);
            } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:405"); }
        }

        putIdUserRole(ob, obj);

        // 4. Populate JSON Properties
        if (classMetadata != null) {
            String[] props = classMetadata.getPropertyNames();
            for (String key : props) {
                if ("fileLocation".equalsIgnoreCase(key)||(isStreaming && "foto".equalsIgnoreCase(key))) continue;

                try {
                    Object subContent = classMetadata.getPropertyValue(obj, key, EntityMode.POJO);
                    if (subContent == null) continue;

                    Type type = classMetadata.getPropertyType(key);
                    String returnedClassName = type.getReturnedClass().getName();

                    if (isExcluded(returnedClassName, clazzPengecualian)) continue;

                    ob.put(key + "_class", returnedClassName);

                    if (subContent instanceof GeneralValueObject) {
                        handleSubObjectToJson(ob, key, (GeneralValueObject) subContent, indexke, clazzPengecualian);
                    } else if (subContent instanceof Date) {
                        handleDateToJson(ob, key, (Date) subContent, obj);
                    } else {
                        ob.put(key, subContent);
                    }
                } catch (Exception e) {
                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:435");
                }
            }
        }
        return ob;
    }

    public static JSONObject convertToJsonObjectSimple(GeneralValueObject obj, int dept) {
        JSONObject ob = new JSONObject();
        if (obj == null) return ob;

        boolean isStreaming = isStreamingClass(obj);

        ClassMetadata classMetadata = isStreaming 
                ? StreamingHibernateUtil.getInstance().getClassMetadata(obj.getClass())
                : HibernateUtil.getClassMetadata(obj.getClass());

        // Handle FileFoto
        if (isStreaming && obj instanceof FileFoto) {
            try {
                FileFoto fileFoto = (FileFoto) obj;
                if (fileFoto.ambilFile() != null) {
                    ob.put("lokasi_file_absolut", fileFoto.ambilFile().getAbsolutePath());
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonJSONUtil.java:459"); /* Silent fail */ }
        }

        // Handle Basic IDs
        putIdUserRole(ob, obj);

        // Handle Properties
        if (classMetadata != null) {
            String[] propertyNames = classMetadata.getPropertyNames();

            for (String key : propertyNames) {
                if ("fileLocation".equalsIgnoreCase(key)) continue;

                try {
                    Object subContent = classMetadata.getPropertyValue(obj, key, EntityMode.POJO);
                    if (subContent == null) continue;

                    Class<?> returnedClass = classMetadata.getPropertyType(key).getReturnedClass();
                    String className = returnedClass.getName();

                    ob.put(key + "_class", className);

                    if (String.class.isAssignableFrom(returnedClass)) {
                        ob.put(key, subContent.toString());
                    } else if (Date.class.isAssignableFrom(returnedClass)) {
                        handleDateSimple(ob, key, (Date) subContent, obj);
                    } else if (Number.class.isAssignableFrom(returnedClass) || Boolean.class.isAssignableFrom(returnedClass)) {
                        ob.put(key, subContent);
                    } else if (subContent instanceof GeneralValueObject && dept < 2) {
                        // RECURSION logic
                        GeneralValueObject subObj = (GeneralValueObject) subContent;
                        
                        // FIX: Gunakan dept + 1, jangan ubah variabel dept dengan ++
                        JSONObject subData = convertToJsonObjectSimple(subObj, dept + 1);
                        
                        ob.put(key + "_data", subData);
                        ob.put(key + ".id", subObj.getId());
                        putIdUserRoleForKey(ob, key, subObj);
                    }

                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonJSONUtil.java:499"); /* Ignore single field error */ }
            }
        }
        return ob;
    }

    // ==========================================
    // SECTION: UTILITIES (FILE & HELPERS)
    // ==========================================

    private static void putIdUserRole(JSONObject ob, GeneralValueObject obj) {
        try {
            if (obj instanceof Tbmuser) ob.put("userId", ((Tbmuser) obj).getUserId());
            else if (obj instanceof Tbmrole) ob.put("roleId", ((Tbmrole) obj).getRoleId());
            else ob.put("id", obj.getId());
        } catch (JSONException e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:514"); }
    }
    
    private static void putIdUserRoleForKey(JSONObject ob, String key, GeneralValueObject obj) throws JSONException {
        if (obj instanceof Tbmuser) ob.put(key, ((Tbmuser) obj).getUserId());
        else if (obj instanceof Tbmrole) ob.put(key, ((Tbmrole) obj).getRoleId());
        else ob.put(key, obj.getId());
    }

    private static void handleSubObjectToJson(JSONObject ob, String key, GeneralValueObject subContent, int indexke, String[] exceptions) throws Exception {
        if (ConstantValues.classExist(subContent.getClass())) {
            ob.put(key + ".id", subContent.getId());
            putIdUserRoleForKey(ob, key, subContent);
        } else {
            // Write complex object to file reference
            ob.put(key + "_class_object", true);
            String ref = subContent.write(indexke + 1, exceptions).getAbsolutePath();
            ob.put(key + ".file_reference", ref);
            ob.put(key, subContent.getId());
        }
    }
    
    private static void handleDateToJson(JSONObject ob, String key, Date date, GeneralValueObject parentObj) throws JSONException {
        ob.put(key + "_milis_str", String.valueOf(date.getTime()));
        try {
            if (checkTanggalAtauTimeStamp(parentObj, key)) {
                ob.put(key, Common.dateFormat1.get().format(date));
            } else {
                ob.put(key, Common.dateFormat3.get().format(date));
            }
        } catch (Exception e) { ob.put(key, date.toString()); }
    }

    private static void handleDateSimple(JSONObject ob, String key, Date date, GeneralValueObject obj) {
        try {
            ob.put(key + "_milis_str", String.valueOf(date.getTime()));
            if (Common.checkTanggalAtauTimeStamp(obj, key)) {
                ob.put(key, Common.dateFormat1.get().format(date));
            } else {
                ob.put(key, Common.dateFormat3.get().format(date));
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonJSONUtil.java:555"); /* Silent */ }
    }

    private static boolean isExcluded(String className, String[] exceptions) {
        if (exceptions == null) return false;
        for (String s : exceptions) {
            if (s.equalsIgnoreCase(className)) return true;
        }
        return false;
    }

    private static boolean isStreamingClass(GeneralValueObject obj) {
        return STREAMING_CLASSES.contains(obj.getClass().getName());
    }

    public static boolean checkTanggalAtauTimeStamp(GeneralValueObject obj, String key) {
        if (obj == null || key == null) return false;
        String k = key.toLowerCase();

        if (obj instanceof Pertemuan) return k.equals("tanggal") || k.equals("tanggalrealisasi");
        if (obj instanceof Perkuliahan) return k.equals("perkuliahandimulai") || k.equals("perkuliahansampai") || k.equals("tanggalmulaiperkuliahan");
        if (obj instanceof KelompokKkn || obj instanceof KelompokPkl) return k.equals("tanggal_mulai") || k.equals("tanggal_selesai");
        if (obj instanceof MahasiswaRequestTugasAkhir) return k.equals("tanggalawalbimbingan") || k.equals("tanggalakhirbimbingan");
        if (obj instanceof Skripsi) return k.equals("tanggalsidang") || k.equals("tanggalseminar") || k.equals("tglsk");
        if (obj instanceof KrsMahasiswa) return k.equals("tanggalawalbimbingan");

        return false;
    }

	// Cache direktori yang SUDAH dipastikan ada. getFileLocation() dipanggil RIBUAN kali
	// (retreive/put/FileFotoLain/branding); sebelumnya tiap panggilan melakukan
	// parent.exists()+mkdirs() yang sangat lambat di filesystem lambat/mount jaringan dan
	// menyebabkan startup macet. Dengan cache, exists()/mkdirs() hanya sekali per direktori.
	private static final java.util.Set<String> ENSURED_DIRS =
			java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String, Boolean>());

	private static void ensureParentDir(File file) {
		if (file == null) {
			return;
		}
		File parent = file.getParentFile();
		if (parent == null) {
			return;
		}
		String path = parent.getPath();
		if (ENSURED_DIRS.contains(path)) {
			return;
		}
		try {
			if (!parent.exists()) {
				parent.mkdirs();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonJSONUtil.java:607");
		}
		ENSURED_DIRS.add(path);
	}

    @SuppressWarnings("rawtypes")
    public static File getFileLocation(Class clazz, Serializable id, String key) {
        String lokasiFileTemprorary = ConstantValues.ambilLokasiFileTemprorary(clazz);
        // Clean Logic: ID Tbmuser/Tbmrole logic sama dengan default
        String fileLocation = lokasiFileTemprorary + clazz.getSimpleName() + "/" + id + "/" + key + ".json";
        
        File file = new File(fileLocation);
        ensureParentDir(file);
        return file;
    }

    @SuppressWarnings("rawtypes")
    public static File getFileLocation(GeneralValueObject obj, String key) {
        if (obj == null) return null;
        Class clazz = obj.getClass();
        String lokasi = ConstantValues.ambilLokasiFileTemprorary(clazz);
        
        // Default Logic
        String suffix = "/" + obj.getId() + "/" + key + ".json";
        
        // Custom Override Logic (Sesuai kode legacy)
        if (obj instanceof Tbmuser) {
             suffix = "/" + ((Tbmuser) obj).getUserId() + "/" + key + ".json";
        } else if (obj instanceof Tbmrole) {
             suffix = "/" + ((Tbmrole) obj).getRoleId() + "/" + key + ".json";
        } else if (obj instanceof Perkuliahan) {
             Perkuliahan p = (Perkuliahan) obj;
             long fakId = (p.getJurusan() != null && p.getJurusan().getFakultas() != null) ? p.getJurusan().getFakultas().getId() : 0;
             long jurId = (p.getJurusan() != null) ? p.getJurusan().getId() : 0;
             suffix = "/" + fakId + "/" + jurId + "/" + p.getTahunAjaran() + "/" + p.getSemester() + "/" + key + ".json";
        }
        // Tambahkan else if untuk class lain seperti DetailPerkuliahan jika diperlukan...

        File file = new File(lokasi + clazz.getSimpleName() + suffix);
        ensureParentDir(file);
        return file;
    }

    public static File setJSONTemporary(GeneralValueObject obj, String key, JSONObject jsonObject) {
        if (obj == null || jsonObject == null || key == null) return null;

        try {
            jsonObject.put("class", obj.getClass().getName());
            File file = CommonJSONUtil.getFileLocation(obj, key);
            if (file != null) {
                ais.common.BacaTulisUtil.tulis(file, jsonObject.toString());
                return file;
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:661");
            System.err.println("Error saving JSON temp: " + obj.getClass().getSimpleName());
        }
        return null;
    }

    public static JSONObject getJSONTemporary(GeneralValueObject obj, String key) {
        if (obj == null || key == null) return new JSONObject();

        try {
            File file = CommonJSONUtil.getFileLocation(obj, key);
            if (file != null && file.exists()) {
                String content = ais.common.BacaTulisUtil.baca(file);
                if (content != null && !content.trim().isEmpty()) {
                    return new JSONObject(content);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:679");
        }
        return new JSONObject();
    }

    // Pola pengenal URL di dalam teks bebas: mulai dari "http://", "https://", atau "www."
    // dan berhenti di karakter yang jelas BUKAN bagian URL -- whitespace, kutip ganda/tunggal,
    // dan tanda kurung siku HTML (< >) -- supaya fragmen HTML mentah yang tertangkap
    // (mis. href="https://...">Klik) tidak ikut terbawa sebagai satu "URL".
    private static final java.util.regex.Pattern URL_TOKEN_PATTERN = java.util.regex.Pattern.compile(
            "(?i)(https?://[^\\s\"'<>]+|www\\.[^\\s\"'<>]+)");

    public static List<String> getUrls(String s) {
        List<String> urls = new ArrayList<String>();
        if (s == null) return urls;

        java.util.regex.Matcher matcher = URL_TOKEN_PATTERN.matcher(s);
        while (matcher.find()) {
            String item = matcher.group();
            // Buang tanda baca penutup yang mungkin ikut tertangkap di ujung URL
            // (mis. titik/koma akhir kalimat, atau kurung tutup dari teks sekitarnya)
            item = item.replaceAll("[)\\]\\.,;:]+$", "");
            if (item.isEmpty()) {
                continue;
            }
            // Setiap kandidat diproses dalam try-catch tersendiri (per-item) sehingga satu
            // kandidat URL yang gagal di-parse TIDAK menggagalkan ekstraksi URL lain dalam
            // teks yang sama.
            try {
                if (item.contains("drive.google.com")) {
                    item = item.replace(",", "_");
                }
                String itemLower = item.toLowerCase();
                String candidate = item;
                if (!itemLower.startsWith("http://") && !itemLower.startsWith("https://")) {
                    // Domain tanpa protokol (mis. "www.4shared.com") -- tambahkan "https://" secara
                    // otomatis supaya link tetap terdeteksi valid dan bisa diklik, bukan dilewati.
                    candidate = "https://" + item;
                }
                new URL(candidate);
                urls.add(candidate);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonJSONUtil.java:696"); /* Not a URL */ }
        }
        return urls;
    }
}