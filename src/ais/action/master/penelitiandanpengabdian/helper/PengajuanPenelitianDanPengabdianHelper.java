package ais.action.master.penelitiandanpengabdian.helper;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.JurusanAction;
import ais.action.master.LogLoginAction;
import ais.action.master.bkd.helper.PenilaianAsesorHelper;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.AmbilDataTbmuserBanbox;
import ais.action.master.helper.DetailArtikelHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataMahasiswaBanyak;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.action.report.format1.penelitiandanpengabdian.LaporanPenelitian;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.Html2Text;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PenilaianAsesor;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.penelitiandanpengabdian.AnggotaPengajuanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.FilePengajuanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.JenisPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.PenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.PengajuanTahapanPelaporanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.SumberDanaPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.TipePenelitianDanPengabdian;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper inti (dan superclass, lihat {@link PersetujuanPenelitianHelper}) untuk seluruh alur
 * <b>pengajuan penelitian dan pengabdian kepada masyarakat</b> dosen/mahasiswa: pengisian form
 * proposal, penyimpanan, listing/pencarian, pengelolaan anggota tim, penilaian oleh asesor
 * (terintegrasi dengan {@link PenilaianAsesorHelper}), workflow disposisi SOP (implementasi
 * {@link FormSop}), dan publikasi hasil ke repositori institusi berbasis DSpace.
 *
 * <p>
 * Kelas ini dapat dipakai dalam beberapa mode tergantung konstruktor: mode form pengajuan biasa
 * (kosong), mode persetujuan/disposisi ({@code persetujuan=true} dengan {@link TipePenelitianDanPengabdian}
 * tertentu), atau dibatasi ke peruntukan tertentu ({@code diperuntukkanPengajuan}, mis. khusus
 * dosen atau mahasiswa lewat {@link PengumumanAkademis}).
 * </p>
 *
 * <p>
 * <b>Integrasi DSpace</b> — method statis {@code getDspace*} membangun/menyinkronkan hierarki
 * repositori DSpace secara otomatis dari struktur data lokal: community per jenis pengajuan+jurusan
 * ({@link #getDspacePengajuanPenelitianDanPengabdian}), diikuti collection per tipe penelitian
 * ({@link #getDspaceTipePengajuanPenelitianDanPengabdian}) dan per tahun
 * ({@link #getDspaceTipePengajuanPenelitianDanPengabdianTahun}), lalu item DSpace individual untuk
 * satu pengajuan ({@link #getDspace}) lengkap dengan metadata Dublin Core (penulis, editor, hak
 * cipta). UUID setiap simpul komunitas/koleksi dicache pada {@link Konfigurasi} bernama
 * {@code dspace_label_collection_*} agar tidak dibuat berulang. Autentikasi ke DSpace memakai
 * {@code cookie} sesi yang diteruskan dari pemanggil (tidak ada kredensial tertanam di kelas ini).
 * </p>
 */
public class PengajuanPenelitianDanPengabdianHelper implements DataCriteria, DataSearchDefault, FormSop {

	private MyGrid gridPengajuan;
	private Combobox searchPenelitianDanPengabdian;

	private Textbox cariPengaju;

	private Paging paging;
	private Paging pagingAnggota;
	protected String usernamePengajuan;
	protected String diperuntukkanPengajuan;

	/** Membuat helper mode persetujuan/disposisi untuk satu {@code jenis} (tipe) penelitian/pengabdian tertentu. */
	public PengajuanPenelitianDanPengabdianHelper(boolean persetujuan, TipePenelitianDanPengabdian jenis) {
		this.persetujuan = persetujuan;
		this.jenis = jenis;
	}

	/** Membuat helper mode form pengajuan biasa (tanpa pembatasan tipe/peruntukan). */
	public PengajuanPenelitianDanPengabdianHelper() {
	}

	/** Membuat helper dibatasi ke peruntukan tertentu (mis. {@link PengumumanAkademis#UNTUK_DOSEN}/{@code UNTUK_MAHASISWA}). */
	public PengajuanPenelitianDanPengabdianHelper(String diperuntukkanPengajuan) {
		this.diperuntukkanPengajuan = diperuntukkanPengajuan;
	}

	private Boolean readonly = false;
	private TipePenelitianDanPengabdian jenis = null;
	private MyDatebox tanggal;
	// protected Media media;
	protected LampiranLain f;
	protected LampiranLain sTugas;
	protected LampiranLain sKeterangan;
	private boolean persetujuan = false;
	private PenelitianDanPengabdian penelitianDanPengabdianData;
	private PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdianData;
	private AmbilDataTbmuserBanbox tbmuserD;
	private AmbilDataMahasiswaBanbox mahasiswa;
	private Combobox penelitianDanPengabdian;
	private Textbox judul;
	private MyCkEditor tujuan;
	private MyDoublebox jumlahDana;
	private Vbox sumberdana;
	private Textbox masaPenugasan;
	private Textbox keterangan;
	private Textbox keyword;
	private Textbox anggota;
	private Textbox editorDanKontributor;
	private DisposisiSop disposisiSop;
	private MyToolbarbuttonConfig save;
	private EventListener setujui;
	private MyWindow window;
	protected LampiranLain sRekomendasi;
	private boolean rekomnedasiWajib = false;

	/**
	 * Implementasi {@link FormSop#form}: membangun form pengajuan penelitian/pengabdian (pengaju,
	 * judul, tujuan, jenis penelitian/pengabdian, masa penugasan, sumber dana, jumlah dana,
	 * anggota tim, editor/kontributor, abstrak, kata kunci, lampiran) di dalam konteks alur
	 * disposisi SOP. Dipakai baik untuk membuat pengajuan baru maupun menampilkan/menyunting
	 * pengajuan yang sudah ada (dari {@code generalValueObject}).
	 *
	 * @param generalValueObject entitas {@link PengajuanPenelitianDanPengabdian} yang diedit, atau data kosong untuk pengajuan baru
	 * @param disposisiSop       konteks disposisi SOP yang menaungi form ini
	 * @param save                tombol simpan yang disediakan alur SOP, ditautkan ke {@link #onSave}
	 * @param setujui             listener persetujuan dari alur SOP
	 * @return grid komponen form siap ditempel
	 */
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop, MyToolbarbuttonConfig save,
			EventListener setujui) throws Exception {
		this.disposisiSop = disposisiSop;
		this.save = save;
		this.setujui = setujui;

		Component parent = (Component) save.getAttribute("parent");

		this.penelitianDanPengabdianData = null;
		this.pengajuanPenelitianDanPengabdianData = null;
		if (generalValueObject instanceof PenelitianDanPengabdian) {
			this.penelitianDanPengabdianData = (PenelitianDanPengabdian) generalValueObject;

		} else if (generalValueObject instanceof PengajuanPenelitianDanPengabdian) {
			this.pengajuanPenelitianDanPengabdianData = (PengajuanPenelitianDanPengabdian) generalValueObject;
			this.penelitianDanPengabdianData = this.pengajuanPenelitianDanPengabdianData == null ? null
					: this.pengajuanPenelitianDanPengabdianData.getPenelitianDanPengabdian();
		}

		if (this.penelitianDanPengabdianData != null && jenis == null
				&& this.penelitianDanPengabdianData.getTipePenelitianDanPengabdian() != null) {
			jenis = this.penelitianDanPengabdianData == null ? null
					: this.penelitianDanPengabdianData.getTipePenelitianDanPengabdian();
		}

		if (pengajuanPenelitianDanPengabdianData != null) {
			if (pengajuanPenelitianDanPengabdianData.getTbmuser() == null && tbmuser != null
					&& tbmuser.ambilDosen() != null) {
				pengajuanPenelitianDanPengabdianData.setTbmuser(tbmuser);
				pengajuanPenelitianDanPengabdianData.setDiajukanOleh(tbmuser);
			} else if (pengajuanPenelitianDanPengabdianData.getTbmuser() == null && tbmuser != null
					&& tbmuser.getMahasiswa() != null) {
				pengajuanPenelitianDanPengabdianData.setMahasiswa(tbmuser.getMahasiswa());
			}
		}
		MyGrid grid = displayWindowPengajuanBaru(parent, penelitianDanPengabdianData,
				pengajuanPenelitianDanPengabdianData, jenis);

		if (disposisiSop == null) {

			South south = new South();
			south.setVisible(parent != null && parent instanceof Window);
			ais.ui.util.ZkCompat.setFlex(south, true);
			if (south.isVisible()) {
				window = new MyWindow();

				window.setHeight("95%");
				window.setWidth("90%");
				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				south.setParent(borderlayout);
				window.setParent(parent);
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);
				grid.setParent(center);

			} else {
				window = null;
				grid.setParent(parent);
			}

			if (!south.isVisible()) {
				Common.freeze(parent, true);
			}

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(south);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					window.detach();
				}
			});
			cancel.setParent(toolbar);

			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (onSave(event)) {
						loadDataPengajuan();
						window.detach();
					}
				}
			});
			save.setParent(toolbar);

			if (pengajuanPenelitianDanPengabdianData != null && pengajuanPenelitianDanPengabdianData.getStatus()
					.equals(PengajuanPenelitianDanPengabdian.DISETUJUI)) {
				Common.freeze(window, true);
				save.setVisible(false);
				cancel.setDisabled(false);
			}

			if (south.isVisible()) {
				window.onModal();
			}
		}

		return grid;
	}

	@SuppressWarnings("unchecked")
	@Override
	/**
	 * Memvalidasi (pengaju wajib salah satu dosen/user atau mahasiswa; lama pengerjaan, abstrak,
	 * kata kunci, dan pilihan penelitian/pengabdian induk wajib diisi; surat rekomendasi wajib
	 * diunggah bila {@link #rekomnedasiWajib} aktif dan belum ada lampiran yang tersimpan) dan
	 * menyimpan/memperbarui data pengajuan dari isian form saat ini, termasuk sumber dana yang
	 * dicentang. Memuat ulang entitas terkait dari sesi terkini sebelum menyimpan untuk menghindari
	 * kondisi data sudah dihapus/berubah pihak lain (menampilkan peringatan bila demikian).
	 *
	 * @param event event pemicu tombol simpan
	 * @return {@code true} bila validasi lolos dan data tersimpan; {@code false} bila validasi gagal
	 */
	public boolean onSave(Event event) throws Exception {
		Session session = Common.getManualSession();

		Tbmuser selectedTbmuser = (Tbmuser) tbmuserD.getAttribute("tbmuser");
		Mahasiswa selectedMahasiswa = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");

		if (selectedTbmuser == null && selectedMahasiswa == null) {
			MyMessageboxConfig.show("Mahasiswa atau user harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (masaPenugasan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Lama Pengerjaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (keterangan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Abstrak harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (keyword.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Kata Kunci harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		PenelitianDanPengabdian selectedPenelitianDanPengabdian = null;
		if (penelitianDanPengabdian.getSelectedItem() != null
				&& penelitianDanPengabdian.getSelectedItem().getValue() instanceof PenelitianDanPengabdian) {
			selectedPenelitianDanPengabdian = (PenelitianDanPengabdian) penelitianDanPengabdian.getSelectedItem()
					.getValue();
		}
		if (selectedPenelitianDanPengabdian == null || selectedPenelitianDanPengabdian.getId() == null) {
			MyMessageboxConfig.show("Penelitian dan Pengabdian harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		selectedPenelitianDanPengabdian = (PenelitianDanPengabdian) session.get(PenelitianDanPengabdian.class,
				selectedPenelitianDanPengabdian.getId());
		if (selectedPenelitianDanPengabdian == null) {
			MyMessageboxConfig.show(
					"Data Penelitian dan Pengabdian yang dipilih sudah tidak ditemukan. Silakan buka ulang form.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (rekomnedasiWajib && jenis != null) {
			if (pengajuanPenelitianDanPengabdianData != null && pengajuanPenelitianDanPengabdianData.getId() != null) {

				if (sRekomendasi == null) {
					LampiranLain lam = LampiranLain.ambil(pengajuanPenelitianDanPengabdianData.getId(),
							"Surat Rekomendasi " + jenis.getIsi());
					if (lam == null) {
						MyMessageboxConfig.show(
								"Surat Rekomendasi " + (jenis == null ? "" : jenis.getIsi()) + " wajib diupload !",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return false;
					}
				}
			} else {
				if (sRekomendasi == null) {
					MyMessageboxConfig.show(
							"Surat Rekomendasi " + (jenis == null ? "" : jenis.getIsi()) + " wajib diupload !",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (pengajuanPenelitianDanPengabdianData == null || pengajuanPenelitianDanPengabdianData.getId() == null) {
			pengajuanPenelitianDanPengabdianData = new PengajuanPenelitianDanPengabdian();
		} else {
			PengajuanPenelitianDanPengabdian dataTersimpan = (PengajuanPenelitianDanPengabdian) session.get(
					PengajuanPenelitianDanPengabdian.class, pengajuanPenelitianDanPengabdianData.getId());
			if (dataTersimpan == null) {
				MyMessageboxConfig.show("Data pengajuan sudah tidak ditemukan. Silakan buka ulang daftar pengajuan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			pengajuanPenelitianDanPengabdianData = dataTersimpan;
		}

		pengajuanPenelitianDanPengabdianData.setTbmuser(selectedTbmuser);
		pengajuanPenelitianDanPengabdianData.setMahasiswa(selectedMahasiswa);
		pengajuanPenelitianDanPengabdianData.setPenelitianDanPengabdian(selectedPenelitianDanPengabdian);
		pengajuanPenelitianDanPengabdianData.setMasaPenugasan(masaPenugasan.getValue());
		pengajuanPenelitianDanPengabdianData.setTanggal(tanggal.getValue());
		pengajuanPenelitianDanPengabdianData.setJudul(judul.getValue());
		pengajuanPenelitianDanPengabdianData.setAnggota(anggota.getValue());
		pengajuanPenelitianDanPengabdianData.setKeterangan(keterangan.getValue());
		pengajuanPenelitianDanPengabdianData.setKeyword(keyword.getValue());
		pengajuanPenelitianDanPengabdianData.setTujuan(tujuan.getValue());
		pengajuanPenelitianDanPengabdianData.setJumlahDana(jumlahDana.getValue());
		pengajuanPenelitianDanPengabdianData.setEditorDanKontributor(editorDanKontributor.getValue());
		pengajuanPenelitianDanPengabdianData
				.setSumberDanaPenelitianDanPengabdianes(new HashSet<SumberDanaPenelitianDanPengabdian>());
		if (disposisiSop != null && disposisiSop.getId() != null) {
			pengajuanPenelitianDanPengabdianData.setDisposisiSop(disposisiSop);
		}
		List<MyCheckboxConfig> checkboxs = sumberdana.getChildren();
		for (MyCheckboxConfig checkbox : checkboxs) {
			if (checkbox != null && checkbox.isChecked()
					&& checkbox.getAttribute("nilai") instanceof SumberDanaPenelitianDanPengabdian) {
				SumberDanaPenelitianDanPengabdian sumberDana = (SumberDanaPenelitianDanPengabdian) checkbox
						.getAttribute("nilai");
				if (sumberDana != null && sumberDana.getId() != null) {
					sumberDana = (SumberDanaPenelitianDanPengabdian) session
							.get(SumberDanaPenelitianDanPengabdian.class, sumberDana.getId());
					if (sumberDana != null) {
						pengajuanPenelitianDanPengabdianData.getSumberDanaPenelitianDanPengabdianes()
								.add(sumberDana);
					}
				}
			}
		}

		if (pengajuanPenelitianDanPengabdianData != null) {
			if (pengajuanPenelitianDanPengabdianData.getTbmuser() == null && tbmuser != null
					&& tbmuser.ambilDosen() != null) {
				pengajuanPenelitianDanPengabdianData.setTbmuser(tbmuser);
				pengajuanPenelitianDanPengabdianData.setDiajukanOleh(tbmuser);
			} else if (pengajuanPenelitianDanPengabdianData.getTbmuser() == null && tbmuser != null
					&& tbmuser.getMahasiswa() != null) {
				pengajuanPenelitianDanPengabdianData.setMahasiswa(tbmuser.getMahasiswa());
			}
		}

		Common.refreshSaveOrUpdate(session, pengajuanPenelitianDanPengabdianData);
		session.flush();

		List<AnggotaPengajuanPenelitianDanPengabdian> anggotaPengajuanPenelitianDanPengabdians = new ArrayList<AnggotaPengajuanPenelitianDanPengabdian>();

		String anggotaText = pengajuanPenelitianDanPengabdianData.getAnggota() == null ? ""
				: pengajuanPenelitianDanPengabdianData.getAnggota();
		for (String s : anggotaText.split(",")) {
			if (!s.trim().isEmpty()) {
				Tbmuser tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.idEq(s.trim())).uniqueResult();
				if (tbmuser != null) {
					AnggotaPengajuanPenelitianDanPengabdian anggotaPengajuanPenelitianDanPengabdian = new AnggotaPengajuanPenelitianDanPengabdian();
					anggotaPengajuanPenelitianDanPengabdian.setTbmuser(tbmuser);
					anggotaPengajuanPenelitianDanPengabdian
							.setPengajuanPenelitianDanPengabdian(pengajuanPenelitianDanPengabdianData);
					anggotaPengajuanPenelitianDanPengabdians.add(anggotaPengajuanPenelitianDanPengabdian);
				} else {
					Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", s.trim())).setMaxResults(1)
							.uniqueResult();
					if (mahasiswa != null) {
						AnggotaPengajuanPenelitianDanPengabdian anggotaPengajuanPenelitianDanPengabdian = new AnggotaPengajuanPenelitianDanPengabdian();
						anggotaPengajuanPenelitianDanPengabdian.setMahasiswa(mahasiswa);
						anggotaPengajuanPenelitianDanPengabdian
								.setPengajuanPenelitianDanPengabdian(pengajuanPenelitianDanPengabdianData);
						anggotaPengajuanPenelitianDanPengabdians.add(anggotaPengajuanPenelitianDanPengabdian);
					}

//					else {
//						MyMessageboxConfig.show("Username anggota \"" + s + "\" tidak ditemukan, coba periksa kembali",
//								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
//						return false;
//					}
				}
			}
		}

		session.createSQLQuery(
				"delete from penelitiandanpengabdian.anggota_pengajuan_penelitian_dan_pengabdian where pengajuan_penelitian_dan_pengabdian="
						+ pengajuanPenelitianDanPengabdianData.getId())
				.executeUpdate();

		for (AnggotaPengajuanPenelitianDanPengabdian anggotaPengajuanPenelitianDanPengabdian : anggotaPengajuanPenelitianDanPengabdians) {
			anggotaPengajuanPenelitianDanPengabdian
					.setPengajuanPenelitianDanPengabdian(pengajuanPenelitianDanPengabdianData);
			Common.refreshSaveOrUpdate(session, anggotaPengajuanPenelitianDanPengabdian);
		}

		if (f != null) {

			try {
				Session sessionmy = StreamingHibernateUtil.getInstance().currentSession();

				sessionmy.refresh(f);
				f.setRef(pengajuanPenelitianDanPengabdianData.getId());

				sessionmy.getTransaction().begin();
				sessionmy.update(f);
				sessionmy.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

			File ff = f.ambilFile();
			String mimeType = Files.probeContentType(ff.toPath());
			FilePengajuanPenelitianDanPengabdian filePengajuanPengajuanPenelitianDanPengabdian = new FilePengajuanPenelitianDanPengabdian();
			filePengajuanPengajuanPenelitianDanPengabdian.setMimeType(mimeType);
			filePengajuanPengajuanPenelitianDanPengabdian.setNama(ff.getName());
			filePengajuanPengajuanPenelitianDanPengabdian.setPath(ff.getAbsolutePath());
			filePengajuanPengajuanPenelitianDanPengabdian
					.setPengajuanPenelitianDanPengabdian(pengajuanPenelitianDanPengabdianData);
			filePengajuanPengajuanPenelitianDanPengabdian.setUploadDate(ais.ui.util.WaktuUtil.getDate());
			session.save(filePengajuanPengajuanPenelitianDanPengabdian);

			HttpServletRequest request = (HttpServletRequest) (ExecutionsCtrl.getCurrent() == null ? null
					: ExecutionsCtrl.getCurrent().getNativeRequest());
			String url = "http" + (Common.isSecure(request) ? "s" : "") + "://" + request.getServerName() + ":"
					+ request.getServerPort() + request.getContextPath()
					+ "/FilePengajuanPengajuanPenelitianDanPengabdian?id="
					+ filePengajuanPengajuanPenelitianDanPengabdian.getId();
			pengajuanPenelitianDanPengabdianData.setPathUrl(url);
			Common.refreshSaveOrUpdate(session, pengajuanPenelitianDanPengabdianData);
		}

		if (sTugas != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(sTugas);
				sTugas.setRef(pengajuanPenelitianDanPengabdianData.getId());

				session.getTransaction().begin();
				session.update(sTugas);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (sKeterangan != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(sKeterangan);
				sKeterangan.setRef(pengajuanPenelitianDanPengabdianData.getId());

				session.getTransaction().begin();
				session.update(sKeterangan);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (sRekomendasi != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(sRekomendasi);
				sRekomendasi.setRef(pengajuanPenelitianDanPengabdianData.getId());

				session.getTransaction().begin();
				session.update(sRekomendasi);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		return true;
	}

	private boolean punyaSumberDana(PengajuanPenelitianDanPengabdian pengajuan,
			SumberDanaPenelitianDanPengabdian sumberDana) {
		if (pengajuan == null || sumberDana == null || sumberDana.getId() == null
				|| pengajuan.getSumberDanaPenelitianDanPengabdianes() == null) {
			return false;
		}
		for (SumberDanaPenelitianDanPengabdian s : pengajuan.getSumberDanaPenelitianDanPengabdianes()) {
			if (s != null && sumberDana.getId().equals(s.getId())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pengajuan Penelitian dan Pengabdian";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return pengajuanPenelitianDanPengabdianData;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return PengajuanPenelitianDanPengabdian.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;

	}

	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("unchecked")
	private MyGrid displayWindowPengajuanBaru(Component parent,
			final PenelitianDanPengabdian penelitianDanPengabdianData,
			final PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdianData,
			final TipePenelitianDanPengabdian jenis) throws Exception {
		this.penelitianDanPengabdianData = penelitianDanPengabdianData;
		this.pengajuanPenelitianDanPengabdianData = pengajuanPenelitianDanPengabdianData;
		this.jenis = penelitianDanPengabdianData != null ? penelitianDanPengabdianData.getTipePenelitianDanPengabdian()
				: jenis;
		DetailArtikelHelper.initdataAwal();

		MyGrid grid = new MyGrid();

		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File " + (jenis == null ? "" : jenis.getIsi())));

		f = null;

		if (jenis != null) {
			Hbox hbox1 = new Hbox();

			hbox1.setParent(row);
			LampiranLain.createDownloadUploadFileLain(hbox1,
					pengajuanPenelitianDanPengabdianData == null ? null : pengajuanPenelitianDanPengabdianData.getId(),
					"File " + jenis.getIsi(), jenis.getIsi(), false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							f = (LampiranLain) arg0.getData();
							if (pengajuanPenelitianDanPengabdianData != null
									&& pengajuanPenelitianDanPengabdianData.getId() != null) {
								try {
									Session session = StreamingHibernateUtil.getInstance().currentSession();

									session.refresh(f);
									f.setRef(pengajuanPenelitianDanPengabdianData.getId());

									session.getTransaction().begin();
									session.update(f);
									session.getTransaction().commit();

									StreamingHibernateUtil.getInstance().closeSession();
								} catch (Exception e) {
									StreamingHibernateUtil.getInstance().rollbackTransaction();
									Common.tampilErrorJikaAdmin(e);
								}
							}
						}
					}, null, false, false, false, !persetujuan);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan oleh" + " (*)"));

		Hbox hboxDiajukan = new Hbox();
		hboxDiajukan.setParent(row);
		if (usernamePengajuan != null) {
			Tbmuser tbmuser = (Tbmuser) ConstantValues.simpleObject(
					HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("userId", usernamePengajuan)).setMaxResults(1),
					Tbmuser.class);
			pengajuanPenelitianDanPengabdianData.setTbmuser(tbmuser);
		}

		final Label labelPengguna;
		hboxDiajukan.appendChild(labelPengguna = new Label(ais.common.Common.getBahasaConfig("Ambil Pengguna:")));
		tbmuserD = new AmbilDataTbmuserBanbox();
		if (persetujuan) {
			hboxDiajukan.appendChild(new Label(pengajuanPenelitianDanPengabdianData == null
					|| pengajuanPenelitianDanPengabdianData.getTbmuser() == null ? ""
							: pengajuanPenelitianDanPengabdianData.getTbmuser().getUserNama()));
		} else {
			hboxDiajukan.appendChild(tbmuserD);
		}
		tbmuserD.setDiperuntukkan(diperuntukkanPengajuan);
		tbmuserD.setValue(pengajuanPenelitianDanPengabdianData == null
				|| pengajuanPenelitianDanPengabdianData.getTbmuser() == null ? ""
						: pengajuanPenelitianDanPengabdianData.getTbmuser().getUserNama());
		tbmuserD.setAttribute("tbmuser", pengajuanPenelitianDanPengabdianData == null ? null
				: pengajuanPenelitianDanPengabdianData.getTbmuser());
		tbmuserD.setWidth("200px");

		if (pengajuanPenelitianDanPengabdianData != null && pengajuanPenelitianDanPengabdianData.getTbmuser() != null) {
			tbmuserD.setDisabled(true);
		}

		labelPengguna.setVisible(
				diperuntukkanPengajuan == null || diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_UMUM));
		tbmuserD.setVisible(
				diperuntukkanPengajuan == null || diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_UMUM)
						|| diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_DOSEN)
						|| diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_PEGAWAI));

		if (pengajuanPenelitianDanPengabdianData.getTbmuser() == null && usernamePengajuan != null) {
			Mahasiswa mahasiswa = (Mahasiswa) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", usernamePengajuan)).setMaxResults(1)
					.uniqueResult();
			pengajuanPenelitianDanPengabdianData.setMahasiswa(mahasiswa);
		}

		final Label labelMahasiswa;
		hboxDiajukan.appendChild(labelMahasiswa = new Label(ais.common.Common.getBahasaConfig("Mahasiswa:")));
		mahasiswa = new AmbilDataMahasiswaBanbox();
		if (persetujuan) {
			hboxDiajukan.appendChild(new Label(pengajuanPenelitianDanPengabdianData == null
					|| pengajuanPenelitianDanPengabdianData.getMahasiswa() == null ? ""
							: pengajuanPenelitianDanPengabdianData.getMahasiswa().getNama()));
		} else {
			hboxDiajukan.appendChild(mahasiswa);
		}
		mahasiswa.setValue(pengajuanPenelitianDanPengabdianData == null
				|| pengajuanPenelitianDanPengabdianData.getMahasiswa() == null ? ""
						: pengajuanPenelitianDanPengabdianData.getMahasiswa().getNama());
		mahasiswa.setAttribute("mahasiswa", pengajuanPenelitianDanPengabdianData == null ? null
				: pengajuanPenelitianDanPengabdianData.getMahasiswa());
		mahasiswa.setAttribute("myValue", pengajuanPenelitianDanPengabdianData == null ? null
				: pengajuanPenelitianDanPengabdianData.getMahasiswa());
		mahasiswa.setWidth("200px");

		if (pengajuanPenelitianDanPengabdianData != null
				&& pengajuanPenelitianDanPengabdianData.getMahasiswa() != null) {
			mahasiswa.setDisabled(true);
		}

		labelMahasiswa.setVisible(
				diperuntukkanPengajuan == null || diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_UMUM));
		mahasiswa.setVisible(
				diperuntukkanPengajuan == null || diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_UMUM)
						|| diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_MAHASISWA));

		if (diperuntukkanPengajuan == null || diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_UMUM)) {
			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					mahasiswa.setVisible(true);
					mahasiswa.setVisible(true);
					labelPengguna.setVisible(true);
					labelMahasiswa.setVisible(true);
					if (mahasiswa.getAttribute("mahasiswa") == null && tbmuserD.getAttribute("tbmuser") == null) {
						mahasiswa.setVisible(true);
						mahasiswa.setVisible(true);
						labelPengguna.setVisible(true);
						labelMahasiswa.setVisible(true);
					} else if (mahasiswa.getAttribute("mahasiswa") == null) {
						mahasiswa.setVisible(false);
						labelMahasiswa.setVisible(false);
					} else if (tbmuserD.getAttribute("tbmuser") == null) {
						tbmuserD.setVisible(false);
						labelPengguna.setVisible(false);
					}

				}
			};

			eventListener.onEvent(null);
			mahasiswa.setEventListener(eventListener);
			tbmuserD.setEventListener(eventListener);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal " + (jenis == null ? "Penelitian/Pengandian" : jenis)));
		tanggal = new MyDatebox(pengajuanPenelitianDanPengabdianData.getTanggal());
		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat6.get().format(pengajuanPenelitianDanPengabdianData.getTanggal())));
		} else {
			row.appendChild(tanggal);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih " + (jenis == null ? "Penelitian/Pengandian" : jenis)));
		penelitianDanPengabdian = new Combobox();
		if (persetujuan) {
			row.appendChild(
					new Label(penelitianDanPengabdianData == null ? "" : penelitianDanPengabdianData.getJudul()));
		} else {
			row.appendChild(penelitianDanPengabdian);
		}
		penelitianDanPengabdian.setWidth("90%");

		Session session = HibernateUtil.currentSession();
		Criteria criteriaTest = session.createCriteria(PenelitianDanPengabdian.class)
				.add(Restrictions.and(Restrictions.eq("aktif", true),
						diperuntukkanPengajuan == null || diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_UMUM)
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_UMUM),
										Restrictions.eq("diperuntukkan", diperuntukkanPengajuan))))

				.add(jenis == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.isNull("tipePenelitianDanPengabdian"),
								Restrictions.eq("tipePenelitianDanPengabdian", jenis)));

		int testSize = ((Number) criteriaTest.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (testSize == 0) {
			PenelitianDanPengabdian p = new PenelitianDanPengabdian();
			p.setNama(jenis + " " + ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
			p.setJudul(jenis + " " + ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
			p.setDibuka(true);
			p.setDiperuntukkan(diperuntukkanPengajuan);
			p.setTipePenelitianDanPengabdian(jenis);
			p.setJenisPenelitianDanPengabdian(
					(JenisPenelitianDanPengabdian) session.createCriteria(JenisPenelitianDanPengabdian.class)
							.setMaxResults(1).addOrder(Order.asc("id")).uniqueResult());
			session.save(p);
			session.flush();
			this.penelitianDanPengabdianData = p;
		}

		Criteria criteria = session.createCriteria(PenelitianDanPengabdian.class)
				.add(Restrictions.and(Restrictions.eq("aktif", true),
						diperuntukkanPengajuan == null || diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_UMUM)
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_UMUM),
										Restrictions.eq("diperuntukkan", diperuntukkanPengajuan))))

				.add(jenis == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.isNull("tipePenelitianDanPengabdian"),
								Restrictions.eq("tipePenelitianDanPengabdian", jenis)));

		List<?> list = criteria.list();
		if ((this.penelitianDanPengabdianData == null || this.penelitianDanPengabdianData.getId() == null) && list != null
				&& !list.isEmpty() && list.get(0) instanceof PenelitianDanPengabdian) {
			this.penelitianDanPengabdianData = (PenelitianDanPengabdian) list.get(0);
			if (pengajuanPenelitianDanPengabdianData != null) {
				pengajuanPenelitianDanPengabdianData.setPenelitianDanPengabdian(this.penelitianDanPengabdianData);
			}
		}

		Common.insertComboItems(penelitianDanPengabdian, "judul", "tipePenelitianDanPengabdian", list);
		Common.selectComboItem(true, penelitianDanPengabdian, this.penelitianDanPengabdianData);
		penelitianDanPengabdian.setReadonly(true);
		penelitianDanPengabdian.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PenelitianDanPengabdian selectedPenelitianDanPengabdian = (PenelitianDanPengabdian) (penelitianDanPengabdian
						.getSelectedItem() == null ? null : penelitianDanPengabdian.getSelectedItem().getValue());
				if (selectedPenelitianDanPengabdian != null) {

					Mahasiswa mhs = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");
					Tbmuser usr = (Tbmuser) tbmuserD.getAttribute("tbmuser");

					PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian = (PengajuanPenelitianDanPengabdian) HibernateUtil
							.currentSession().createCriteria(PengajuanPenelitianDanPengabdian.class)
							.add(Restrictions.eq("penelitianDanPengabdian", selectedPenelitianDanPengabdian))
							.add(Restrictions.or(Restrictions.eq("mahasiswa", mhs), Restrictions.eq("tbmuser", usr)))
							.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

					if (pengajuanPenelitianDanPengabdian != null) {

						try {
							if (window != null) {
								window.detach();
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/penelitiandanpengabdian/helper/PengajuanPenelitianDanPengabdianHelper.java:814");
							// TODO: handle exception
						}

						form(pengajuanPenelitianDanPengabdian, disposisiSop, save, setujui);
					}

				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Judul " + (jenis == null ? "Penelitian/Pengandian" : jenis) + " (*)"));
		judul = new Textbox(pengajuanPenelitianDanPengabdianData.getJudul());
		if (persetujuan) {
			row.appendChild(new Label(pengajuanPenelitianDanPengabdianData.getJudul()));
		} else {
			row.appendChild(judul);
		}

		judul.setWidth("90%");
		judul.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tujuan " + (jenis == null ? "Penelitian/Pengandian" : jenis)));
		tujuan = new MyCkEditor();
		if (pengajuanPenelitianDanPengabdianData != null
				&& pengajuanPenelitianDanPengabdianData.getStatus().equals(PengajuanPenelitianDanPengabdian.DISETUJUI)
				|| persetujuan) {
			row.appendChild(new ais.ui.util.MyHtml(pengajuanPenelitianDanPengabdianData == null ? ""
					: pengajuanPenelitianDanPengabdianData.getTujuan()));
		} else {
			row.appendChild(tujuan);
		}
		tujuan.setValue(pengajuanPenelitianDanPengabdianData.getTujuan());
		tujuan.setWidth("90%");
		tujuan.setHeight("100px");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Dana"));
		jumlahDana = new MyDoublebox(pengajuanPenelitianDanPengabdianData.getJumlahDana());
		if (persetujuan) {
			row.appendChild(
					new Label(Common.numberFormat.get().format(pengajuanPenelitianDanPengabdianData.getJumlahDana())));
		} else {
			row.appendChild(jumlahDana);
		}
		row = new MyFormRow();
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Sumber Dana"));
		sumberdana = new Vbox();
		Label sumberDataText = new Label();
		if (persetujuan) {
			row.appendChild(sumberDataText);
		} else {
			row.appendChild(sumberdana);
		}
		if (pengajuanPenelitianDanPengabdianData.getId() != null) {
			session.refresh(pengajuanPenelitianDanPengabdianData);
		}
		String ss = "";
		List<SumberDanaPenelitianDanPengabdian> sumberDanaPenelitianDanPengabdians = session
				.createCriteria(SumberDanaPenelitianDanPengabdian.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		for (SumberDanaPenelitianDanPengabdian s : sumberDanaPenelitianDanPengabdians) {
			MyCheckboxConfig checkbox = new MyCheckboxConfig(s.getNama());
			checkbox.setAttribute("nilai", s);
			sumberdana.appendChild(checkbox);

			if (pengajuanPenelitianDanPengabdianData.getId() != null
					&& punyaSumberDana(pengajuanPenelitianDanPengabdianData, s)) {
				checkbox.setChecked(true);

				ss += ss.isEmpty() ? s : "," + s;
			}
		}

		sumberDataText.setValue(ss);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lama Pengerjaan (*)"));
		masaPenugasan = new Textbox(pengajuanPenelitianDanPengabdianData.getMasaPenugasan());
		if (persetujuan) {
			row.appendChild(new Label(pengajuanPenelitianDanPengabdianData.getMasaPenugasan()));
		} else {
			row.appendChild(masaPenugasan);
		}
		masaPenugasan.setWidth("90%");

		Common.initKeterangan(rows, "Misal: 1 tahun, 6 bulan, 2 minggu, 5 hari, 8 jam, 1 semester");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Abstrak" + " (*)"));
		keterangan = new Textbox();
		if (pengajuanPenelitianDanPengabdianData != null
				&& pengajuanPenelitianDanPengabdianData.getStatus().equals(PengajuanPenelitianDanPengabdian.DISETUJUI)
				|| persetujuan) {
			row.appendChild(new ais.ui.util.MyHtml(pengajuanPenelitianDanPengabdianData == null ? ""
					: pengajuanPenelitianDanPengabdianData.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}
		keterangan.setValue(pengajuanPenelitianDanPengabdianData == null ? ""
				: pengajuanPenelitianDanPengabdianData.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setRows(15);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Kata Kunci " + (jenis == null ? "Penelitian/Pengandian" : jenis) + " (*)"));

		keyword = new Textbox(pengajuanPenelitianDanPengabdianData.getKeyword());
		if (persetujuan) {
			row.appendChild(new Label(pengajuanPenelitianDanPengabdianData.getKeyword()));
		} else {
			row.appendChild(keyword);
		}
		keyword.setWidth("90%");
		keyword.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Surat Tugas " + (jenis == null ? "" : jenis.getIsi())));

		sTugas = null;

		if (jenis != null) {
			Hbox hbox1 = new Hbox();

			hbox1.setParent(row);
			LampiranLain.createDownloadUploadFileLain(hbox1,
					pengajuanPenelitianDanPengabdianData == null ? null : pengajuanPenelitianDanPengabdianData.getId(),
					"Surat Tugas " + jenis.getIsi(), "Surat Tugas " + jenis.getIsi(), false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							sTugas = (LampiranLain) arg0.getData();
							if (pengajuanPenelitianDanPengabdianData != null
									&& pengajuanPenelitianDanPengabdianData.getId() != null) {
								try {
									Session session = StreamingHibernateUtil.getInstance().currentSession();

									session.refresh(sTugas);
									sTugas.setRef(pengajuanPenelitianDanPengabdianData.getId());

									session.getTransaction().begin();
									session.update(sTugas);
									session.getTransaction().commit();

									StreamingHibernateUtil.getInstance().closeSession();
								} catch (Exception e) {
									StreamingHibernateUtil.getInstance().rollbackTransaction();
									Common.tampilErrorJikaAdmin(e);
								}
							}
						}
					}, null, false, false, false, !persetujuan);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Surat Keterangan " + (jenis == null ? "" : jenis.getIsi())));

		sKeterangan = null;

		if (jenis != null) {
			Hbox hbox1 = new Hbox();

			hbox1.setParent(row);
			LampiranLain.createDownloadUploadFileLain(hbox1,
					pengajuanPenelitianDanPengabdianData == null ? null : pengajuanPenelitianDanPengabdianData.getId(),
					"Surat Keterangan " + jenis.getIsi(), "Surat Keterangan " + jenis.getIsi(), false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							sKeterangan = (LampiranLain) arg0.getData();
							if (pengajuanPenelitianDanPengabdianData != null
									&& pengajuanPenelitianDanPengabdianData.getId() != null) {
								try {
									Session session = StreamingHibernateUtil.getInstance().currentSession();

									session.refresh(sKeterangan);
									sKeterangan.setRef(pengajuanPenelitianDanPengabdianData.getId());

									session.getTransaction().begin();
									session.update(sKeterangan);
									session.getTransaction().commit();

									StreamingHibernateUtil.getInstance().closeSession();
								} catch (Exception e) {
									StreamingHibernateUtil.getInstance().rollbackTransaction();
									Common.tampilErrorJikaAdmin(e);
								}
							}
						}
					}, null, false, false, false, !persetujuan);
		}

		rekomnedasiWajib = Common.bolehKonfigurasi("rekomendasi_wajib_penelitian_dan_pengabdian", Konfigurasi.TIDAK_AKTIF);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Surat Rekomendasi " + (jenis == null ? "" : jenis.getIsi()) + (rekomnedasiWajib ? " *" : "")));

		sRekomendasi = null;

		if (jenis != null) {
			Hbox hbox1 = new Hbox();

			hbox1.setParent(row);
			LampiranLain.createDownloadUploadFileLain(hbox1,
					pengajuanPenelitianDanPengabdianData == null ? null : pengajuanPenelitianDanPengabdianData.getId(),
					"Surat Rekomendasi " + jenis.getIsi(), "Surat Rekomendasi " + jenis.getIsi(), false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							sRekomendasi = (LampiranLain) arg0.getData();
							if (pengajuanPenelitianDanPengabdianData != null
									&& pengajuanPenelitianDanPengabdianData.getId() != null) {
								try {
									Session session = StreamingHibernateUtil.getInstance().currentSession();

									session.refresh(sRekomendasi);
									sRekomendasi.setRef(pengajuanPenelitianDanPengabdianData.getId());

									session.getTransaction().begin();
									session.update(sRekomendasi);
									session.getTransaction().commit();

									StreamingHibernateUtil.getInstance().closeSession();
								} catch (Exception e) {
									StreamingHibernateUtil.getInstance().rollbackTransaction();
									Common.tampilErrorJikaAdmin(e);
								}
							}
						}
					}, null, false, false, false, !persetujuan);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Anggota " + (jenis == null ? "Penelitian/Pengandian" : jenis)));
		anggota = new Textbox(
				pengajuanPenelitianDanPengabdianData == null ? "" : pengajuanPenelitianDanPengabdianData.getAnggota());
		row.appendChild(anggota);
		anggota.setWidth("90%");
		anggota.setRows(3);

		Common.initKeterangan(rows,
				"Untuk memasukkan banyak Anggota, masukkan username masing-masing pengguna dengan pemisah tanda koma (,)");

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Anggota Baru", "/img/user_male_add.png");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Hbox hboxAmbilAnggotaBaru = new Hbox();
		row.appendChild(hboxAmbilAnggotaBaru);
		hboxAmbilAnggotaBaru.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Tbmuser tbmuser : tbmusers) {
								anggota.setValue(
										anggota.getValue() + (anggota.getValue().isEmpty() ? tbmuser.getUserId()
												: "," + tbmuser.getUserId()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		toolbarbutton = new MyToolbarbuttonConfig("Ambil Anggota Mahasiswa", "/img/user_male_add.png");

		hboxAmbilAnggotaBaru.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataMahasiswaBanyak ambil = new AmbilDataMahasiswaBanyak(new ArrayList<Mahasiswa>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Mahasiswa> mahasiswas = (List<Mahasiswa>) arg0.getData();
						if (mahasiswas != null && mahasiswas.size() != 0) {
							for (Mahasiswa mahasiswa : mahasiswas) {
								anggota.setValue(anggota.getValue() + (anggota.getValue().isEmpty() ? mahasiswa.getNim()
										: "," + mahasiswa.getNim()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama editor dan kontributor"));
		editorDanKontributor = new Textbox(pengajuanPenelitianDanPengabdianData.getEditorDanKontributor());
		row.appendChild(editorDanKontributor);
		editorDanKontributor.setWidth("90%");
		editorDanKontributor.setRows(2);

		return grid;

	}

	public static DspaceInformation getDspacePengajuanPenelitianDanPengabdian(String cookie,
			PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian) throws Exception {
		Jurusan jurusan = null;

		if (pengajuanPenelitianDanPengabdian.getMahasiswa() != null) {
			jurusan = pengajuanPenelitianDanPengabdian.getMahasiswa().getJurusan();
		} else if (pengajuanPenelitianDanPengabdian.getTbmuser() != null
				&& pengajuanPenelitianDanPengabdian.getTbmuser().getDosen() != null) {
			jurusan = pengajuanPenelitianDanPengabdian.getTbmuser().getDosen().getJurusan();
		}

		String label_pengajuanPenelitianDanPengabdian = pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian()
				.getTipePenelitianDanPengabdian().getIsi();

		String description = label_pengajuanPenelitianDanPengabdian + " untuk " + Common.getBahasaConfig("Jurusan")
				+ " " + jurusan.getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", label_pengajuanPenelitianDanPengabdian);
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", label_pengajuanPenelitianDanPengabdian + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi(
				"dspace_label_collection_pengajuanPenelitianDanPengabdian_" + pengajuanPenelitianDanPengabdian
						.getPenelitianDanPengabdian().getTipePenelitianDanPengabdian().getId() + "_" + jurusan.getId(),
				"");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "communities",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/communities");

	}

	public static DspaceInformation getDspaceTipePengajuanPenelitianDanPengabdianTahun(String cookie,
			PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian) throws Exception {
		Jurusan jurusan = null;

		if (pengajuanPenelitianDanPengabdian.getMahasiswa() != null) {
			jurusan = pengajuanPenelitianDanPengabdian.getMahasiswa().getJurusan();
		} else if (pengajuanPenelitianDanPengabdian.getTbmuser() != null
				&& pengajuanPenelitianDanPengabdian.getTbmuser().getDosen() != null) {
			jurusan = pengajuanPenelitianDanPengabdian.getTbmuser().getDosen().getJurusan();
		}
		PenelitianDanPengabdian penelitianDanPengabdian = pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian();

		String label_pengajuanPenelitianDanPengabdian = penelitianDanPengabdian.getJudul();

		String description = label_pengajuanPenelitianDanPengabdian + " untuk " + Common.getBahasaConfig("Jurusan")
				+ " " + jurusan.getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getTahun().toString());
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", label_pengajuanPenelitianDanPengabdian + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi("dspace_label_collection_penelitianDanPengabdian_tahun_"
				+ jurusan.getId() + "_" + penelitianDanPengabdian.getId() + "_"
				+ pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getTahun(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + getDspaceTipePengajuanPenelitianDanPengabdian(cookie, pengajuanPenelitianDanPengabdian)
						+ "/collections");

	}

	public static DspaceInformation getDspaceTipePengajuanPenelitianDanPengabdian(String cookie,
			PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian) throws Exception {
		Jurusan jurusan = null;

		if (pengajuanPenelitianDanPengabdian.getMahasiswa() != null) {
			jurusan = pengajuanPenelitianDanPengabdian.getMahasiswa().getJurusan();
		} else if (pengajuanPenelitianDanPengabdian.getTbmuser() != null
				&& pengajuanPenelitianDanPengabdian.getTbmuser().getDosen() != null) {
			jurusan = pengajuanPenelitianDanPengabdian.getTbmuser().getDosen().getJurusan();
		}
		PenelitianDanPengabdian penelitianDanPengabdian = pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian();

		String label_pengajuanPenelitianDanPengabdian = penelitianDanPengabdian.getJudul();

		String description = label_pengajuanPenelitianDanPengabdian + " untuk " + Common.getBahasaConfig("Jurusan")
				+ " " + jurusan.getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", label_pengajuanPenelitianDanPengabdian);
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", label_pengajuanPenelitianDanPengabdian + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi("dspace_label_collection_penelitianDanPengabdian_"
				+ jurusan.getId() + "_" + penelitianDanPengabdian.getId(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + getDspacePengajuanPenelitianDanPengabdian(cookie, pengajuanPenelitianDanPengabdian)
						+ "/collections");

	}

	@SuppressWarnings("unchecked")
	public static DspaceInformation getDspace(String cookie,
			PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian, boolean update) throws Exception {

		JSONArray jsonArray = new JSONArray();

		String nama = "";
		if (pengajuanPenelitianDanPengabdian.getMahasiswa() != null) {
			nama = pengajuanPenelitianDanPengabdian.getMahasiswa().getNama();
		} else if (pengajuanPenelitianDanPengabdian.getTbmuser() != null) {
			nama = pengajuanPenelitianDanPengabdian.getTbmuser().getUserNama();
		}

		JSONObject jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.contributor.author");
		jsonMetadata.put("value", nama);
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.contributor.editor");
		jsonMetadata.put("value", nama);
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.date.copyright");
		jsonMetadata.put("value",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonArray.put(jsonMetadata);

		List<AnggotaPengajuanPenelitianDanPengabdian> anggotaPengajuanPenelitianDanPengabdians = HibernateUtil
				.currentSession().createCriteria(AnggotaPengajuanPenelitianDanPengabdian.class)
				.add(Restrictions.eq("pengajuanPenelitianDanPengabdian", pengajuanPenelitianDanPengabdian)).list();

		for (AnggotaPengajuanPenelitianDanPengabdian anggota : anggotaPengajuanPenelitianDanPengabdians) {
			String oleh = "";
			if (anggota.getMahasiswa() != null) {
				oleh = anggota.getMahasiswa().getNama();
			} else if (anggota.getTbmuser() != null) {
				oleh = anggota.getTbmuser().getUserNama();
			}
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.contributor.advisor");
			jsonMetadata.put("value", oleh);
			jsonArray.put(jsonMetadata);
		}

		Html2Text parser = new Html2Text();
		parser.parse(new StringReader(pengajuanPenelitianDanPengabdian.getKeterangan()));

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description.abstract");
		jsonMetadata.put("value", parser.getText());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.type");
		jsonMetadata.put("value", pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian()
				.getTipePenelitianDanPengabdian().getIsi());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value", pengajuanPenelitianDanPengabdian.getJudul());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.subject");
		jsonMetadata.put("value", pengajuanPenelitianDanPengabdian.getKeyword());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.publisher");
		jsonMetadata.put("value", Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonArray.put(jsonMetadata);

		if (pengajuanPenelitianDanPengabdian.getTanggal() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.date.issued");
			jsonMetadata.put("value", Common.databaseDateFormat.get().format(pengajuanPenelitianDanPengabdian.getTanggal()));
			jsonArray.put(jsonMetadata);
		}

		LampiranLain lampiranLain = LampiranLain.ambil(pengajuanPenelitianDanPengabdian.getId(),
				"File " + pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getTipePenelitianDanPengabdian()
						.getIsi());

		if (lampiranLain != null) {
			String uri = lampiranLain.createLinkUri(false);
			if (uri != null && !uri.trim().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", uri);
				jsonArray.put(jsonMetadata);
			}
		}

		boolean berdasarkanTahun = Common.bolehKonfigurasi("export_penelitian_dan_pengabdian_dspace_berdasarkan_tahun", Konfigurasi.TIDAK_AKTIF);

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("metadata", jsonArray);

		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie, pengajuanPenelitianDanPengabdian,
				jsonPost.toString(), jsonArray.toString(), update, "items",
				"collections/" + (berdasarkanTahun
						? getDspaceTipePengajuanPenelitianDanPengabdianTahun(cookie, pengajuanPenelitianDanPengabdian)
						: getDspaceTipePengajuanPenelitianDanPengabdian(cookie, pengajuanPenelitianDanPengabdian))
						+ "/items",
				"items/{uuid}/metadata");

		if (lampiranLain != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
					"Lampiran " + pengajuanPenelitianDanPengabdian.getJudul());
		}

		return dspaceInformation;
	}

	private Boolean ases = false;
	private Tbmuser tbmuser;
	private Combobox caristatus;
	private Combobox cariTahapPengajuan;
	private Textbox cariJudul;
	private Textbox cariAbstrak;
	private MyGrid gridPengajuanAnggota;
	private Row r1Anggota;

	@SuppressWarnings("unchecked")
	public void displayPengajuan(final Boolean ases, final String usernamePengajuan,
			final String diperuntukkanPengajuan, final PenelitianDanPengabdian penelitianDanPengabdianData,
			final Component component, final MyWindow window, final TipePenelitianDanPengabdian jenis,
			final String tinggi) {

		this.jenis = penelitianDanPengabdianData == null
				|| penelitianDanPengabdianData.getTipePenelitianDanPengabdian() == null ? jenis
						: penelitianDanPengabdianData.getTipePenelitianDanPengabdian();
		this.usernamePengajuan = usernamePengajuan;
		this.diperuntukkanPengajuan = diperuntukkanPengajuan;

		this.ases = ases;

		if (usernamePengajuan != null && !usernamePengajuan.trim().isEmpty()) {
			tbmuser = (Tbmuser) ConstantValues.simpleObject(
					HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("userId", usernamePengajuan)).setMaxResults(1),
					Tbmuser.class);
		}

		System.out.println("usernamePengajuan => " + usernamePengajuan + ", ases = " + ases);

		Borderlayout myborderlayout = new ais.ui.util.MyBorderlayout();
		myborderlayout.setParent(component instanceof Tabpanel ? Common.tampilanScroll(component) : component);
		myborderlayout.setHeight(tinggi);

		North mynorth = new North();
		mynorth.setParent(myborderlayout);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(mynorth);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig((jenis == null ? "Penelitian/Pengabdian" : jenis) + ""));

		searchPenelitianDanPengabdian = new Combobox();

		Common.insertComboDanSemua(searchPenelitianDanPengabdian, "judul", "jenisPenelitianDanPengabdian",
				PenelitianDanPengabdian.class,
				Restrictions.and(Restrictions.and(Restrictions.eq("aktif", true),
						diperuntukkanPengajuan == null || diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_UMUM)
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_UMUM),
										Restrictions.eq("diperuntukkan", diperuntukkanPengajuan))),
						jenis == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("tipePenelitianDanPengabdian"),
										Restrictions.eq("tipePenelitianDanPengabdian", jenis))));

		Common.selectComboItem(searchPenelitianDanPengabdian, penelitianDanPengabdianData);
		if (penelitianDanPengabdianData != null) {
			searchPenelitianDanPengabdian.setDisabled(true);
		}
		searchPenelitianDanPengabdian.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataPengajuan();
			}
		});
		searchPenelitianDanPengabdian.setWidth("90%");

		row.appendChild(searchPenelitianDanPengabdian);

		cariPengaju = new Textbox();
		if (usernamePengajuan == null || usernamePengajuan.trim().isEmpty()) {
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Diajukan oleh")));
			cariPengaju.setParent(row);
			cariPengaju.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadDataPengajuan();
				}
			});
		}
		cariPengaju.setWidth("90%");

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Judul")));
		cariJudul = new Textbox();
		cariJudul.setParent(row);
		cariJudul.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataPengajuan();
			}
		});
		cariJudul.setWidth("90%");

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Abstrak")));
		cariAbstrak = new Textbox();
		cariAbstrak.setParent(row);
		cariAbstrak.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataPengajuan();
			}
		});
		cariAbstrak.setWidth("90%");

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setParent(row);
		cari.setTooltiptext("Cari");
		cari.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadDataPengajuan();
			}
		});

		Center mycenter = new Center();
		mycenter.setParent(myborderlayout);
		ais.ui.util.ZkCompat.setFlex(mycenter, true);
		mycenter.setBorder("none");

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataPengajuan();
			}
		});

		pagingAnggota = new Paging();
		Common.initPaging(pagingAnggota, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataPengajuanAnggota();
			}
		});

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setVisible(!readonly);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(mycenter);
		borderlayout.setHeight(tinggi);

		North north = new North();
		north.setParent(borderlayout);
		toolbar.setParent(north);
		north.setBorder("none");

		Center center1 = new Center();
		center1.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center1, true);
		center1.setBorder("none");

		Row rowUtama = Common.tampilanScroll1(center1);

		if (jenis == null) {

			Map<Long, GeneralValueObject> tipePenelitianDanPengabdians = ConstantValues
					.ambilBerdasarClass(TipePenelitianDanPengabdian.class);
			for (GeneralValueObject s : tipePenelitianDanPengabdians.values()) {
				final TipePenelitianDanPengabdian tipePenelitianDanPengabdian = (TipePenelitianDanPengabdian) s;
				if (tipePenelitianDanPengabdian.getAktif()) {
					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
							"Ajukan " + tipePenelitianDanPengabdian.getIsi(), "/img/new.gif");
					button.setVisible(!ases);
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {

							PenelitianDanPengabdian penelitian = penelitianDanPengabdianData == null
									? new PenelitianDanPengabdian()
									: penelitianDanPengabdianData;
							penelitian.setTipePenelitianDanPengabdian(tipePenelitianDanPengabdian);

							PengajuanPenelitianDanPengabdianHelper.this.jenis = tipePenelitianDanPengabdian;
							PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdianData = new PengajuanPenelitianDanPengabdian();
							pengajuanPenelitianDanPengabdianData.setPenelitianDanPengabdian(penelitian);

							MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
							save.setAttribute("parent",
									ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							form(pengajuanPenelitianDanPengabdianData, null, save, null);

						}

					});
					button.setParent(toolbar);
				}
			}
		} else {
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
					"Ajukan " + (jenis == null ? "Penelitian/Pengabdian" : jenis), "/img/new.gif");
			button.setVisible(!ases);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					PenelitianDanPengabdian penelitian = penelitianDanPengabdianData == null
							? new PenelitianDanPengabdian()
							: penelitianDanPengabdianData;
					penelitian.setTipePenelitianDanPengabdian(jenis);

					PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdianData = new PengajuanPenelitianDanPengabdian();
					pengajuanPenelitianDanPengabdianData.setPenelitianDanPengabdian(penelitian);
					PengajuanPenelitianDanPengabdianHelper.this.jenis = jenis;

					MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
					save.setAttribute("parent", ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					form(pengajuanPenelitianDanPengabdianData, null, save, null);
				}

			});
			button.setParent(toolbar);
		}

		String[] contents = new String[] { "id", "penelitianDanPengabdian", "tbmuser", "mahasiswa", "status",
				"tahapPengajuan", "anggota", "pathUrl", "judul", "keterangan", "tujuan", "jumlahDana",
				"sumberDanaPenelitianDanPengabdianes", "tahapPengajuan", "kodeUnik" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(PengajuanPenelitianDanPengabdian.class,
				this, "Download", "/img/print.png", contents);
		toolbar.appendChild(cetakToolbarbutton);

		if (penelitianDanPengabdianData != null) {
			List<String> koresponden = new ArrayList<String>();
			for (String s : penelitianDanPengabdianData.getKorespondensi().split(",")) {
				if (!s.trim().isEmpty()) {
					koresponden.add(s.trim());
				}
			}

			Tbmuser tbmuser = Common.getCurrentUser();
			MyToolbarbuttonConfig upload = Common.uploadData(this, PengajuanPenelitianDanPengabdian.class, contents);
			upload.setVisible(!ases && koresponden.contains(tbmuser.getUserId()) && tbmuser.getMahasiswa() == null
					&& tbmuser.ambilDosen() == null);
			toolbar.appendChild(upload);
		}

		cetakToolbarbutton = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
		toolbar.appendChild(cetakToolbarbutton);
		cetakToolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				LaporanPenelitian laporanPenelitian = new LaporanPenelitian(
						PengajuanPenelitianDanPengabdianHelper.this.usernamePengajuan, jenis);
				laporanPenelitian.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				laporanPenelitian.setTitle("Cetak Laporan " + (jenis == null ? "Penelitian/Pengabdian" : ""));
				laporanPenelitian.setClosable(true);
				laporanPenelitian.setHeight("99%");
				laporanPenelitian.setWidth("90%");
				laporanPenelitian.onModal();
			}
		});

		MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor", "/img/corner.gif");
		toolbar.appendChild(exportKeOjs);
		exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("penelitian_dan_pengabdian_terhubung_ke_dspace"));
		exportKeOjs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Intbox intbox = new Intbox(0);
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (intbox.getValue() == 0) {
							MyMessageboxConfig.show("Data tidak ditemukan, khusus untuk " + jenis.getIsi()
									+ " dosen, dosen harus mempunya HOMEBASE PRODI terlebih dahulu sebelum bisa mempublikasikan ke dalam repository",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						onSearchDefault(arg0);
						LogLoginAction.tampilDpsaceLog();
					}
				});

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							String cookie = DspaceCommon.login();
							List<PengajuanPenelitianDanPengabdian> pengajuanPenelitianDanPengabdians = initCriteria(
									true).add(Restrictions.eq("status", PengajuanPenelitianDanPengabdian.DISETUJUI))
									.list();
							intbox.setValue(pengajuanPenelitianDanPengabdians.size());

							int rowIndex = 1;
							for (PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian : pengajuanPenelitianDanPengabdians) {
								label.setValue(
										"Sedang memproses data " + pengajuanPenelitianDanPengabdian.toString() + " ("
												+ Common.numberFormat.get().format(
														(rowIndex++) * 100.0 / pengajuanPenelitianDanPengabdians.size())
												+ " %)");
								PengajuanPenelitianDanPengabdianHelper.getDspace(cookie,
										pengajuanPenelitianDanPengabdian, true);
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
						}
						label.setValue("");
					}
				}).start();
			}
		});

		MyToolbarbuttonConfig batalExport = new MyToolbarbuttonConfig("Batalkan Ekspor", "/img/svg/trash.svg");
		toolbar.appendChild(batalExport);
		batalExport.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("penelitian_dan_pengabdian_terhubung_ke_dspace"));
		batalExport.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin membatalkan ekspor data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											onSearchDefault(arg0);
											LogLoginAction.tampilDpsaceLog();
										}
									});

									new Thread(new Runnable() {

										@Override
										public void run() {
											try {
											try {
												String cookie = DspaceCommon.login();
												List<PengajuanPenelitianDanPengabdian> pengajuanPenelitianDanPengabdians = initCriteria(
														true)
														.createAlias("tbmuser.dosen", "dosen", Criteria.LEFT_JOIN)
														.add(Restrictions.or(
																Restrictions.isNotNull("mahasiswa.jurusan"),
																Restrictions.isNotNull("dosen.jurusan")))
														.add(Restrictions.eq("status",
																PengajuanPenelitianDanPengabdian.DISETUJUI))
														.list();

												int rowIndex = 1;
												for (PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian : pengajuanPenelitianDanPengabdians) {
													label.setValue("Sedang memproses data "
															+ pengajuanPenelitianDanPengabdian.toString() + " ("
															+ Common.numberFormat.get().format((rowIndex++) * 100.0
																	/ pengajuanPenelitianDanPengabdians.size())
															+ " %)");
													DspaceInformation dspaceInformation = DspaceInformation
															.getDspaceInformation(
																	PengajuanPenelitianDanPengabdian.class.getName(),
																	pengajuanPenelitianDanPengabdian.getId());
													if (dspaceInformation != null) {
														int i = DspaceInformation.delete(cookie,
																"items/" + dspaceInformation.getUuid(),
																dspaceInformation.getPostInfo());
														if (i == 200) {

															Session session = HibernateUtil.currentNativeSession();
															session.getTransaction().begin();
															session.delete(dspaceInformation);
															session.getTransaction().commit();
															HibernateUtil.closeSession();
														}
													}
												}
											} catch (Exception e) {
												// TODO Auto-generated catch
												// block
												Common.tampilErrorJikaAdmin(e);
											}
											label.setValue("");
																					} finally {
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

								}

							}
						});
			}
		});

		batalExport = new MyToolbarbuttonConfig("Setujui Semua", "/img/svg/check2.svg");
		toolbar.appendChild(batalExport);
		batalExport.setVisible(Common.getApakahAdmin());
		batalExport.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin mensetujui semua ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											onSearchDefault(arg0);
										}
									});

									new Thread(new Runnable() {

										@Override
										public void run() {
											try {
											try {
												List<PengajuanPenelitianDanPengabdian> pengajuanPenelitianDanPengabdians = initCriteria(
														true).list();

												int rowIndex = 1;
												for (PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian : pengajuanPenelitianDanPengabdians) {
													label.setValue("Sedang memproses data "
															+ pengajuanPenelitianDanPengabdian.toString() + " ("
															+ Common.numberFormat.get().format((rowIndex++) * 100.0
																	/ pengajuanPenelitianDanPengabdians.size())
															+ " %)");

													Session session = HibernateUtil.currentNativeSession();
													try {
														session.refresh(pengajuanPenelitianDanPengabdian);
														pengajuanPenelitianDanPengabdian
																.setStatus(PengajuanPenelitianDanPengabdian.DISETUJUI);
														session.getTransaction().begin();
														session.update(pengajuanPenelitianDanPengabdian);
														session.getTransaction().commit();
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/penelitiandanpengabdian/helper/PengajuanPenelitianDanPengabdianHelper.java:1833");
														// TODO: handle exception
													}
													HibernateUtil.closeSession();

													session = HibernateUtil.currentNativeSession();
													List<PengajuanTahapanPelaporanPenelitianDanPengabdian> danPengabdians = session
															.createCriteria(
																	PengajuanTahapanPelaporanPenelitianDanPengabdian.class)
															.add(Restrictions.eq("pengajuanPenelitianDanPengabdian",
																	pengajuanPenelitianDanPengabdian))
															.list();

													for (PengajuanTahapanPelaporanPenelitianDanPengabdian tahapanPelaporanPenelitianDanPengabdian : danPengabdians) {
														tahapanPelaporanPenelitianDanPengabdian
																.setStatus(PengajuanPenelitianDanPengabdian.DISETUJUI);
														try {
															session.getTransaction().begin();
															session.update(tahapanPelaporanPenelitianDanPengabdian);
															session.getTransaction().commit();
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/penelitiandanpengabdian/helper/PengajuanPenelitianDanPengabdianHelper.java:1853");
															// TODO: handle exception
														}

													}

													HibernateUtil.closeSession();
												}
											} catch (Exception e) {
												// TODO Auto-generated catch
												// block
												Common.tampilErrorJikaAdmin(e);
											}
											label.setValue("");
																					} finally {
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

								}

							}
						});
			}
		});

		caristatus = new Combobox();
		caristatus.setParent(toolbar);
		MyComboitemConfig comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.BELUM_DIPROSES);
		comboitem.setValue(PengajuanPenelitianDanPengabdian.BELUM_DIPROSES);
		caristatus.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.SEDANG_DIPROSES);
		comboitem.setValue(PengajuanPenelitianDanPengabdian.SEDANG_DIPROSES);
		caristatus.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.DISETUJUI);
		comboitem.setValue(PengajuanPenelitianDanPengabdian.DISETUJUI);
		caristatus.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.DITOLAK);
		comboitem.setValue(PengajuanPenelitianDanPengabdian.DITOLAK);
		caristatus.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua Status");
		comboitem.setValue(null);
		caristatus.appendChild(comboitem);

		caristatus.setSelectedItem(comboitem);
		caristatus.setReadonly(true);
		caristatus.setCols(10);

		caristatus.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataPengajuan();
			}
		});

		cariTahapPengajuan = new Combobox();
		cariTahapPengajuan.setParent(toolbar);
		comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.TAHAP_PROPOSAL);
		comboitem.setValue(PengajuanPenelitianDanPengabdian.TAHAP_PROPOSAL);
		cariTahapPengajuan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.TAHAP_PENGUMPULAN_DATA);
		comboitem.setValue(PengajuanPenelitianDanPengabdian.TAHAP_PENGUMPULAN_DATA);
		cariTahapPengajuan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.TAHAP_ANALISIS_DATA);
		comboitem.setValue(PengajuanPenelitianDanPengabdian.TAHAP_ANALISIS_DATA);
		cariTahapPengajuan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.TAHAP_LAPORAN_AKHIR);
		comboitem.setValue(PengajuanPenelitianDanPengabdian.TAHAP_LAPORAN_AKHIR);
		cariTahapPengajuan.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua Tahap");
		comboitem.setValue(null);
		cariTahapPengajuan.appendChild(comboitem);

		cariTahapPengajuan.setCols(10);
		cariTahapPengajuan.setSelectedItem(comboitem);
		cariTahapPengajuan.setReadonly(true);
		cariTahapPengajuan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataPengajuan();
			}
		});

		gridPengajuan = new MyGrid();
		gridPengajuan.setMold("paging");
		gridPengajuan.setPageSize(1000);
		gridPengajuan.setParent(rowUtama);

		Columns columns = new Columns();
		columns.setParent(gridPengajuan);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.Reg");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel((jenis == null ? "Penelitian/Pengabdian" : jenis.getIsi()));
		column.setWidth(penelitianDanPengabdianData == null ? "20%" : "0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Diajukan oleh");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Abstrak");
		column.setWidth(ases ? "35%" : "45%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Anggota");
		column.setWidth("18%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth(ases ? "25%" : "15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahap");
		column.setWidth(ases ? "5%" : "8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth(ases ? "0%" : "16%");

		loadDataPengajuan();

		MyFormRow r = new MyFormRow();
		r.setParent(rowUtama.getParent());
		paging.setParent(r);

		r1Anggota = new MyFormRow();
		r1Anggota.setParent(rowUtama.getParent());
		r1Anggota.appendChild(new MyLabelBold("Sebagai Anggota"));

		MyFormRow r1 = new MyFormRow();
		r1.setParent(rowUtama.getParent());

		gridPengajuanAnggota = new MyGrid();
		gridPengajuanAnggota.setMold("paging");
		gridPengajuanAnggota.setPageSize(1000);
		gridPengajuanAnggota.setParent(r1);

		columns = new Columns();
		columns.setParent(gridPengajuanAnggota);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.Reg");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel((jenis == null ? "Penelitian/Pengabdian" : jenis.getIsi()));
		column.setWidth(penelitianDanPengabdianData == null ? "20%" : "0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Diajukan oleh");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Abstrak");
		column.setWidth(ases ? "35%" : "45%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Anggota");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth(ases ? "25%" : "15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahap");
		column.setWidth(ases ? "5%" : "8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setVisible(false);
		column.setWidth("0px");

		r = new MyFormRow();
		r.setParent(rowUtama.getParent());
		pagingAnggota.setParent(r);

		loadDataPengajuanAnggota();

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PengajuanPenelitianDanPengabdian.class)

				.add(cariJudul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("judul", cariJudul.getValue().trim(), MatchMode.ANYWHERE))

				.add(cariAbstrak.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", cariAbstrak.getValue().trim(), MatchMode.ANYWHERE))

				.add(caristatus.getSelectedItem() == null || caristatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("status", caristatus.getSelectedItem().getValue()))

				.add(cariTahapPengajuan.getSelectedItem() == null
						|| cariTahapPengajuan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahapPengajuan", cariTahapPengajuan.getSelectedItem().getValue()))

				.add(Restrictions.or(Restrictions.isNotNull("mahasiswa"), Restrictions.isNotNull("tbmuser")))

				.createAlias("tbmuser", "tbmuser", Criteria.LEFT_JOIN)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)

				.createAlias("penelitianDanPengabdian", "penelitianDanPengabdian", Criteria.LEFT_JOIN)

				.add(jenis == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("penelitianDanPengabdian.tipePenelitianDanPengabdian", jenis))

				.add(diperuntukkanPengajuan == null || diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_UMUM)
						? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.eq("penelitianDanPengabdian.diperuntukkan", PengumumanAkademis.UNTUK_UMUM),
								Restrictions.eq("penelitianDanPengabdian.diperuntukkan", diperuntukkanPengajuan)))

				.add(usernamePengajuan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("tbmuser.userId", usernamePengajuan),
								Restrictions.eq("mahasiswa.nim", usernamePengajuan)))

				.add(cariPengaju.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.or(
								Restrictions.ilike("tbmuser.userId", cariPengaju.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("tbmuser.userNama", cariPengaju.getValue().trim(),
										MatchMode.ANYWHERE)),
								Restrictions.or(
										Restrictions.ilike("mahasiswa.nim", cariPengaju.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("mahasiswa.nama", cariPengaju.getValue().trim(),
												MatchMode.ANYWHERE))))

				.add(searchPenelitianDanPengabdian.getSelectedItem() == null
						|| searchPenelitianDanPengabdian.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("penelitianDanPengabdian",
										searchPenelitianDanPengabdian.getSelectedItem().getValue()));

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}

		return criteria;
	}

	public Criteria initCriteriaSebagaiAnggota(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AnggotaPengajuanPenelitianDanPengabdian.class)

				.createAlias("tbmuser", "tbmuser", Criteria.LEFT_JOIN)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)

				.add(usernamePengajuan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("tbmuser.userId", usernamePengajuan),
								Restrictions.eq("mahasiswa.nim", usernamePengajuan)))

				.add(cariPengaju.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.or(
								Restrictions.ilike("tbmuser.userId", cariPengaju.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("tbmuser.userNama", cariPengaju.getValue().trim(),
										MatchMode.ANYWHERE)),
								Restrictions.or(
										Restrictions.ilike("mahasiswa.nim", cariPengaju.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("mahasiswa.nama", cariPengaju.getValue().trim(),
												MatchMode.ANYWHERE))))

				.setProjection(Projections.groupProperty("pengajuanPenelitianDanPengabdian"));

		if (order) {
			criteria

					.addOrder(Order.desc("pengajuanPenelitianDanPengabdian.id"));
		}

		criteria = criteria.createCriteria("pengajuanPenelitianDanPengabdian")

				.add(cariJudul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("judul", cariJudul.getValue().trim(), MatchMode.ANYWHERE))

				.add(cariAbstrak.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", cariAbstrak.getValue().trim(), MatchMode.ANYWHERE))

				.add(caristatus.getSelectedItem() == null || caristatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("status", caristatus.getSelectedItem().getValue()))

				.add(cariTahapPengajuan.getSelectedItem() == null
						|| cariTahapPengajuan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahapPengajuan", cariTahapPengajuan.getSelectedItem().getValue()))

				.add(Restrictions.or(Restrictions.isNotNull("mahasiswa"), Restrictions.isNotNull("tbmuser")))

				.createAlias("penelitianDanPengabdian", "penelitianDanPengabdian", Criteria.LEFT_JOIN)

				.add(jenis == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("penelitianDanPengabdian.tipePenelitianDanPengabdian", jenis))

				.add(diperuntukkanPengajuan == null || diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_UMUM)
						? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.eq("penelitianDanPengabdian.diperuntukkan", PengumumanAkademis.UNTUK_UMUM),
								Restrictions.eq("penelitianDanPengabdian.diperuntukkan", diperuntukkanPengajuan)))

				.add(searchPenelitianDanPengabdian.getSelectedItem() == null
						|| searchPenelitianDanPengabdian.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("penelitianDanPengabdian",
										searchPenelitianDanPengabdian.getSelectedItem().getValue()));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadDataPengajuan() {
		if (cariJudul == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);
		List<PengajuanPenelitianDanPengabdian> pengajuanPenelitianDanPengabdian = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pengajuanPenelitianDanPengabdian);
		gridPengajuan.setSclass("dgrid");
		gridPengajuan.setRowRenderer(new DetailPengajuanPenelitianDanPengabdianRenderer());
		gridPengajuan.setModelCheckMobile(strset);
		gridPengajuan.renderAll();

	}

	@SuppressWarnings("unchecked")
	public void loadDataPengajuanAnggota() {
		if (cariJudul == null) {
			return;
		}
		Common.initPaging(initCriteria(false), pagingAnggota);
		List<PengajuanPenelitianDanPengabdian> pengajuanPenelitianDanPengabdian = initCriteriaSebagaiAnggota(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (pagingAnggota == null ? 0 : pagingAnggota.getActivePage()))
				.list();

		gridPengajuanAnggota.getParent().setVisible(!pengajuanPenelitianDanPengabdian.isEmpty());
		r1Anggota.setVisible(!pengajuanPenelitianDanPengabdian.isEmpty());

		ListModel strset = new SimpleListModel(pengajuanPenelitianDanPengabdian);
		gridPengajuanAnggota.setSclass("dgrid");
		gridPengajuanAnggota.setRowRenderer(new DetailPengajuanPenelitianDanPengabdianRenderer());
		gridPengajuanAnggota.setModelCheckMobile(strset);
		gridPengajuanAnggota.renderAll();

	}

	@SuppressWarnings("unchecked")
	public static void displayRow(Row arg0, final PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian,
			final Pegawai pegawai, final boolean ases, final TipePenelitianDanPengabdian jenis) throws Exception {

		// TODO Auto-generated method stub
		arg0.setValign("top");

//		FilePengajuanPenelitianDanPengabdian content = (FilePengajuanPenelitianDanPengabdian) HibernateUtil
//				.currentSession().createCriteria(FilePengajuanPenelitianDanPengabdian.class)
//				.add(Restrictions.eq("pengajuanPenelitianDanPengabdian", pengajuanPenelitianDanPengabdian))
//				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

		Tbmuser tbmuser = Common.getCurrentUser();
		List<String> koresponden = new ArrayList<String>();
		for (String s : pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getKorespondensi().split(",")) {
			if (!s.trim().isEmpty()) {
				koresponden.add(s.trim());
			}
		}

		List<String> korespondenGrup = new ArrayList<String>();
		if (pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian() != null) {

			for (String s : pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getKorespondensiGrupPengguna()
					.split(",")) {
				if (!s.trim().isEmpty()) {
					korespondenGrup.add(s.trim());
				}
			}
		}

		final Vbox vboxKeterangan = new Vbox();
		final EventListener keteranganEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(vboxKeterangan);

				if (pengajuanPenelitianDanPengabdian.getKeterangan() != null
						&& !pengajuanPenelitianDanPengabdian.getKeterangan().trim().isEmpty()) {
					new Label("Status : " + pengajuanPenelitianDanPengabdian.getStatus()).setParent(vboxKeterangan);
				}

				Session session = HibernateUtil.currentSession();
				List<PenilaianAsesor> asesorMemberikanPenilaians = session.createCriteria(PenilaianAsesor.class)
						.createAlias("asesemenPenilaian", "asesemenPenilaian")
						.add(Restrictions.eq("asesemenPenilaian.pengajuanPenelitianDanPengabdian",
								pengajuanPenelitianDanPengabdian))
						.list();
				for (PenilaianAsesor penilaianAsesor : asesorMemberikanPenilaians) {
					new Label(penilaianAsesor.getAsesor().getAsesorPenunjangKinerjaDosen().getNama() + " : "
							+ Common.numberFormat.get().format(penilaianAsesor.getSks()) + " sks, "
							+ (penilaianAsesor.getKeterangan())
							+ (penilaianAsesor.getAsesemenPenilaian().getPegawai() == null ? ""
									: " (" + penilaianAsesor.getAsesemenPenilaian().getPegawai().getNama() + ")"))
							.setParent(vboxKeterangan);
				}
			}
		};

		if (pengajuanPenelitianDanPengabdian.getTbmuser() != null
				&& pengajuanPenelitianDanPengabdian.getTbmuser().getPegawai() != null) {
			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			EventListener eventListener = new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (detail.getChildren().isEmpty()) {
						if (ases) {
							List<AnggotaPengajuanPenelitianDanPengabdian> anggotaPengajuanPenelitianDanPengabdians = HibernateUtil
									.currentSession().createCriteria(AnggotaPengajuanPenelitianDanPengabdian.class)
									.add(Restrictions.eq("pengajuanPenelitianDanPengabdian",
											pengajuanPenelitianDanPengabdian))
									.list();
							Set<Long> treeMap = new HashSet<Long>();
							treeMap.add(pengajuanPenelitianDanPengabdian.getTbmuser().getPegawai().getId());
							for (AnggotaPengajuanPenelitianDanPengabdian anggota : anggotaPengajuanPenelitianDanPengabdians) {
								if (anggota.getTbmuser() != null && anggota.getTbmuser().getPegawai() != null) {
									treeMap.add(anggota.getTbmuser().getPegawai().getId());
								}
							}

							if (pegawai != null && treeMap.contains(pegawai.getId())
									&& pengajuanPenelitianDanPengabdian.getTbmuser() != null
									&& pengajuanPenelitianDanPengabdian.getTbmuser().getDosen() != null
									&& pengajuanPenelitianDanPengabdian.getStatus()
											.equals(PengajuanPenelitianDanPengabdian.DISETUJUI)) {

								Tabbox tabbox = new Tabbox();
								tabbox.setParent(detail);
								tabbox.setHeight("2000px");
								tabbox.setWidth("100%");

								Tabs tabs = new Tabs();
								tabs.setParent(tabbox);

								final MyTabConfig tabSoal = new MyTabConfig("Penilaian Asesor");
								tabSoal.setParent(tabs);

								final MyTabConfig tabPengajaran = new MyTabConfig(
										"Rincian " + (jenis == null ? "Penelitian/Pengabdian" : jenis));
								tabPengajaran.setParent(tabs);

								Tabpanels tabpanels = new Tabpanels();
								tabpanels.setParent(tabbox);

								Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
								tabpanelUtama.setStyle("min-height: 300px;");
								tabpanelUtama.setParent(tabpanels);

								PenilaianAsesorHelper.formNilai(pegawai, "pengajuanPenelitianDanPengabdian",
										pengajuanPenelitianDanPengabdian, null,
										pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian()
												.getTahunAkademik(),
										pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getSemester(),
										pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian()
												.getTipePenelitianDanPengabdian() + " ber-judul \""
												+ pengajuanPenelitianDanPengabdian.getJudul() + "\"",
										PenilaianAsesor.PENELITIAN_ATAU_PENGABDIAN, keteranganEventListener)
										.setParent(tabpanelUtama);

								final Tabpanel jurusanTabpanel = new ais.ui.util.MyTabpanel();
								jurusanTabpanel.setParent(tabpanels);
								jurusanTabpanel.setStyle("min-height: 1100px;");
								jurusanTabpanel.setWidth("100%");

								tabPengajaran.addEventListener("onClick", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										if (jurusanTabpanel.getChildren().isEmpty()) {
											TahapanPelaporanPenelitianDanPengabdianHelper tahapan = new TahapanPelaporanPenelitianDanPengabdianHelper();
											tahapan.displayTahapanPelaporan(ases, jenis,
													pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian(),
													pengajuanPenelitianDanPengabdian, jurusanTabpanel);
										}
									}
								});

							} else {
								TahapanPelaporanPenelitianDanPengabdianHelper tahapan = new TahapanPelaporanPenelitianDanPengabdianHelper();
								tahapan.displayTahapanPelaporan(ases, jenis,
										pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian(),
										pengajuanPenelitianDanPengabdian, detail);
							}
						} else {
							TahapanPelaporanPenelitianDanPengabdianHelper tahapan = new TahapanPelaporanPenelitianDanPengabdianHelper();
							tahapan.displayTahapanPelaporan(ases, jenis,
									pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian(),
									pengajuanPenelitianDanPengabdian, detail);
						}
					}
				}

			};
			detail.addEventListener("onOpen", eventListener);
			if (ases) {
				detail.setOpen(true);
				eventListener.onEvent(null);
			}
		} else {
			new Label().setParent(arg0);
		}

		new Label(pengajuanPenelitianDanPengabdian.noreg()).setParent(arg0);

		Vbox aa;
		(aa = RevisiHelper
				.createNewRevisi(PengajuanPenelitianDanPengabdian.class, pengajuanPenelitianDanPengabdian,
						pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian() == null ? ""
								: pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getJudul()))
				.setParent(arg0);

		Hbox hbox1 = new Hbox();

		hbox1.setParent(aa);
		LampiranLain.createDownloadUploadFileLain(hbox1,
				pengajuanPenelitianDanPengabdian == null ? null : pengajuanPenelitianDanPengabdian.getId(),
				"File " + jenis.getIsi(), jenis.getIsi(), false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain ttd = (LampiranLain) arg0.getData();
						if (pengajuanPenelitianDanPengabdian != null
								&& pengajuanPenelitianDanPengabdian.getId() != null) {
							try {
								Session session = StreamingHibernateUtil.getInstance().currentSession();

								session.refresh(ttd);
								ttd.setRef(pengajuanPenelitianDanPengabdian.getId());

								session.getTransaction().begin();
								session.update(ttd);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}
						}
					}
				}, null, false, false, false, true);

