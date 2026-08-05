package ais.action.report.format1.spi;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Toolbar;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.spi.PenugasanAuditSPI;
import ais.database.model.spi.TemuanAuditSPI;
import ais.database.model.spi.TimAuditSPI;
import ais.ui.util.MyWindow;

/**
 * <h2>LaporanPenugasanAuditSPI &mdash; Cetak Lembar Kerja Penugasan Audit SPI (PDF/Excel/dll.)</h2>
 *
 * <p>
 * Jendela pratinjau &amp; cetak untuk satu {@link PenugasanAuditSPI} &mdash; menyusun seluruh data
 * penugasan (judul, unit kerja, jenis audit, tim audit, dan daftar temuan beserta klasifikasinya)
 * menjadi berkas laporan lewat template Jasper {@code format1/spi/lembar_kerja_penugasan_audit_spi}.
 * Kelas ini SENGAJA meniru persis kerangka {@code ais.action.report.format1.akademik.LaporanHasilSPMI}
 * (laporan serupa milik modul Audit Mutu Internal akademik yang sudah production-proven) karena
 * kebutuhannya identik: satu dokumen induk dengan tabel rincian temuan di bawahnya.
 * </p>
 *
 * <h3>Satu template Jasper menghasilkan PDF MAUPUN Excel sekaligus</h3>
 * <p>
 * Toolbar pratinjau dibangun lewat {@link CommonReport#exportReport} &mdash; helper baku aplikasi
 * ini yang SUDAH menyediakan tombol unduh untuk berbagai format (PDF, XLS, DOCX, dst.) dari SATU
 * definisi laporan Jasper yang sama, tergantung konfigurasi {@code report_tombol_pdf}/
 * {@code report_tombol_xls} dsb. Karena itu, TIDAK PERLU membangun jalur ekspor Excel terpisah:
 * cukup satu berkas {@code .jrxml}, dan pengguna bisa memilih format unduhan yang diinginkan
 * langsung dari toolbar jendela pratinjau ini.
 * </p>
 *
 * <h3>Hanya temuan yang SUDAH diisi yang tercetak</h3>
 * <p>
 * Sama seperti {@code LaporanHasilSPMI}, laporan ini membaca baris {@link TemuanAuditSPI} yang
 * BENAR-BENAR ADA di database untuk penugasan ini &mdash; bukan berjalan menyusuri seluruh pohon
 * checklist (yang akan menampilkan baris "Belum diisi" di kertas cetak, tidak berguna untuk
 * dokumen resmi). Checklist yang belum diperiksa auditor cukup tidak muncul di lembar kerja cetak.
 * </p>
 *
 * @author e-Campus SPI Team
 */
public class LaporanPenugasanAuditSPI extends MyWindow {

    private static final long serialVersionUID = 1L;

    private static final String TEMPLATE = "format1/spi/lembar_kerja_penugasan_audit_spi";

    private Center center;
    private Toolbar toolbar;

    private PenugasanAuditSPI penugasanAuditSPI;

