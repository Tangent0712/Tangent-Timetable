INSERT IGNORE INTO api_key (api_key, label) VALUES ('demo-key-001', '默认用户');

INSERT IGNORE INTO period_config (period_number, start_time, end_time, category) VALUES
(1, '08:00', '08:45', 'MORNING'),
(2, '08:50', '09:35', 'MORNING'),
(3, '09:50', '10:35', 'MORNING'),
(4, '10:40', '11:25', 'MORNING'),
(5, '11:30', '12:15', 'MORNING'),
(6, '13:45', '14:30', 'AFTERNOON'),
(7, '14:35', '15:20', 'AFTERNOON'),
(8, '15:35', '16:20', 'AFTERNOON'),
(9, '16:25', '17:10', 'AFTERNOON'),
(10, '18:30', '19:15', 'EVENING'),
(11, '19:25', '20:10', 'EVENING'),
(12, '20:20', '21:05', 'EVENING');
