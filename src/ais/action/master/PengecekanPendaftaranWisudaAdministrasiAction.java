package ais.action.master;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.PendaftaranWisudaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;

/**
 * Controller/action ZK untuk pengecekan pendaftaran wisuda administrasi. Tipe ini merupakan titik
 * masuk UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi
 * khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Bandbox bandboxMahasiswa}, {@code Row
 * rowSimpan}, {@code Row rowConfirm}, {@code Label labelCekAdministrasi}, {@code MyButtonConfig btnSetuju},
 * {@code MyButtonConfig btnTolak}, {@code PendaftaranWisuda pendaftaranWisuda}, {@code List pilih};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}); mutasi data ({@code
 * onSetuju()}); operasi domain lain ({@code onPilihMahasiswa()}, {@code onTolak()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PengecekanPendaftaranWisudaAdministrasiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8131416291660409271L;
	Bandbox bandboxMahasiswa;

	Row rowSimpan;
	Row rowConfirm;

	Label labelCekAdministrasi;
	MyButtonConfig btnSetuju;
	MyButtonConfig btnTolak;

	PendaftaranWisuda pendaftaranWisuda;
	private List<MyCheckboxConfig> pilih = new ArrayList<MyCheckboxConfig>();
	private Rows rowsdata;
	private JSONObject jsonObject;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
	}

	public void onPilihMahasiswa() throws Exception {
		if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
			MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		Common.clear(rowsdata);

		Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
		Session session = HibernateUtil.currentSession();
		pendaftaranWisuda = (PendaftaranWisuda) session.createCriteria(PendaftaranWisuda.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();

		if (pendaftaranWisuda == null) {

			rowsdata.setVisible(false);

			MyMessageboxConfig.show("Mahasiswa ini belum mendaftar wisuda", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);

			return;
		} else {

			Row row = new Row();row.setValign("top");
			row.appendChild(new MyLabelConfig("Nama"));
			row.appendChild(new Label(pendaftaranWisuda.getMahasiswa().getNama()));
			row.setParent(rowsdata);

			row = new Row();
			row.appendChild(new MyLabelConfig("Prodi"));
			row.appendChild(new Label(pendaftaranWisuda.getMahasiswa().getJurusan().getNama()));
			row.setParent(rowsdata);

			row = new Row();
			row.appendChild(new MyLabelConfig("Program"));
			row.appendChild(new Label(pendaftaranWisuda.getMahasiswa().getProgram()));
			row.setParent(rowsdata);

			row = new Row();
			row.appendChild(new MyLabelConfig("Tahun masuk"));
			row.appendChild(new Label(pendaftaranWisuda.getMahasiswa().getTahunangkatan() + ""));
			row.setParent(rowsdata);

			if (pendaftaranWisuda.getMahasiswa().getTahunLulus() != null) {
				row = new Row();
				row.appendChild(new MyLabelConfig("Tahun lulus"));
				row.appendChild(new Label(pendaftaranWisuda.getMahasiswa().getTahunLulus() + ""));
				row.setParent(rowsdata);
			}

			row = new Row();
			row.appendChild(new MyLabelConfig("Judul"));
			row.appendChild(
					new Label(pendaftaranWisuda.getSkripsi() == null ? "" : pendaftaranWisuda.getSkripsi().getJudul()));
			row.setParent(rowsdata);

			String wisuda_administrasi = "Transkrip Akademik;Biaya Perkuliahan;Biaya Wisuda;TandaLulus Ujian Komprehensive;Propesa;Lembar Pengesahan Skripsi;Tanda Lulus Toafl/Toefl;Pas Photo;Administrasi";
			jsonObject = new JSONObject(pendaftaranWisuda.getStatusPendaftaran());
			for (String konf : Common.getKonfigurasi("wisuda_administrasi", wisuda_administrasi).getNilai()
					.split(";")) {
				String key = getClass().getSimpleName().toLowerCase() + "_" + konf.toLowerCase().replaceAll(" ", "_");
				String nilai = jsonObject.isNull(key) ? null : jsonObject.get(key) + "";

				row = new Row();
				row.appendChild(new MyLabelConfig());
				MyCheckboxConfig a;
				row.appendChild(a = new MyCheckboxConfig(konf));
				a.setAttribute("value", konf);
				a.setChecked(nilai != null && nilai.equalsIgnoreCase("1"));
				row.setParent(rowsdata);
				pilih.add(a);
			}

			if (pendaftaranWisuda.getStatusPersetujuanAdministrasi() != null
					&& pendaftaranWisuda.getStatusPersetujuanAdministrasi().equals(1)) {
				btnTolak.setDisabled(false);
				btnSetuju.setDisabled(true);
			} else {
				btnTolak.setDisabled(true);
				btnSetuju.setDisabled(false);
			}

		}
	}

	public void onSetuju() throws Exception {
		for (MyCheckboxConfig a : pilih) {
			if (!a.isChecked()) {
				MyMessageboxConfig.show("\"" + a.getLabel() + "\" harus dipilih", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			} else {
				String konf = (String) a.getAttribute("value");
				String key = getClass().getSimpleName().toLowerCase() + "_" + konf.toLowerCase().replaceAll(" ", "_");

				jsonObject.put(key, "1");
			}
		}

		MyMessageboxConfig.show("Apakah anda yakin, mahasiswa ini tidak ada masalah menyangkut administrasi ?",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.CANCEL)
							return;
						else {
							PendaftaranWisudaDao daftarWisudaDao = DaoFactory.getInstance().getPendaftaranWisudaDao();
							if (pendaftaranWisuda.getId() != null) {
								pendaftaranWisuda = daftarWisudaDao.load(pendaftaranWisuda.getId());
							}

							pendaftaranWisuda.setStatusPendaftaran(jsonObject.toString());
							pendaftaranWisuda.setStatusPersetujuanAdministrasi(1);

							if (pendaftaranWisuda.getId() != null) {
								daftarWisudaDao.update(pendaftaranWisuda);
							}
							btnTolak.setDisabled(false);
							btnSetuju.setDisabled(true);
							return;
						}

					}
				});

	}

	public void onTolak() throws InterruptedException {

		MyMessageboxConfig.show(
				"Apakah anda yakin, mahasiswa ini belum menyelesaikan masalah menyangkut administrasi ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.CANCEL)
							return;
						else {
							PendaftaranWisudaDao daftarWisudaDao = DaoFactory.getInstance().getPendaftaranWisudaDao();
							if (pendaftaranWisuda.getId() != null) {
								pendaftaranWisuda = daftarWisudaDao.load(pendaftaranWisuda.getId());
							}

							pendaftaranWisuda.setStatusPersetujuanAdministrasi(0);

							if (pendaftaranWisuda.getId() != null) {
								daftarWisudaDao.update(pendaftaranWisuda);
							}

							btnTolak.setDisabled(true);
							btnSetuju.setDisabled(false);
							return;
						}

					}
				});

	}
}
