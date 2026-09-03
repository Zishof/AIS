package ais.action.master.sekolah.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ParameterTambahan;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.KegiatanSiswa;
import ais.database.model.sekolah.KelompokKegiatanSiswa;
import ais.database.model.sekolah.ParameterTambahanKegiatanSiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

/**
 * Listener ZK yang membangun dan mengelola baris-baris <b>parameter tambahan dinamis</b>
 * (dikonfigurasi admin, dapat bertingkat/hierarkis lewat relasi parent-child) pada layar kegiatan
 * siswa ({@link KegiatanSiswa}). Berbeda dari
 * {@link ParameterTambahanCatatanKelasSiswaListener} sejenisnya, kelas ini mendukung: (1) parameter
 * anak bersarang (rekursif lewat {@link #displayRinci}) yang muncul di grid indentasi begitu
 * parameter induknya memiliki anak terdaftar; (2) <b>syarat tampil</b> (skip-logic) — parameter dapat
 * disembunyikan/dilewati validasi berdasarkan jawaban parameter lain, dievaluasi lewat
 * {@code ais.common.ParameterTambahanHtmlHelper#lolosSyaratTampil}. Validasi statis
 * {@link #validate} dipanggil terpisah dari instance (dipakai saat form disimpan) dan turut
 * menghormati syarat tampil agar parameter tersembunyi tidak memblok penyimpanan.
 */
public class ParameterTambahanKegiatanSiswaListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private KegiatanSiswa kegiatanSiswa;
	private Map<String, LampiranLain> lampiranLains;

	/**
	 * Membuat listener yang akan membangun baris parameter tambahan ke dalam {@code rows} saat
	 * dipicu (event membawa {@link KelompokKegiatanSiswa} target), dan menyimpan isiannya kembali
	 * ke {@code kegiatanSiswa}.
	 *
	 * @param kegiatanSiswa   entitas kegiatan siswa yang sedang diedit
	 * @param parameterRows   list baris form parameter tambahan yang dikelola listener ini, dimutasi langsung
	 * @param lampiranLains   peta lampiran yang sudah diunggah, berkunci {@code "idKelompok->idParameter"}
	 * @param rows            komponen {@link Rows} tempat baris form ditambahkan
	 */
	public ParameterTambahanKegiatanSiswaListener(KegiatanSiswa kegiatanSiswa, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.rows = rows;
		this.kegiatanSiswa = kegiatanSiswa;
		this.lampiranLains = lampiranLains;
	}

	/**
	 * Memvalidasi seluruh parameter tambahan kegiatan siswa yang aktif terhadap nilai tersimpan pada
	 * {@code kegiatanSiswa.getNilaiInds()}: parameter wajib diisi (yang lolos syarat tampil dan bukan
	 * bertipe {@code TIDAK_ADA}) harus memiliki nilai bukan kosong/{@code "null"}, dan parameter yang
	 * mensyaratkan lampiran wajib harus sudah memiliki {@link LampiranLain} tersimpan. Parameter yang
	 * tidak lolos syarat tampil (skip-logic) dilewati dari validasi wajib-isi.
	 *
	 * @param kegiatanSiswa   entitas kegiatan siswa yang divalidasi
	 * @param eventListener   listener yang dipanggil/diteruskan saat validasi gagal (mis. untuk menutup dialog setelah pesan ditutup)
	 * @param tampilMessage   bila {@code true}, tampilkan {@link MyMessageboxConfig} peringatan saat gagal; bila {@code false}, langsung panggil {@code eventListener} tanpa pesan
	 * @return {@code true} bila seluruh parameter valid; {@code false} pada pelanggaran pertama yang ditemukan
	 */
	@SuppressWarnings("unchecked")
	public static boolean validate(KegiatanSiswa kegiatanSiswa, EventListener eventListener,
			final Boolean tampilMessage) throws Exception {

		List<ParameterTambahanKegiatanSiswa> parameterTambahanKegiatanSiswas = HibernateUtil.currentSession()
				.createCriteria(ParameterTambahanKegiatanSiswa.class)
				.createAlias("parameterTambahan", "parameterTambahan")
				.createAlias("kelompokKegiatanSiswa", "kelompokKegiatanSiswa")

				.add(Restrictions.eq("parameterTambahan.aktif", true))
				.add(Restrictions.eq("kelompokKegiatanSiswa.aktif", true)).list();
		// System.out.println("parameterTambahanKegiatanSiswas => " + parameterTambahanKegiatanSiswas);
		// SYARAT TAMPIL (skip-logic): peta nilai jawaban per id-parameter untuk melewati validasi wajib
		// pada parameter yang TERSEMBUNYI (syarat tampil tak terpenuhi) agar tak memblok penyimpanan.
		java.util.Map<Long, String> nilaiByParamIdKgs = ais.common.ParameterTambahanHtmlHelper
				.petaNilaiDariInds(kegiatanSiswa.getNilaiInds());
		for (ParameterTambahanKegiatanSiswa parameterTambahanKegiatanSiswa : parameterTambahanKegiatanSiswas) {
			ParameterTambahan parameterTambahan = parameterTambahanKegiatanSiswa.getParameterTambahan();
			final boolean lolosSyaratKgs = ais.common.ParameterTambahanHtmlHelper.lolosSyaratTampil(parameterTambahan, nilaiByParamIdKgs);
			KelompokKegiatanSiswa kelompokKegiatanSiswa = parameterTambahanKegiatanSiswa.getKelompokKegiatanSiswa();
			if (parameterTambahan != null && kelompokKegiatanSiswa != null) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(KegiatanSiswa.class,
						kegiatanSiswa.getId(), kelompokKegiatanSiswa.getId() + "->" + parameterTambahan.getId());

				String val = "";
				String[] spl = kegiatanSiswa.getNilaiInds().split("\n");
				for (String d : spl) {
					String[] value = d.split("<=>");
					if (value[0].trim().equalsIgnoreCase(jenis)) {
						val = value.length > 1 ? value[1].trim() : "";
					}
				}

				boolean wajib = parameterTambahanKegiatanSiswa.getWajibDiisi()
						&& (parameterTambahanKegiatanSiswa.getParameterTambahan() != null
								&& !parameterTambahanKegiatanSiswa.getParameterTambahan().getTipeDataInputan()
										.equals(ParameterTambahan.TIDAK_ADA))
						&& (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"));
				System.out.println(
						"parameterTambahan => " + parameterTambahan + ", val => " + val + ", wajib => " + wajib);

				if (wajib && lolosSyaratKgs) {
					if (tampilMessage) {

						MyMessageboxConfig.show(
								"\"" + kelompokKegiatanSiswa.getNama() + "\" harus Anda lengkapi !\n\nPilihan \""
										+ parameterTambahan.getLabelInputan() + "\" harus dipilih",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, eventListener);
					} else {
						if (eventListener != null) {
							eventListener.onEvent(null);
						}
					}
					return false;
				}
				if (lolosSyaratKgs && parameterTambahan.getLampiranWajibDiisi()) {
					if (parameterTambahan.getHarusMenyertakanLampiran()) {

						LampiranLain lam = LampiranLain.ambil(kegiatanSiswa.getId(), jenis);

						if (lam == null) {
							if (tampilMessage) {
								MyMessageboxConfig.show(
										"\"" + kelompokKegiatanSiswa.getNama()
												+ "\" harus Anda lengkapi !\n\nUntuk pilihan \""
												+ parameterTambahan.getLabelInputan() + "\", lampiran harus di-upload",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
										eventListener);
							} else {
								if (eventListener != null) {
									eventListener.onEvent(null);
								}
							}
							return false;
						}

					}
				}
			}
		}
		return true;
	}

	/** Menulis kembali nilai-nilai parameter tambahan dari baris form saat ini ke {@code kegiatanSiswa}, dipanggil saat kegiatan siswa disimpan. */
	public void onSave(KegiatanSiswa kegiatanSiswa) {
		kegiatanSiswa.populateParameterTambahanKegiatanSiswa(parameterRows);
	}

	/** Memeriksa apakah ada minimal satu parameter tambahan kegiatan siswa yang aktif terdaftar di sistem (dipakai untuk menentukan apakah blok parameter tambahan perlu ditampilkan sama sekali). */
	public boolean check() {
		int c = ((Number) HibernateUtil.currentSession().createCriteria(ParameterTambahanKegiatanSiswa.class)
				.createAlias("parameterTambahan", "parameterTambahan")
				.createAlias("kelompokKegiatanSiswa", "kelompokKegiatanSiswa")
				.add(Restrictions.eq("parameterTambahan.aktif", true))
				.add(Restrictions.eq("kelompokKegiatanSiswa.aktif", true)).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		System.out.println("Check alumni " + c);
		return c != 0;
	}

	/**
	 * Membangun ulang baris parameter tambahan untuk satu {@link KelompokKegiatanSiswa} (dibawa
	 * oleh {@code event.getData()}): menghapus baris lama, menambahkan baris judul kelompok, lalu
	 * mendelegasikan penyusunan baris parameter (termasuk yang bertingkat) ke {@link #displayRinci}.
	 *
	 * @param event event pemicu yang membawa {@link KelompokKegiatanSiswa} target sebagai data
	 */
	@SuppressWarnings({ "deprecation" })
	@Override
	public void onEvent(Event event) throws Exception {

		for (Row row : parameterRows) {
			row.setVisible(false);
		}
		parameterRows.clear();

		if (event.getData() instanceof KelompokKegiatanSiswa) {
			KelompokKegiatanSiswa kelompokKegiatanSiswa = (KelompokKegiatanSiswa) event.getData();
			if (kelompokKegiatanSiswa != null) {
				MyFormRow rowParameterTambahan = new MyFormRow();
				rowParameterTambahan.setStyle("border:0px;background: transparent;");
				rowParameterTambahan.setParent(rows);

				ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
				rowParameterTambahan.appendChild(new MyLabelStyled(kelompokKegiatanSiswa.getNama() + ""));
				parameterRows.add(rowParameterTambahan);

				displayRinci(rowParameterTambahan, rows, HibernateUtil.currentSession(), kelompokKegiatanSiswa, null,
						0);
			}
		}
	}

	/**
	 * Menyusun baris-baris parameter tambahan untuk {@code kelompokKegiatanSiswa} pada satu level
	 * hierarki (parameter dengan {@code parent} tertentu, atau parameter tanpa induk bila
	 * {@code parent} {@code null}), diurutkan sesuai {@link Comparable} bawaan
	 * {@link ParameterTambahanKegiatanSiswa}. Setiap parameter dievaluasi syarat tampilnya
	 * ({@code lolosSyaratTampil}) dan baris disembunyikan bila tidak lolos. Parameter yang memiliki
	 * anak (dideteksi lewat pemetaan {@code parent} pada seluruh {@link ParameterTambahan}) dirender
	 * dengan sub-grid berindentasi berisi baris anaknya, dibangun rekursif lewat panggilan
	 * {@code displayRinci} berikutnya dengan {@code parent} = parameter ini.
	 *
	 * @param rowParameterTambahan baris judul kelompok, visibilitasnya diperbarui bila ada parameter yang tampil
	 * @param rowsUtama             komponen {@link Rows} tempat baris parameter level ini ditambahkan
	 * @param session               sesi Hibernate aktif
	 * @param kelompokKegiatanSiswa kelompok parameter yang sedang disusun
	 * @param parent                parameter induk untuk level ini, atau {@code null} untuk level teratas
	 * @param indexParent           kedalaman rekursi saat ini (dipakai untuk pelacakan, tidak membatasi kedalaman)
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	private void displayRinci(Row rowParameterTambahan, Rows rowsUtama, Session session,
			KelompokKegiatanSiswa kelompokKegiatanSiswa, ParameterTambahan parent, int indexParent) {

		final List<ParameterTambahanKegiatanSiswa> parameterTambahanKegiatanSiswas = ConstantValues.simpleList(
				session.createCriteria(ParameterTambahanKegiatanSiswa.class)
						.add(Restrictions.eq("kelompokKegiatanSiswa", kelompokKegiatanSiswa))

						.createAlias("parameterTambahan", "parameterTambahan")
						.add(parent == null || parent.getId() == null ? Restrictions.isNull("parameterTambahan.parent")
								: Restrictions.eq("parameterTambahan.parent", parent))
						.createAlias("kelompokKegiatanSiswa", "kelompokKegiatanSiswa")

						.add(Restrictions.eq("parameterTambahan.aktif", true))
						.add(Restrictions.eq("kelompokKegiatanSiswa.aktif", true)),
				ParameterTambahanKegiatanSiswa.class);
		Collections.sort(parameterTambahanKegiatanSiswas);

		EventListener isi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				kegiatanSiswa.populateParameterTambahanKegiatanSiswa(parameterRows);
			}
		};

		System.out.println("parent -> " + parent + ", jumlah " + parameterTambahanKegiatanSiswas.size());
		boolean tampil = false;
		if (!parameterTambahanKegiatanSiswas.isEmpty()) {
			Map<Long, ParameterTambahan> mapdata = ConstantValues.ambilBerdasarClass(ParameterTambahan.class);
			// SYARAT TAMPIL (skip-logic): peta nilai jawaban per id-parameter untuk evaluasi kondisi tampil.
			java.util.Map<Long, String> nilaiByParamIdKgs = ais.common.ParameterTambahanHtmlHelper.petaNilaiDariInds(kegiatanSiswa.getNilaiInds());
			for (final ParameterTambahanKegiatanSiswa parameterTambahanKegiatanSiswa : parameterTambahanKegiatanSiswas) {
				ParameterTambahan parameterTambahan = parameterTambahanKegiatanSiswa.getParameterTambahan();
				final boolean lolosSyaratKgs = ais.common.ParameterTambahanHtmlHelper.lolosSyaratTampil(parameterTambahan, nilaiByParamIdKgs);

				boolean adaChild = false;
				for (ParameterTambahan p : mapdata.values()) {
					if (p.getParent() != null) {
						if (p.getParent().getId().equals(parameterTambahan.getId())) {
							adaChild = true;
							break;
						}
					}
				}

				String jenis = LampiranLain.resolveJenisParameterTambahan(KegiatanSiswa.class,
						kegiatanSiswa.getId(), kelompokKegiatanSiswa.getId() + "->" + parameterTambahan.getId());

				MyFormRow row = new MyFormRow();row.setValign("top");

				row.setValign("top");row.setAttribute("parameterTambahan", parameterTambahan);
				row.setValign("top");row.setAttribute("kelompokKegiatanSiswa", kelompokKegiatanSiswa);
				row.setParent(rowsUtama);
				row.appendChild(new Label(parameterTambahan.getLabelInputan()
						+ (parameterTambahanKegiatanSiswa.getWajibDiisi() ? " (*)" : " ")));
				if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
					parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
				}
				String val = "";
				String ket = "";
				String[] spl = kegiatanSiswa.getNilaiInds().split("\n");
				for (String d : spl) {
					String[] value = d.split("<=>");
					if (value[0].trim().equalsIgnoreCase(jenis)) {
						val = value.length > 1 ? value[1].trim() : "";
						try {
							ket = value.length > 0 ? value[value.length - 1] : "";
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/ParameterTambahanKegiatanSiswaListener.java:236");

						}
					}
				}

				boolean adaKomponenKgs = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
						kegiatanSiswa.getId(), val, ket, parameterTambahan, isi);
				row.setVisible(lolosSyaratKgs); // sembunyikan baris bila syarat tampil tak terpenuhi
				tampil |= (adaKomponenKgs && lolosSyaratKgs);

				Rows rows;
				if (adaChild) {
					MyFormRow row1 = new MyFormRow();
					ais.ui.util.ZkCompat.setSpans(row1, "2");
					row1.setParent(rowsUtama);
					row1.setVisible(lolosSyaratKgs); // ikut sembunyi bila induk parameter tak lolos syarat

					Grid grid = new Grid();grid.setSclass("dgrid");
					grid.setSclass("fgrid");
					grid.setStyle("padding-left: 50px;");
					grid.setParent(row1);

					Columns columns = new Columns();
					columns.setParent(grid);

					MyColumnConfig column = new MyColumnConfig();
					column.setParent(columns);
					column.setWidth("30%");

					column = new MyColumnConfig();
					column.setParent(columns);

					rows = new Rows();
					rows.setParent(grid);

				} else {
					rows = rowsUtama;
				}

				if (adaChild) {

					displayRinci(rowParameterTambahan, rows, session, kelompokKegiatanSiswa, parameterTambahan,
							indexParent + 1);
				}
			}
			rowParameterTambahan.setVisible(tampil);
		}
	}
}
