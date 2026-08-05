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

public class ParameterTambahanKegiatanSiswaListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private KegiatanSiswa kegiatanSiswa;
	private Map<String, LampiranLain> lampiranLains;

	public ParameterTambahanKegiatanSiswaListener(KegiatanSiswa kegiatanSiswa, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.rows = rows;
		this.kegiatanSiswa = kegiatanSiswa;
		this.lampiranLains = lampiranLains;
	}

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
				String jenis = kelompokKegiatanSiswa.getId() + "->" + parameterTambahan.getId();

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

	public void onSave(KegiatanSiswa kegiatanSiswa) {
		kegiatanSiswa.populateParameterTambahanKegiatanSiswa(parameterRows);
	}

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

				String jenis = kelompokKegiatanSiswa.getId() + "->" + parameterTambahan.getId();

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
