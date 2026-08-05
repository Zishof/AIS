package ais.action.master.recruitment.helper;

import java.util.List;

import ais.database.model.recruitment.CalonPegawai;

public interface NisGenerator {

	public String generateNis(CalonPegawai calonPegawai);

	public String generateNis(CalonPegawai calonPegawai, List<String> nimPengecualian);

}
