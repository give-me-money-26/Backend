insert into buildings (id, name, code, map_x, map_y, priority_rank, model_type, advice, data_status) values
('KIM-SOU-HWAN', '김수환관', 'K관', 34, 69, 4, 'TOWER', '배기구 주변 장애물을 줄이고 정면 흐름을 활용하는 배치를 검토하세요.', 'TEST_DATA'),
('DASOL', '다솔관', 'D관', 32, 36, 2, 'COURTYARD', '경사 지형과 연결동 사이의 흐름을 고려해 배기구 전면 배치를 비교하세요.', 'TEST_DATA'),
('NICHOLLS', '니콜스관', 'N관', 51, 53, 1, 'LONG', '예시 밀도가 높은 오후 2~4시를 기준으로 소형 터빈 배치를 검토하세요.', 'TEST_DATA'),
('MARIA', '마리아관', 'M관', 55, 38, 3, 'WING', '건물 사이 통로와 주변 흐름을 함께 살펴 배기구와 터빈의 거리를 조절하세요.', 'TEST_DATA'),
('STUDENT-HALL', '학관', '학생미래인재관', 58, 22, 5, 'HALL', '대규모 발전보다 소형 센서와 환경 안내 장치의 보조 전원 활용을 검토하세요.', 'TEST_DATA');

insert into hourly_wind_densities (building_id, hour_of_day, density_w_m2) values
('KIM-SOU-HWAN',10,3),('KIM-SOU-HWAN',11,4.4),('KIM-SOU-HWAN',12,6.2),('KIM-SOU-HWAN',13,9),('KIM-SOU-HWAN',14,12.1),('KIM-SOU-HWAN',15,13.5),('KIM-SOU-HWAN',16,11.4),('KIM-SOU-HWAN',17,8.3),('KIM-SOU-HWAN',18,5.8),('KIM-SOU-HWAN',19,3.9),('KIM-SOU-HWAN',20,2.8),('KIM-SOU-HWAN',21,1.9),('KIM-SOU-HWAN',22,1.2),
('DASOL',10,3.8),('DASOL',11,5.5),('DASOL',12,7.8),('DASOL',13,11.4),('DASOL',14,15.2),('DASOL',15,17.1),('DASOL',16,14.5),('DASOL',17,10.4),('DASOL',18,7.2),('DASOL',19,5),('DASOL',20,3.4),('DASOL',21,2.4),('DASOL',22,1.6),
('NICHOLLS',10,4.2),('NICHOLLS',11,6.1),('NICHOLLS',12,8.4),('NICHOLLS',13,12.1),('NICHOLLS',14,16.8),('NICHOLLS',15,18.9),('NICHOLLS',16,15.7),('NICHOLLS',17,11.6),('NICHOLLS',18,8),('NICHOLLS',19,5.3),('NICHOLLS',20,3.8),('NICHOLLS',21,2.7),('NICHOLLS',22,1.8),
('MARIA',10,3.9),('MARIA',11,5.7),('MARIA',12,7.9),('MARIA',13,11),('MARIA',14,15),('MARIA',15,16.6),('MARIA',16,14.1),('MARIA',17,10),('MARIA',18,7),('MARIA',19,4.8),('MARIA',20,3.5),('MARIA',21,2.5),('MARIA',22,1.6),
('STUDENT-HALL',10,2.2),('STUDENT-HALL',11,3.1),('STUDENT-HALL',12,4.2),('STUDENT-HALL',13,6),('STUDENT-HALL',14,8),('STUDENT-HALL',15,8.9),('STUDENT-HALL',16,7.4),('STUDENT-HALL',17,5.5),('STUDENT-HALL',18,3.9),('STUDENT-HALL',19,2.7),('STUDENT-HALL',20,1.9),('STUDENT-HALL',21,1.3),('STUDENT-HALL',22,0.9);

