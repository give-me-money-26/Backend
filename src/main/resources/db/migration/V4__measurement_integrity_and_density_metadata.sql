alter table hourly_wind_densities add column density_unit varchar(20) not null default 'W_PER_M2';
alter table hourly_wind_densities add column source_title varchar(200);
alter table hourly_wind_densities add column source_document varchar(300);
alter table hourly_wind_densities add column data_status varchar(40) not null default 'PRELIMINARY';
alter table hourly_wind_densities add column derivation_method varchar(200);

update hourly_wind_densities
set source_type = 'PRELIMINARY_MEASUREMENT_PDF',
    source_title = '성심교정 5개 건물 10~22시 예비 측정 자료',
    source_document = '프로젝트 제공 PDF 표',
    data_status = 'PRELIMINARY',
    derivation_method = 'PDF에 제시된 풍력 에너지 밀도(W/m²)를 재계산 없이 저장';

update buildings set data_status = 'PRELIMINARY_DATA' where data_status = 'TEST_DATA';

alter table measurements add constraint fk_measurement_vent
    foreign key (vent_id) references vents(id);
alter table measurements add constraint fk_measurement_sensor
    foreign key (sensor_id) references sensors(id);