//		File file = content == null ? null : new File(content.getPath());
//		if (file == null || !file.exists()) {
//			file = null;
//		}

		A foto = null;

		String oleh = "";
		if (pengajuanPenelitianDanPengabdian.getMahasiswa() != null) {
			foto = CommonMedia.tampilkanGambarKecil(pengajuanPenelitianDanPengabdian.getMahasiswa());
			oleh = (pengajuanPenelitianDanPengabdian.getMahasiswa().getNim() + " "
					+ pengajuanPenelitianDanPengabdian.getMahasiswa().getNama());
		} else if (pengajuanPenelitianDanPengabdian.getTbmuser() != null) {
			if (pengajuanPenelitianDanPengabdian.getTbmuser().getDosen() != null) {
				foto = CommonMedia.tampilkanGambarKecil(pengajuanPenelitianDanPengabdian.getTbmuser().getDosen());
			} else if (pengajuanPenelitianDanPengabdian.getTbmuser().getPegawai() != null) {
				foto = CommonMedia.tampilkanGambarKecil(pengajuanPenelitianDanPengabdian.getTbmuser().getPegawai());
			} else {
				foto = CommonMedia.tampilkanGambarKecil(pengajuanPenelitianDanPengabdian.getTbmuser());
			}
			oleh = (pengajuanPenelitianDanPengabdian.getTbmuser().getUserNama() + " ("
					+ pengajuanPenelitianDanPengabdian.getTbmuser().getUserId() + ")");
		}

