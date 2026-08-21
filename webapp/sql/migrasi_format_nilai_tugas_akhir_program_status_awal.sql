alter table public.format_nilai_skripsi
	add column if not exists program varchar(50);

alter table public.format_nilai_skripsi
	add column if not exists status_awal_mahasiswa int8;

alter table public.format_nilai_proposal_skripsi
	add column if not exists program varchar(50);

alter table public.format_nilai_proposal_skripsi
	add column if not exists status_awal_mahasiswa int8;

do $$
begin
	if not exists (
		select 1
		from pg_constraint
		where conname = 'fk_format_nilai_skripsi_status_awal_mahasiswa'
	) then
		alter table public.format_nilai_skripsi
			add constraint fk_format_nilai_skripsi_status_awal_mahasiswa
			foreign key (status_awal_mahasiswa) references public.status_awal_mahasiswa(id);
	end if;

	if not exists (
		select 1
		from pg_constraint
		where conname = 'fk_format_nilai_proposal_skripsi_status_awal_mahasiswa'
	) then
		alter table public.format_nilai_proposal_skripsi
			add constraint fk_format_nilai_proposal_skripsi_status_awal_mahasiswa
			foreign key (status_awal_mahasiswa) references public.status_awal_mahasiswa(id);
	end if;
end $$;
