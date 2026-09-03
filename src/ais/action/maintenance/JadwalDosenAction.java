package ais.action.maintenance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Html;

import ais.action.master.PengumumanAkademisAction;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Pertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk jadwal dosen. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}); pembacaan/pencarian ({@code tampilkanKehadiranDosen()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class JadwalDosenAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7612581816935380134L;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		Borderlayout borderlayout = new Borderlayout();
		if (borderlayout != null) { borderlayout.setHeight("100%"); }
		if (borderlayout != null) { borderlayout.setWidth("100%"); }
		page.getFirstRoot().appendChild(borderlayout);

		Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		if (center != null) { center.setBorder("none"); }
		if (center != null) { center.setParent(borderlayout); }

		center.appendChild(new Html(tampilkanKehadiranDosen(execution.getParameter("kode"))));
		Common.initLaguage();
	}

	@SuppressWarnings("unchecked")
	public static String tampilkanKehadiranDosen(String kodeProdi) {
		String pengumuman = "";
		String pengumumanTabel = "";
		String sekarang = Common.dateFormat8.get().format(WaktuUtil.getDate());
		try {

			int jml = 5;
			int jmlBawah = 2;
			int i = 1;

			Map<Long, Pertemuan> pertemuans = PengumumanAkademisAction.pertemuansHarian.get(sekarang);
			if (pertemuans == null) {
				List<Pertemuan> pertemuansData = HibernateUtil.currentSession().createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("waktuMulai")).add(Restrictions.eq("tanggal", WaktuUtil.getDate()))
						.add(Restrictions.or(Restrictions.isNotNull("jadwalPelajaran"),
								Restrictions.isNotNull("perkuliahan")))
						.list();
				pertemuans = new java.util.concurrent.ConcurrentHashMap<Long, Pertemuan>();
				for (Pertemuan pertemuan : pertemuansData) {
					pertemuans.put(pertemuan.getId(), pertemuan);
				}
				pertemuansData = null;
				PengumumanAkademisAction.pertemuansHarian.put(sekarang, pertemuans);
			}

			List<Pertemuan> hariIni = new ArrayList<Pertemuan>();

			for (Pertemuan pertemuan : new ArrayList<Pertemuan>(pertemuans.values())) {
				if (pertemuan.getPerkuliahan() != null && pertemuan.getTanggal() != null
						&& pertemuan.getPerkuliahan().getJumlahDosen() > 0
						&& sekarang.equals(Common.dateFormat8.get().format(pertemuan.getTanggal()))) {
					if (kodeProdi == null || kodeProdi.isEmpty() || (kodeProdi != null
							&& pertemuan.getPerkuliahan().getJurusan() != null
							&& pertemuan.getPerkuliahan().getJurusan().getKodeEpsbed() != null
							&& pertemuan.getPerkuliahan().getJurusan().getKodeEpsbed().equalsIgnoreCase(kodeProdi))) {

						if (hariIni.size() > 30) {
							break;
						}

						hariIni.add(pertemuan);
					}
				}
			}

			//System.out.println("Pertemuan hari ini -> " + hariIni.size() + " total " + pertemuans.size());

			if (hariIni != null && !hariIni.isEmpty()) {

				Collections.sort(hariIni);

				Long randId = Common.randLong();
				pengumuman += "<div class=\"sdosenhow-container\" style=\"background-color: rgba(255,255,255,0.4);" + "\">\n";

				int banyak = 0;
				for (Pertemuan pertemuan : hariIni) {

					List<Long> dosens = pertemuan.ambilDosenId();
					for (@SuppressWarnings("unused")
					Long id : dosens) {

						if (i % jml == 0) {
							banyak++;
						}

						i++;
					}
				}

				i = 1;
				int j = 1;
				int k = 1;
				String tds = "";
				for (Pertemuan pertemuan : hariIni) {

					List<Dosen> dosens = pertemuan.ambilDosen();
					for (Dosen idDosen : dosens) {

						String link = CommonMedia.getUrlFotoPengguna(new Tbmuser(idDosen));

						Statusabsensi statusabsensi = null;
						if (pertemuan.getId() != null) {

							statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
									pertemuan.retreiveAbsensiId(idDosen.getId()));

						}

						if (statusabsensi == null) {
							statusabsensi = ConstantValues.BELUM_ABSEN;
						}

						String tableData = "<table>";

						tableData += "<tr><td><strong>" + idDosen.getNama() + "</strong></td></tr>";
						tableData += "<tr><td>Kehadiran: " + statusabsensi.getNama() + "</td></tr>";
						tableData += "<tr><td>" + pertemuan.getStatusPertemuan().getNama() + "</td></tr>";
						tableData += "<tr><td>" + pertemuan.getPerkuliahan().getMatakuliah().getNama() + " "
								+ pertemuan.getPerkuliahan().getSemester() + " " + pertemuan.getPerkuliahan().getKelas()
								+ "</td></tr>";

						if (pertemuan.getRuang() != null) {
							tableData += "<tr><td>" + pertemuan.getRuang().getNama() + "</td></tr>";
						}

						if (idDosen.getTelp() != null && !idDosen.getTelp().isEmpty()) {
							tableData += "<tr><td>" + idDosen.getTelp() + "</td></tr>";
						}

						tableData += "<tr><td>" + pertemuan.getWaktuMulai() + " sd " + pertemuan.getWaktuSelesai()
								+ "</td></tr>";

						tableData += "</table>";

						String td = "<td>" + "<img src=\"" + link
								+ "\" class=\"gambar_profile\" /><br>"
								+ tableData + "\n" + "</td>";

						tds += td;

						if (i % jml == 0) {

							String table = "<table><tr>" + tds + "  </tr></table>";

							pengumumanTabel += "<hr><br>" + table;

							if (k % jmlBawah == 0) {

								pengumuman += "\n<div name=\"mySdosen" + randId + "\" class=\"mySdosen fade\">\n"
										+ "<div class=\"numbertext\">\n" + j + " / " + banyak
										+ "</div>\n" + pengumumanTabel + "</div>";

								pengumumanTabel = "";
								j++;
							}
							tds = "";
							k++;
						}

						i++;
					}
				}

				if (!tds.isEmpty()) {
					String table = "<table><tr>" + tds + "  </tr></table>";

					pengumuman += "\n<div name=\"mySdosen" + randId + "\" class=\"mySdosen fade\">\n"
							+ "<div class=\"numbertext\">\n" + j + " / " + banyak + "</div>\n"
							+ table + "</div>";
				}

				pengumuman += "</div><br>\n";
				pengumuman += "<div>\n";

				for (int h = 0; h <= banyak; h++) {
					pengumuman += "<span name=\"dot" + randId + "\" class=\"dot\"></span> \n";
				}
				pengumuman += "</div><br>\n";

				pengumuman += "<script>\n" + "var slideIndex" + randId + " = 0;\n" + "showSdosen" + randId + "();\n" +

						"function showSdosen" + randId + "() { \n try { \n" + "  var i" + randId + ";\n"
						+ "  var sdosen = document.getElementsByName(\"mySdosen" + randId + "\");\n"
						+ "  var dots = document.getElementsByName(\"dot" + randId + "\");\n" + "  for (i" + randId
						+ " = 0; i" + randId + " < sdosen.length; i" + randId + "++) {\n" + "    sdosen[i" + randId
						+ "].style.display = \"none\";  \n" + "  }\n" + "  slideIndex" + randId + "++;\n"
						+ "  if (slideIndex" + randId + " > sdosen.length) {slideIndex" + randId + " = 1}    \n"
						+ "  for (i" + randId + " = 0; i" + randId + " < dots.length; i" + randId + "++) {\n"
						+ "    dots[i" + randId + "].className = dots[i" + randId
						+ "].className.replace(\" active\", \"\");\n" + "  }\n" + "  sdosen[slideIndex" + randId
						+ "-1].style.display = \"block\";  \n" + "  dots[slideIndex" + randId
						+ "-1].className += \" active\";\n" + " \n } catch (e) {}\n  setTimeout(showSdosen" + randId
						+ ", " + (10 * 1000) + "); \n" + "    }\n" + "</script>";

			}

			hariIni = null;
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		return pengumuman.isEmpty() ? ""
				: "<h2>Informasi Kehadiran Dosen Harian " + Common.dateFormat4.get().format(WaktuUtil.getDate()) + "</h2><br>"
						+ pengumuman;
	}

}