//		String url = Common.getRequestHostWithProtocol() + "/FilePengajuanPengajuanPenelitianDanPengabdian?id="
//				+ (content == null ? "-1" : content.getId());
//		pengajuanPenelitianDanPengabdian.setPathUrl(url);
//		A a;

		Vbox vbox = new Vbox();
		vbox.setParent(arg0);
		vbox.setPack("center");
		vbox.setAlign("center");

		if (foto != null) {
			foto.setParent(vbox);
		}

//		(a = new A(oleh)).setParent(vbox);
//		a.setHref(file == null || !file.exists() ? "#" : url);
//		if (file != null && file.exists()) {
//			a.setTarget("_blank");
//		}
//
//		if (file == null) {
//			a.addEventListener("onClick", new EventListener() {
//
//				@Override
//				public void onEvent(Event arg0) throws Exception {
//					// TODO Auto-generated method stub
//					MyMessageboxConfig.show("File lampiran tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
//							MyMessageboxConfig.INFORMATION);
//				}
//			});
//		}
//
//		new Label(content == null ? "" : Common.dateFormat.get().format(content.getUploadDate())).setParent(vbox);

		new MyLabelKecil(pengajuanPenelitianDanPengabdian.getKeterangan()).setParent(arg0);

		vbox = new Vbox();
		vbox.setParent(arg0);
		int i = 1;
		List<AnggotaPengajuanPenelitianDanPengabdian> anggotaPengajuanPenelitianDanPengabdians = HibernateUtil
				.currentSession().createCriteria(AnggotaPengajuanPenelitianDanPengabdian.class)
				.add(Restrictions.eq("pengajuanPenelitianDanPengabdian", pengajuanPenelitianDanPengabdian)).list();
		for (AnggotaPengajuanPenelitianDanPengabdian anggota : anggotaPengajuanPenelitianDanPengabdians) {
			oleh = "";
			if (anggota.getMahasiswa() != null) {
				oleh = (anggota.getMahasiswa().getNim() + " " + anggota.getMahasiswa().getNama());
			} else if (anggota.getTbmuser() != null) {
				oleh = (anggota.getTbmuser().getUserNama() + " (" + anggota.getTbmuser().getUserId() + ")");
			}
			new MyLabelKecil(i + ". " + oleh).setParent(vbox);
			i++;
		}

		Dosen dsn = pengajuanPenelitianDanPengabdian.getTbmuser() == null ? null
				: pengajuanPenelitianDanPengabdian.getTbmuser().getDosen();
		if ((dsn != null && dsn.yangLoginMerupakanAtasan()) || (!ases && (koresponden.contains(tbmuser.getUserId())
				|| korespondenGrup.contains(tbmuser.hakAkses().getRoleId())))) {
			final Combobox status = new Combobox();
			status.setParent(arg0);
			status.setWidth("90%");
			MyComboitemConfig comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.BELUM_DIPROSES);
			comboitem.setValue(PengajuanPenelitianDanPengabdian.BELUM_DIPROSES);
			status.appendChild(comboitem);

			comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.SEDANG_DIPROSES);
			comboitem.setValue(PengajuanPenelitianDanPengabdian.SEDANG_DIPROSES);
			status.appendChild(comboitem);

			comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.DISETUJUI);
			comboitem.setValue(PengajuanPenelitianDanPengabdian.DISETUJUI);
			status.appendChild(comboitem);

			comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.DITOLAK);
			comboitem.setValue(PengajuanPenelitianDanPengabdian.DITOLAK);
			status.appendChild(comboitem);

			Common.selectComboItem(status, pengajuanPenelitianDanPengabdian.getStatus());
			status.setReadonly(true);

			status.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					HibernateUtil.currentSession().refresh(pengajuanPenelitianDanPengabdian);
					pengajuanPenelitianDanPengabdian.setStatus(
							(String) (status.getSelectedItem() == null || status.getSelectedItem().getValue() == null
									? null
									: status.getSelectedItem().getValue()));
					Common.refreshUpdate(pengajuanPenelitianDanPengabdian);
				}
			});

			final Combobox tahapPengajuan = new Combobox();
			tahapPengajuan.setParent(arg0);
			tahapPengajuan.setWidth("90%");
			comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.TAHAP_PROPOSAL);
			comboitem.setValue(PengajuanPenelitianDanPengabdian.TAHAP_PROPOSAL);
			tahapPengajuan.appendChild(comboitem);

			comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.TAHAP_PENGUMPULAN_DATA);
			comboitem.setValue(PengajuanPenelitianDanPengabdian.TAHAP_PENGUMPULAN_DATA);
			tahapPengajuan.appendChild(comboitem);

			comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.TAHAP_ANALISIS_DATA);
			comboitem.setValue(PengajuanPenelitianDanPengabdian.TAHAP_ANALISIS_DATA);
			tahapPengajuan.appendChild(comboitem);

			comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.TAHAP_LAPORAN_AKHIR);
			comboitem.setValue(PengajuanPenelitianDanPengabdian.TAHAP_LAPORAN_AKHIR);
			tahapPengajuan.appendChild(comboitem);

			Common.selectComboItem(tahapPengajuan, pengajuanPenelitianDanPengabdian.getTahapPengajuan());
			tahapPengajuan.setReadonly(true);

			tahapPengajuan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pengajuanPenelitianDanPengabdian
							.setTahapPengajuan((String) (tahapPengajuan.getSelectedItem() == null ? null
									: tahapPengajuan.getSelectedItem().getValue()));
					Common.refreshUpdate(pengajuanPenelitianDanPengabdian);
				}
			});

		} else {
			if (ases) {
				vboxKeterangan.setParent(arg0);
				keteranganEventListener.onEvent(null);
				new Label(pengajuanPenelitianDanPengabdian.getTahapPengajuan()).setParent(arg0);
			} else {
				new Label(pengajuanPenelitianDanPengabdian.getStatus()).setParent(arg0);
				new Label(pengajuanPenelitianDanPengabdian.getTahapPengajuan()).setParent(arg0);
			}
		}