    public LaporanPenugasanAuditSPI(PenugasanAuditSPI penugasanAuditSPI) {
        super();
        this.penugasanAuditSPI = penugasanAuditSPI;
        try {
            init();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Penugasan Audit SPI", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
            		new String[] {
            			"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
            			"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
        }
    }

    private void init() throws Exception {
        Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
        borderlayout.setParent(this);

        center = new Center();
        center.setParent(borderlayout);
        ais.ui.util.ZkCompat.setFlex(center, true);

        org.zkoss.zul.North north = new org.zkoss.zul.North();
        north.setParent(borderlayout);
        north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public Map<String, Serializable> generateParameters() throws Exception {
                Map parameters = generateParameter();
                return parameters;
            }
        }, TEMPLATE, null, new EventListener() {

            @Override
            public void onEvent(Event arg0) throws Exception {
                onReport(arg0);
            }
        }));

        onReport(null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map generateParameter() throws Exception {

        if (penugasanAuditSPI != null && penugasanAuditSPI.getId() != null) {
            HibernateUtil.currentSession().refresh(penugasanAuditSPI);
        }

        Map parameters = ais.common.HashMapGenerator.getRand();

        parameters.put("namaSatuanKerja",
                penugasanAuditSPI.getSatuanKerja() != null ? penugasanAuditSPI.getSatuanKerja().getNama() : "");
        parameters.put("namaJenisAudit",
                penugasanAuditSPI.getJenisAuditSPI() != null ? penugasanAuditSPI.getJenisAuditSPI().getNama() : "");
        parameters.put("tanggalMulai",
                penugasanAuditSPI.getTanggalMulai() != null
                        ? Common.dateFormat6.get().format(penugasanAuditSPI.getTanggalMulai()) : "");
        parameters.put("tanggalSelesai",
                penugasanAuditSPI.getTanggalSelesai() != null
                        ? Common.dateFormat6.get().format(penugasanAuditSPI.getTanggalSelesai()) : "-");
        parameters.put("judulPenugasan",
                penugasanAuditSPI.getNama() != null ? penugasanAuditSPI.getNama() : "");
        parameters.put("statusPersetujuan",
                penugasanAuditSPI.getStatus() != null ? penugasanAuditSPI.getStatus() : "");

        Session session = HibernateUtil.currentSession();

        List<TimAuditSPI> tim = session.createCriteria(TimAuditSPI.class)
                .add(Restrictions.eq("penugasanAuditSPI", penugasanAuditSPI))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .list();
        List<String> ketuaList = new ArrayList<String>();
        List<String> anggotaList = new ArrayList<String>();
        for (TimAuditSPI t : tim) {
            String namaAnggota = t.getAnggota() != null ? t.getAnggota().getUserNama() : "";
            if (TimAuditSPI.KETUA_TIM.equals(t.getPeranTim())) {
                ketuaList.add(namaAnggota);
            } else {
                anggotaList.add(namaAnggota);
            }
        }
        parameters.put("ketuaTim", gabungKoma(ketuaList));
        parameters.put("anggotaTim", gabungKoma(anggotaList));

        List<Map> maps = new ArrayList<Map>();
        int kritis = 0, mayor = 0, minor = 0, observasi = 0, sesuai = 0;

        Criteria criteria = session.createCriteria(TemuanAuditSPI.class)
                .add(Restrictions.eq("penugasanAuditSPI", penugasanAuditSPI))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.asc("id"));
        List<TemuanAuditSPI> temuanList = criteria.list();

        for (TemuanAuditSPI temuan : temuanList) {
            String klas = temuan.getKlasifikasi();
            if (TemuanAuditSPI.KRITIS.equals(klas)) kritis++;
            else if (TemuanAuditSPI.MAYOR.equals(klas)) mayor++;
            else if (TemuanAuditSPI.MINOR.equals(klas)) minor++;
            else if (TemuanAuditSPI.OBSERVASI.equals(klas)) observasi++;
            else if (TemuanAuditSPI.SESUAI.equals(klas)) sesuai++;

            Map map = new HashMap();
            map.put("kriteria.nama", kosongkanNull(temuan.getKriteriaSnapshot()));
            map.put("checklist.nama", kosongkanNull(temuan.getChecklistSnapshot()));
            map.put("klasifikasiLabel", kosongkanNull(temuan.getKlasifikasiLabel()));
            map.put("kondisi", kosongkanNull(temuan.getKondisi()));
            map.put("sebab", kosongkanNull(temuan.getSebab()));
            map.put("akibat", kosongkanNull(temuan.getAkibat()));
            map.put("rekomendasi", kosongkanNull(temuan.getRekomendasi()));
            maps.add(map);
        }

        parameters.put("jumlahKritis", kritis);
        parameters.put("jumlahMayor", mayor);
        parameters.put("jumlahMinor", minor);
        parameters.put("jumlahObservasi", observasi);
        parameters.put("jumlahSesuai", sesuai);
        parameters.put("jumlahTotal", kritis + mayor + minor + observasi + sesuai);
        parameters.put("maps", maps);

        return parameters;
    }

    private static String gabungKoma(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (s == null || s.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(s);
        }
        return sb.toString();
    }

    private static String kosongkanNull(String s) {
        return s == null ? "" : s;
    }

    public void onReport(Event event) {
        try {
            File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), TEMPLATE,
                    ais.ui.util.WaktuUtil.getDate(), null, toolbar);
            CommonReport.tampilkanReportPDF(center, file);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Penugasan Audit SPI", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
            		new String[] {
            			"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
            			"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
        }
    }
}
