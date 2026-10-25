-- Real reference data. Airport coordinates are approximate.

INSERT INTO currencies (code, name, symbol, decimals) VALUES
 ('TND', 'Dinar tunisien', 'DT', 3),
 ('EUR', 'Euro',           '€',  2),
 ('USD', 'Dollar américain', '$', 2);

INSERT INTO vehicle_types (code, name_fr, name_en, name_ar, min_passengers, max_passengers, display_order) VALUES
 ('SEDAN',   'Berline', 'Sedan',   'سيارة سيدان', 1, 3,  1),
 ('VAN',     'Van',     'Van',     'فان',          4, 7,  2),
 ('MINIBUS', 'Minibus', 'Minibus', 'ميني باص',     8, 16, 3);

INSERT INTO airports (iata_code, name_fr, name_en, name_ar, latitude, longitude) VALUES
 ('TUN', 'Tunis-Carthage',           'Tunis-Carthage',           'تونس قرطاج',           36.851000, 10.227200),
 ('MIR', 'Monastir Habib Bourguiba', 'Monastir Habib Bourguiba', 'المنستير الحبيب بورقيبة', 35.758100, 10.754700),
 ('NBE', 'Enfidha-Hammamet',         'Enfidha-Hammamet',         'النفيضة الحمامات',      36.075800, 10.438600),
 ('DJE', 'Djerba-Zarzis',            'Djerba-Zarzis',            'جربة جرجيس',           33.875000, 10.775500),
 ('SFA', 'Sfax-Thyna',               'Sfax-Thyna',               'صفاقس طينة',           34.717900, 10.690800),
 ('TOE', 'Tozeur-Nefta',             'Tozeur-Nefta',             'توزر نفطة',            33.939700,  8.110600),
 ('TBJ', 'Tabarka-Aïn Draham',       'Tabarka-Aïn Draham',       'طبرقة عين دراهم',      36.978300,  8.876900),
 ('GAF', 'Gafsa-Ksar',               'Gafsa-Ksar',               'قفصة القصر',           34.422000,  8.822500);

-- 15 pricing zones, built from governorates (Médenine is split Djerba / mainland).
INSERT INTO zones (code, name_fr, name_en, name_ar, display_order) VALUES
 ('Z01', 'Grand Tunis',         'Greater Tunis',        'تونس الكبرى',   1),
 ('Z02', 'Cap Bon',             'Cap Bon',              'الوطن القبلي',   2),
 ('Z03', 'Zaghouan',            'Zaghouan',             'زغوان',          3),
 ('Z04', 'Bizerte',             'Bizerte',              'بنزرت',          4),
 ('Z05', 'Nord-Ouest',          'North-West',           'الشمال الغربي',  5),
 ('Z06', 'Sahel',               'Sahel',                'الساحل',         6),
 ('Z07', 'Kairouan',            'Kairouan',             'القيروان',       7),
 ('Z08', 'Centre-Ouest',        'Central-West',         'الوسط الغربي',   8),
 ('Z09', 'Sfax',                'Sfax',                 'صفاقس',          9),
 ('Z10', 'Gabès',               'Gabès',                'قابس',          10),
 ('Z11', 'Gafsa',               'Gafsa',                'قفصة',          11),
 ('Z12', 'Sud-Ouest',           'South-West',           'الجنوب الغربي', 12),
 ('Z13', 'Djerba',              'Djerba',               'جربة',          13),
 ('Z14', 'Médenine continent',  'Médenine mainland',    'مدنين البر',    14),
 ('Z15', 'Tataouine',           'Tataouine',            'تطاوين',        15);

INSERT INTO areas (code, name_fr, name_en, name_ar, zone_id)
SELECT a.code, a.name_fr, a.name_en, a.name_ar, z.id
FROM (VALUES
 ('TN-11',        'Tunis',                'Tunis',             'تونس',          'Z01'),
 ('TN-12',        'Ariana',               'Ariana',            'أريانة',        'Z01'),
 ('TN-13',        'Ben Arous',            'Ben Arous',         'بن عروس',       'Z01'),
 ('TN-14',        'La Manouba',           'Manouba',           'منوبة',         'Z01'),
 ('TN-21',        'Nabeul',               'Nabeul',            'نابل',          'Z02'),
 ('TN-22',        'Zaghouan',             'Zaghouan',          'زغوان',         'Z03'),
 ('TN-23',        'Bizerte',              'Bizerte',           'بنزرت',         'Z04'),
 ('TN-31',        'Béja',                 'Béja',              'باجة',          'Z05'),
 ('TN-32',        'Jendouba',             'Jendouba',          'جندوبة',        'Z05'),
 ('TN-33',        'Le Kef',               'El Kef',            'الكاف',         'Z05'),
 ('TN-34',        'Siliana',              'Siliana',           'سليانة',        'Z05'),
 ('TN-51',        'Sousse',               'Sousse',            'سوسة',          'Z06'),
 ('TN-52',        'Monastir',             'Monastir',          'المنستير',      'Z06'),
 ('TN-53',        'Mahdia',               'Mahdia',            'المهدية',       'Z06'),
 ('TN-41',        'Kairouan',             'Kairouan',          'القيروان',      'Z07'),
 ('TN-42',        'Kasserine',            'Kasserine',         'القصرين',       'Z08'),
 ('TN-43',        'Sidi Bouzid',          'Sidi Bouzid',       'سيدي بوزيد',    'Z08'),
 ('TN-61',        'Sfax',                 'Sfax',              'صفاقس',         'Z09'),
 ('TN-81',        'Gabès',                'Gabès',             'قابس',          'Z10'),
 ('TN-71',        'Gafsa',                'Gafsa',             'قفصة',          'Z11'),
 ('TN-72',        'Tozeur',               'Tozeur',            'توزر',          'Z12'),
 ('TN-73',        'Kébili',               'Kebili',            'قبلي',          'Z12'),
 ('TN-82-DJERBA', 'Médenine — Djerba',    'Médenine — Djerba', 'مدنين — جربة',  'Z13'),
 ('TN-82-MAIN',   'Médenine — continent', 'Médenine — mainland','مدنين — البر', 'Z14'),
 ('TN-83',        'Tataouine',            'Tataouine',         'تطاوين',        'Z15')
) AS a (code, name_fr, name_en, name_ar, zone_code)
JOIN zones z ON z.code = a.zone_code;