//		return content;
	}

	class DetailPengajuanPenelitianDanPengabdianRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");

			final PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian = (PengajuanPenelitianDanPengabdian) arg1;
			final TipePenelitianDanPengabdian jenis = pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian()
					.getTipePenelitianDanPengabdian();
			PengajuanPenelitianDanPengabdianHelper.displayRow(arg0, pengajuanPenelitianDanPengabdian,
					tbmuser == null ? null : tbmuser.ambilPegawai(), ases, jenis);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

//			if (content != null && content.getPath() != null) {
//				File file = new File(content.getPath());
//
//				Hbox hbox = new Hbox();
//				hbox.setParent(vbox);
//				MyToolbarbutton toolbarbutton = new MyToolbarbutton(FileFoto.iconAwesome(file.getName()),
//						file.getName());
//
//				toolbarbutton.setParent(hbox);
//				toolbarbutton.setVisible(content != null);
//				toolbarbutton.addEventListener("onClick", new EventListener() {
//					@Override
//					public void onEvent(Event event) throws Exception {
//						File file = new File(content.getPath());
//						Filedownload.save(file, content.getMimeType());
//					}
//
//				});
//			}

			Hbox hbox = new Hbox();
			hbox.setParent(vbox);
			MyToolbarbutton toolbarbutton = new MyToolbarbutton("fa-pencil-square-o", "Ubah");
			toolbarbutton.setParent(hbox);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
					save.setAttribute("parent", ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					form(pengajuanPenelitianDanPengabdian, null, save, null);

				}

			});

			toolbarbutton = new MyToolbarbutton("fa-trash", "Hapus");
			toolbarbutton.setDisabled(
					pengajuanPenelitianDanPengabdian.getStatus().equals(PengajuanPenelitianDanPengabdian.DISETUJUI));
			toolbarbutton.setTooltiptext("Hapus Data");
			toolbarbutton.setParent(hbox);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(pengajuanPenelitianDanPengabdian);

											loadDataPengajuan();
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}

			});
		}

	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public Boolean getReadonly() {
		return readonly;
	}

	@Override
	public void onSearchDefault(Event event) {
		loadDataPengajuan();
	}

}
