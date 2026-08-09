package ais.action.master.generic.v2.adapter;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Date;
import java.io.FileOutputStream;
import java.io.FileInputStream;

import org.apache.commons.lang.StringUtils;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.common.Common;
import ais.common.UploadReportHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.KrsMahasiswa;
import ais.database.model.HistoryStatusMahasiswa;
import ais.action.master.helper.HistoryStatusMahasiswaUtil;
import ais.ui.util.DataCriteria;
import ais.common.ConstantValues;
import ais.action.master.MahasiswaAction;
import ais.action.master.EksporFromFeederAction;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.helper.impor.ImportFromEpsbedHelper;
import ais.database.hibernate.OjsHibernateUtil;
import ais.database.model.ojs.Journals;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.LampiranLainMahasiswa;
import org.hibernate.criterion.Order;
import org.apache.commons.io.IOUtils;

/**
 * Implementasi headless operasi Excel Mahasiswa. Format kolom dan aturan mutasi
 * sengaja sama dengan onUpload/DownloadPassword dan onUpload/DownloadRfid pada
 * MahasiswaAction; kelas ini hanya mengganti transport ZK Media/Filedownload.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class MahasiswaExistingBulkOperationService {
    public static final class Download {
        private final String fileName;
        private final byte[] bytes;
        Download(String fileName, byte[] bytes) { this.fileName = fileName; this.bytes = bytes; }
        public String getFileName() { return fileName; }
        public byte[] getBytes() { return bytes; }
    }

    private MahasiswaExistingBulkOperationService() { }

    public static Download downloadPassword(GenericCrudRequestContext context) throws Exception {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = scopedCriteria(session, context);
            criteria.setProjection(Projections.projectionList()
                    .add(Projections.property("nim")).add(Projections.property("pass")));
            List rows = criteria.list();
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("DATA");
            sheet.setDefaultColumnWidth(20);
            XSSFRow header = sheet.createRow(0);
            header.createCell(0).setCellValue("NIM");
            header.createCell(1).setCellValue("PASSWORD");
            int rowIndex = 1;
            for (int i = 0; i < rows.size(); i++) {
                Object[] value = (Object[]) rows.get(i);
                XSSFRow row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(text(value[0]));
                String encrypted = text(value[1]);
                row.createCell(1).setCellValue(encrypted.length() == 0 ? "" : Common.desEncrypter.get().decrypt(encrypted));
            }
            return new Download("password_mahasiswa.xlsx", bytes(workbook));
        } finally { close(session); }
    }

    public static Download downloadRfid(GenericCrudRequestContext context) throws Exception {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = scopedCriteria(session, context);
            criteria.setProjection(Projections.projectionList().add(Projections.property("nim"))
                    .add(Projections.property("idfinger")).add(Projections.property("nama")));
            List rows = criteria.list();
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("DATA");
            sheet.setDefaultColumnWidth(20);
            XSSFRow header = sheet.createRow(0);
            header.createCell(0).setCellValue("NIM");
            header.createCell(1).setCellValue("ID Finger/RFID");
            header.createCell(2).setCellValue("Nama");
            for (int i = 0; i < rows.size(); i++) {
                Object[] value = (Object[]) rows.get(i);
                XSSFRow row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(text(value[0]));
                row.createCell(1).setCellValue(text(value[1]));
                row.createCell(2).setCellValue(text(value[2]));
            }
            return new Download("rfid_mahasiswa.xlsx", bytes(workbook));
        } finally { close(session); }
    }

    public static java.io.File uploadPassword(InputStream input, GenericCrudRequestContext context) throws Exception {
        return upload(input, context, true);
    }

    public static java.io.File uploadRfid(InputStream input, GenericCrudRequestContext context) throws Exception {
        return upload(input, context, false);
    }

    /** Salinan headless inti onUploadUKT; kolom tetap 0=Mahasiswa dan 2=Status Awal. */
    public static java.io.File uploadUkt(InputStream input, GenericCrudRequestContext context) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook(input); XSSFSheet sheet = workbook.getSheetAt(0);
        UploadReportHelper report = new UploadReportHelper("Upload UKT Mahasiswa");
        Set<Long> allowed = scopedIds(context);
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Session session = null; Transaction transaction = null;
            try {
                session = HibernateUtil.currentNativeSession();
                Mahasiswa mahasiswa = (Mahasiswa) Common.getSheetContentAsObject(sheet, 0, i, Mahasiswa.class);
                StatusAwalMahasiswa status = (StatusAwalMahasiswa) Common.getSheetContentAsObject(sheet, 2, i, StatusAwalMahasiswa.class);
                if (mahasiswa == null || status == null) continue;
                if (!allowed.contains(mahasiswa.getId())) { report.gagal(i, mahasiswa.getNim(), "Mahasiswa berada di luar scope PT aktif", "Periksa data dan perguruan tinggi aktif."); continue; }
                mahasiswa.setStatusAwalMahasiswa(status);
                transaction = session.beginTransaction(); session.update(mahasiswa); transaction.commit();
                report.sukses(i, mahasiswa.getNim() + " - " + mahasiswa.getNama(), "UKT diperbarui");
            } catch (Exception error) {
                if (transaction != null && transaction.isActive()) transaction.rollback();
                report.gagal(i, "baris-" + i, error, "Periksa data UKT pada baris ini.");
            } finally { HibernateUtil.closeSession(); }
        }
        return report.simpanLaporan();
    }

    /** Salinan headless inti onUploadStatus dengan kalkulasi semester existing. */
    public static java.io.File uploadStatus(InputStream input, GenericCrudRequestContext context) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook(input); XSSFSheet sheet = workbook.getSheetAt(0);
        UploadReportHelper report = new UploadReportHelper("Upload Status Mahasiswa");
        Set<Long> allowed = scopedIds(context);
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Session session = null; Transaction transaction = null;
            try {
                session = HibernateUtil.currentNativeSession();
                Mahasiswa mahasiswa = (Mahasiswa) Common.getSheetContentAsObject(sheet, 1, i, Mahasiswa.class);
                StatusMahasiswa status = (StatusMahasiswa) Common.getSheetContentAsObject(sheet, 4, i, StatusMahasiswa.class);
                String tahunAkademik = Common.getSheetContentAsString(sheet, 5, i);
                String ganjilGenap = Common.getSheetContentAsString(sheet, 6, i);
                Integer semester = Common.getSheetContentAsInteger(sheet, 7, i);
                if (mahasiswa != null && ganjilGenap != null && ganjilGenap.trim().length() > 0 && tahunAkademik != null) {
                    Integer tahun = Integer.valueOf(StringUtils.split(tahunAkademik, "/")[0]);
                    semester = Common.getSemester(mahasiswa.getTahunangkatan(), ganjilGenap,
                            mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());
                }
                Date tanggalStatus = Common.getSheetContentAsDate(sheet, 8, i);
                StatusAwalMahasiswa statusAwal = (StatusAwalMahasiswa) Common.getSheetContentAsObject(sheet, 9, i, StatusAwalMahasiswa.class);
                String keterangan = Common.getSheetContentAsString(sheet, 10, i);
                if (mahasiswa == null || status == null || tahunAkademik == null || tahunAkademik.trim().length() == 0 || semester == null) continue;
                if (!allowed.contains(mahasiswa.getId())) { report.gagal(i, mahasiswa.getNim(), "Mahasiswa berada di luar scope PT aktif", "Periksa data dan perguruan tinggi aktif."); continue; }
                KrsMahasiswa krs = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);
                HistoryStatusMahasiswa history = HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krs, true);
                history.setStatusMahasiswa(status); history.setTanggalStatus(tanggalStatus);
                history.setStatusAwalMahasiswa(statusAwal); history.setKeterangan(keterangan);
                transaction = session.beginTransaction(); Common.refreshSaveOrUpdate(session, history); transaction.commit();
                report.sukses(i, mahasiswa.getNim(), "Status mahasiswa diperbarui");
            } catch (Exception error) {
                if (transaction != null && transaction.isActive()) transaction.rollback();
                report.gagal(i, "baris-" + i, error, "Periksa data status pada baris ini.");
            } finally { HibernateUtil.closeSession(); }
        }
        return report.simpanLaporan();
    }

    /** Headless onSynchronizeStatus: memakai CommonAcademicSyncHelper existing. */
    public static java.io.File synchronizeStatus(final GenericCrudRequestContext context,
            final String tahunAkademik, final String semester, final int mulai, final int sampai,
            boolean statusMahasiswa, boolean statusKrs, boolean perkuliahan, boolean pembayaran,
            boolean nonAktifkan, boolean reloadTemporary) throws Exception {
        if (reloadTemporary) ConstantValues.reInitDataDiMemory(true);
        final List mahasiswa = scopedEntities(context, mulai, sampai);
        ais.common.LaporanUpload report = Common.singkronisasiStatusMahasiswa(null, new DataCriteria() {
            public Object initCriteria(boolean order) { return mahasiswa; }
        }, tahunAkademik, semester, Integer.valueOf(mulai), Integer.valueOf(sampai),
                statusMahasiswa, statusKrs, perkuliahan, pembayaran, nonAktifkan);
        return report.tulisBerkas();
    }

    /** Headless onImport: tetap memakai parser EPSBED dan mahasiswa.sql existing. */
    public static java.io.File importEpsbed(InputStream input, String fileName,
            GenericCrudRequestContext context) throws Exception {
        if (fileName == null || !"MSMHS.DBF".equalsIgnoreCase(new java.io.File(fileName).getName()))
            throw new GenericCrudException(400, "MSMHS_REQUIRED", "Berkas harus bernama MSMHS.DBF.");
        java.io.File directory = new java.io.File(Common.REAL_PATH, "temp"); directory.mkdirs();
        java.io.File source = java.io.File.createTempFile("epsbed_mahasiswa_", "_MSMHS.DBF", directory);
        FileOutputStream output = new FileOutputStream(source);
        try { byte[] buffer = new byte[8192]; int read; while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read); }
        finally { output.close(); }
        ImportFromEpsbedHelper.doImport(source, null, null, null);
        Session session = null; Transaction transaction = null;
        try {
            session = HibernateUtil.currentNativeSession(); transaction = session.beginTransaction();
            session.createSQLQuery(ImportFromEpsbedHelper.read("mahasiswa.sql")).executeUpdate();
            transaction.commit();
        } catch (Exception error) {
            if (transaction != null && transaction.isActive()) transaction.rollback(); throw error;
        } finally { HibernateUtil.closeSession(); }
        UploadReportHelper report = new UploadReportHelper("Import EPSBED Mahasiswa");
        report.sukses(1, "MSMHS.DBF", "Import dan transformasi mahasiswa.sql selesai");
        return report.simpanLaporan();
    }

    /** Headless tombol Export ke OJS; updateUser tetap menjadi satu sumber logika. */
    public static java.io.File exportOjs(GenericCrudRequestContext context) throws Exception {
        if (!Common.bolehKonfigurasi("terhubung_ke_ojs", ais.database.model.Konfigurasi.TIDAK_AKTIF))
            throw new GenericCrudException(403, "OJS_DISABLED", "Integrasi OJS tidak diaktifkan pada konfigurasi.");
        List mahasiswa = scopedEntities(context, Integer.MIN_VALUE, Integer.MAX_VALUE);
        org.hibernate.Session ojs = null; UploadReportHelper report = new UploadReportHelper("Export Mahasiswa ke OJS");
        try {
            ojs = OjsHibernateUtil.getInstance().currentSession();
            List journals = ojs.createCriteria(Journals.class).list();
            for (int i = 0; i < mahasiswa.size(); i++) {
                Mahasiswa value = (Mahasiswa) mahasiswa.get(i);
                try { MahasiswaAction.updateUser(ojs, value, journals); report.sukses(i + 1, value.getNim(), "User OJS diperbarui"); }
                catch (Exception error) { report.gagal(i + 1, value.getNim(), error, "Periksa konfigurasi dan koneksi OJS."); }
            }
        } finally { try { OjsHibernateUtil.getInstance().closeSession(); } catch (Exception ignored) { } }
        return report.simpanLaporan();
    }

    /** Headless tombol Krm ke Feeder; koneksi dan exportKeFeeder sama dengan Action existing. */
    public static java.io.File exportFeeder(GenericCrudRequestContext context) throws Exception {
        if (!Common.getApakahAdminBolehAksesFeeder()
                || !Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder"))
            throw new GenericCrudException(403, "FEEDER_DISABLED", "Integrasi langsung Neo Feeder tidak diizinkan.");
        String[] connection = EksporFromFeederAction.koneksi();
        String ip = connection[0], port = connection[1], username = connection[2], password = connection[3], url = connection[4];
        if (!EksporFromFeederAction.exists(url)) throw new GenericCrudException(503, "FEEDER_UNAVAILABLE", "Server Neo Feeder tidak merespons: " + url);
        FeederConnector connector = new FeederConnector(ip, Integer.parseInt(port), null);
        String token = connector.getToken(username, password);
        if (token == null || token.trim().length() == 0 || token.trim().toLowerCase().startsWith("error"))
            throw new GenericCrudException(502, "FEEDER_LOGIN_FAILED", "Login Neo Feeder gagal.");
        FeederExporter exporter = new FeederExporter(connector, token, null, null, null);
        List mahasiswa = scopedEntities(context, Integer.MIN_VALUE, Integer.MAX_VALUE);
        UploadReportHelper report = new UploadReportHelper("Kirim Mahasiswa ke Neo Feeder");
        for (int i = 0; i < mahasiswa.size(); i++) {
            Mahasiswa value = (Mahasiswa) mahasiswa.get(i); List<String> errors = new ArrayList<String>();
            try {
                MahasiswaAction.exportKeFeeder(value, exporter, token, connector, errors);
                if (errors.isEmpty()) report.sukses(i + 1, value.getNim(), "Biodata dan riwayat pendidikan diproses");
                else report.gagal(i + 1, value.getNim(), join(errors), "Periksa validasi data PDDikti lalu kirim ulang.");
            } catch (Exception error) { report.gagal(i + 1, value.getNim(), error, "Periksa koneksi dan data Neo Feeder."); }
        }
        return report.simpanLaporan();
    }

    /** Headless onDownloadLampiran, dibatasi daftar Mahasiswa hasil scope adapter. */
    public static java.io.File downloadAttachments(GenericCrudRequestContext context) throws Exception {
        List mahasiswa = scopedEntities(context, Integer.MIN_VALUE, Integer.MAX_VALUE);
        java.io.File root = java.nio.file.Files.createTempDirectory("lampiran_mahasiswa_").toFile();
        String[] kinds = new String[] { LampiranLainMahasiswa.IJAZAH, LampiranLainMahasiswa.TRANSKRIP_NILAI,
                LampiranLainMahasiswa.KTP, LampiranLainMahasiswa.AKTE,
                LampiranLainMahasiswa.SURAT_PENUNJUKAN_PENGURUS_ORGANISASI, LampiranLainMahasiswa.NPWP,
                LampiranLainMahasiswa.KK, LampiranLainMahasiswa.KTP_AYAH, LampiranLainMahasiswa.KTP_IBU,
                LampiranLainMahasiswa.KTP_WALI, LampiranLainMahasiswa.LAMPIRAN_1, LampiranLainMahasiswa.LAMPIRAN_2,
                LampiranLainMahasiswa.LAMPIRAN_3, LampiranLainMahasiswa.LAMPIRAN_4, LampiranLainMahasiswa.LAMPIRAN_5 };
        for (int i = 0; i < mahasiswa.size(); i++) {
            Mahasiswa value = (Mahasiswa) mahasiswa.get(i);
            java.io.File folder = new java.io.File(root, safe(value.getNim() + "_" + value.getNama())); folder.mkdirs();
            copyPhoto(value, folder);
            for (int k = 0; k < kinds.length; k++) copyStudentAttachment(value, kinds[k], folder);
            copyAdditionalAttachments(value, folder);
        }
        java.io.File zip = new java.io.File(root.getParentFile(), root.getName() + ".zip");
        Common.zipDir(zip.getAbsolutePath(), root.getAbsolutePath());
        return zip;
    }

    private static void copyPhoto(Mahasiswa mahasiswa, java.io.File folder) {
        org.hibernate.Session streaming = null;
        try {
            streaming = StreamingHibernateUtil.getInstance().currentSession();
            FotoMahasiswa photo = (FotoMahasiswa) streaming.createCriteria(FotoMahasiswa.class)
                    .add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult();
            if (photo == null) return;
            if (photo.getGdrive() != null) ais.common.BacaTulisUtil.tulis(new java.io.File(folder, "FOTO_" + safe(mahasiswa.getNim()) + ".txt"), photo.forwardGDriveUrl());
            else copyFile(photo.ambilFile(), new java.io.File(folder, "FOTO_" + safe(photo.ambilFile().getName())));
        } catch (Exception error) { Common.tampilErrorJikaAdmin(error); }
        finally { try { StreamingHibernateUtil.getInstance().closeSession(); } catch (Exception ignored) { } }
    }

    private static void copyStudentAttachment(Mahasiswa mahasiswa, String kind, java.io.File folder) {
        org.hibernate.Session streaming = null;
        try {
            streaming = StreamingHibernateUtil.getInstance().currentSession();
            LampiranLainMahasiswa attachment = (LampiranLainMahasiswa) streaming.createCriteria(LampiranLainMahasiswa.class)
                    .add(Restrictions.eq("mahasiswa", mahasiswa.getId())).add(Restrictions.eq("jenis", kind)).setMaxResults(1).uniqueResult();
            if (attachment == null) return;
            if (attachment.getGdrive() != null) ais.common.BacaTulisUtil.tulis(new java.io.File(folder, safe(kind + "_" + mahasiswa.getNim()) + ".txt"), attachment.forwardGDriveUrl());
            else if (attachment.getLink() != null && attachment.getLink().trim().length() > 0) ais.common.BacaTulisUtil.tulis(new java.io.File(folder, safe(kind + "_" + mahasiswa.getNim()) + ".txt"), attachment.getLink().trim());
            else copyFile(attachment.ambilFile(), new java.io.File(folder, safe(kind + "_" + attachment.ambilFile().getName())));
        } catch (Exception error) { Common.tampilErrorJikaAdmin(error); }
        finally { try { StreamingHibernateUtil.getInstance().closeSession(); } catch (Exception ignored) { } }
    }

    private static void copyAdditionalAttachments(Mahasiswa mahasiswa, java.io.File folder) {
        org.hibernate.Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Object[] data = (Object[]) session.createCriteria(BiodataMahasiswa.class).addOrder(Order.desc("id"))
                    .add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1)
                    .setProjection(Projections.projectionList().add(Projections.property("id")).add(Projections.property("parameterTambahanInds"))).uniqueResult();
            if (data == null || data[1] == null) return;
            String[] lines = String.valueOf(data[1]).split("\\n");
            for (int i = 0; i < lines.length; i++) {
                String[] item = lines[i].split("<=>"); String kind = item.length > 0 ? item[0].trim() : "";
                String label = item.length > 1 ? item[1].trim() : kind; String url = item.length > 2 ? item[2].trim() : "";
                if (url.length() == 0) continue;
                LampiranLain attachment = LampiranLain.ambil((Long) data[0], kind); if (attachment == null) continue;
                if (attachment.getGdrive() != null) ais.common.BacaTulisUtil.tulis(new java.io.File(folder, safe(kind + "_" + mahasiswa.getNim()) + ".txt"), attachment.forwardGDriveUrl());
                else copyFile(attachment.ambilFile(), new java.io.File(folder, safe(label + "_" + attachment.ambilFile().getName())));
            }
        } catch (Exception error) { Common.tampilErrorJikaAdmin(error); }
        finally { close(session); }
    }

    private static void copyFile(java.io.File source, java.io.File target) throws Exception {
        FileInputStream input = new FileInputStream(source); FileOutputStream output = new FileOutputStream(target);
        try { IOUtils.copyLarge(input, output); } finally { input.close(); output.close(); }
    }
    private static String safe(String value) { String result = value == null ? "data" : value.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_").trim(); return result.length() == 0 ? "data" : result; }

    private static List scopedEntities(GenericCrudRequestContext context, int mulai, int sampai) throws Exception {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = scopedCriteria(session, context);
            if (!(mulai == Integer.MIN_VALUE && sampai == Integer.MAX_VALUE))
                criteria.add(Restrictions.between("tahunangkatan", Integer.valueOf(mulai), Integer.valueOf(sampai)));
            return new ArrayList(criteria.list());
        } finally { close(session); }
    }
    private static String join(List<String> values) { StringBuilder result = new StringBuilder(); for (int i = 0; i < values.size(); i++) { if (i > 0) result.append(" | "); result.append(values.get(i)); } return result.toString(); }

    public static List scopedRows(GenericCrudRequestContext context, String... properties) throws Exception {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = scopedCriteria(session, context);
            org.hibernate.criterion.ProjectionList projections = Projections.projectionList();
            for (int i = 0; i < properties.length; i++) projections.add(Projections.property(properties[i]));
            criteria.setProjection(projections);
            return new ArrayList(criteria.list());
        } finally { close(session); }
    }

    private static Set<Long> scopedIds(GenericCrudRequestContext context) throws Exception {
        List rows = scopedRows(context, "id"); Set<Long> ids = new HashSet<Long>();
        for (int i = 0; i < rows.size(); i++) {
            Object value = rows.get(i); if (value instanceof Object[]) value = ((Object[]) value)[0];
            if (value instanceof Number) ids.add(Long.valueOf(((Number) value).longValue()));
        }
        return ids;
    }

    public static java.io.File downloadPhotos(GenericCrudRequestContext context) throws Exception {
        List rows = scopedRows(context, "id");
        List<Long> ids = new ArrayList<Long>();
        for (int i = 0; i < rows.size(); i++) {
            Object value = rows.get(i);
            if (value instanceof Number) ids.add(Long.valueOf(((Number) value).longValue()));
            else if (value instanceof Object[] && ((Object[]) value)[0] instanceof Number)
                ids.add(Long.valueOf(((Number) ((Object[]) value)[0]).longValue()));
        }
        return ais.common.helper.DownloadFotoMassalHelper.createFotoMahasiswaZip(ids);
    }

    public static java.io.File uploadPhotos(List<org.zkoss.util.media.Media> medias,
            GenericCrudRequestContext context) throws Exception {
        List rows = scopedRows(context, "nim");
        Set<String> allowed = new HashSet<String>();
        for (int i = 0; i < rows.size(); i++) {
            Object value = rows.get(i);
            if (value instanceof Object[]) value = ((Object[]) value)[0];
            if (value != null) allowed.add(String.valueOf(value));
        }
        ais.common.LaporanUpload report = new ais.common.LaporanUpload("Upload Foto Mahasiswa");
        ais.common.helper.UploadFotoMassalHelper.uploadFotoMahasiswaByNim(medias, report, allowed);
        return report.tulisBerkas();
    }

    private static java.io.File upload(InputStream input, GenericCrudRequestContext context, boolean password) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook(input);
        XSSFSheet sheet = workbook.getSheetAt(0);
        UploadReportHelper report = new UploadReportHelper(password ? "Upload Password Mahasiswa" : "Upload RFID Mahasiswa");
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            String nim = Common.getCellContent(Common.getCell(sheet, 0, i));
            String value = Common.getCellContent(Common.getCell(sheet, 1, i));
            if (nim == null || nim.trim().length() == 0) continue;
            Session session = null; Transaction transaction = null;
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                Criteria criteria = scopedCriteria(session, context);
                criteria.add(Restrictions.eq("nim", nim)).setMaxResults(1);
                Mahasiswa mahasiswa = (Mahasiswa) criteria.uniqueResult();
                if (mahasiswa == null) {
                    report.gagal(i, nim, "Mahasiswa tidak ditemukan", "Periksa NIM pada berkas Excel.");
                    continue;
                }
                if (password) mahasiswa.setPass(Common.desEncrypter.get().encrypt(value == null ? "" : value));
                else mahasiswa.setIdfinger(value);
                transaction = session.beginTransaction();
                session.update(mahasiswa);
                transaction.commit();
                report.sukses(i, nim, password ? "Password diperbarui" : "RFID diperbarui");
            } catch (Exception error) {
                if (transaction != null && transaction.isActive()) transaction.rollback();
                report.gagal(i, nim, error, "Periksa NIM dan format berkas.");
            } finally { close(session); }
        }
        return report.simpanLaporan();
    }

    private static Criteria scopedCriteria(Session session, GenericCrudRequestContext context) throws Exception {
        MahasiswaGenericCrudAdapter adapter = new MahasiswaGenericCrudAdapter();
        Criteria criteria = session.createCriteria(Mahasiswa.class);
        adapter.applyDefaultFilters(criteria, context);
        adapter.applyReadScope(criteria, context);
        return criteria;
    }

    private static byte[] bytes(XSSFWorkbook workbook) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        workbook.write(output);
        return output.toByteArray();
    }
    private static String text(Object value) { return value == null ? "" : String.valueOf(value); }
    private static void close(Session session) { if (session != null) try { session.close(); } catch (Exception ignored) { } }
}
