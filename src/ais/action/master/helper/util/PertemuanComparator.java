package ais.action.master.helper.util;

import java.io.Serializable;
import java.util.Comparator;

import ais.database.model.Pertemuan;

public class PertemuanComparator implements Comparator<Pertemuan>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2877567403893379767L;

	@Override
	public int compare(Pertemuan o1, Pertemuan o2) {
		Pertemuan data = (Pertemuan) o1;
		Pertemuan data2 = (Pertemuan) o2;

		String matkul1 = data.getPerkuliahan().getMatakuliah() == null ? ""
				: data.getPerkuliahan().getMatakuliah().getNama();
		String matkul2 = data2.getPerkuliahan().getMatakuliah() == null ? ""
				: data2.getPerkuliahan().getMatakuliah().getNama();

		Integer semester1 = data.getPerkuliahan().getSemester() == null ? 0
				: data.getPerkuliahan().getSemester();
		Integer semester2 = data2.getPerkuliahan().getSemester() == null ? 0
				: data2.getPerkuliahan().getSemester();

		String kelas1 = data.getPerkuliahan().getKelas();
		String kelas2 = data2.getPerkuliahan().getKelas();

		String dosen1 = data.getPerkuliahan().getDosen1() == null ? "" : data
				.getPerkuliahan().getDosen1().getNama();
		String dosen2 = data2.getPerkuliahan().getDosen1() == null ? "" : data2
				.getPerkuliahan().getDosen1().getNama();

		String str1 = matkul1 + semester1 + kelas1 + dosen1 + "___"
				+ data.getPerkuliahan().getId();
		String str2 = matkul2 + semester2 + kelas2 + dosen2 + "___"
				+ data2.getPerkuliahan().getId();

		return str1.compareTo(str2);
	}

}
