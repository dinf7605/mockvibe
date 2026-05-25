-- ============================================================================
-- V2: 종목 마스터 시드 (한국 30 + 미국 30 = 60종목)
-- - tick_size: KRX 1원 / 미국 0.01 (실제로는 가격대별로 다르지만 단순화)
-- - current_price: NULL로 두고 Provider(D07~D14)가 채운다
-- - 회사명에 '&'가 포함된 종목은 Oracle quote literal q'[...]' 로 escape하여
--   Flyway/SQL*Plus의 substitution variable(&xxx) 오해석을 방지한다.
--   (영향 4건: KT&G, Johnson & Johnson, Procter & Gamble, Merck & Co.)
-- ============================================================================

-- ===== 한국 30종목 (KRX) =====
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('005930', 'KRX', 'KRW', '삼성전자',           'Semiconductor', 'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('000660', 'KRX', 'KRW', 'SK하이닉스',          'Semiconductor', 'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('373220', 'KRX', 'KRW', 'LG에너지솔루션',      'Battery',       'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('207940', 'KRX', 'KRW', '삼성바이오로직스',    'Healthcare',    'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('005380', 'KRX', 'KRW', '현대차',              'Automobile',    'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('000270', 'KRX', 'KRW', '기아',                'Automobile',    'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('035420', 'KRX', 'KRW', 'NAVER',               'Internet',      'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('035720', 'KRX', 'KRW', '카카오',              'Internet',      'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('068270', 'KRX', 'KRW', '셀트리온',            'Healthcare',    'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('005490', 'KRX', 'KRW', 'POSCO홀딩스',         'Steel',         'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('105560', 'KRX', 'KRW', 'KB금융',              'Financial',     'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('055550', 'KRX', 'KRW', '신한지주',            'Financial',     'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('086790', 'KRX', 'KRW', '하나금융지주',        'Financial',     'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('028260', 'KRX', 'KRW', '삼성물산',            'Trading',       'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('012330', 'KRX', 'KRW', '현대모비스',          'Auto Parts',    'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('015760', 'KRX', 'KRW', '한국전력공사',        'Utilities',     'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('003550', 'KRX', 'KRW', 'LG',                  'Holding',       'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('017670', 'KRX', 'KRW', 'SK텔레콤',            'Telecom',       'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('030200', 'KRX', 'KRW', 'KT',                  'Telecom',       'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('051910', 'KRX', 'KRW', 'LG화학',              'Chemical',      'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('006400', 'KRX', 'KRW', '삼성SDI',             'Battery',       'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('096770', 'KRX', 'KRW', 'SK이노베이션',        'Energy',        'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('033780', 'KRX', 'KRW', q'[KT&G]',            'Consumer',      'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('009150', 'KRX', 'KRW', '삼성전기',            'Electronics',   'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('010130', 'KRX', 'KRW', '고려아연',            'Materials',     'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('011200', 'KRX', 'KRW', 'HMM',                 'Shipping',      'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('010950', 'KRX', 'KRW', 'S-Oil',               'Energy',        'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('251270', 'KRX', 'KRW', '넷마블',              'Internet',      'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('036570', 'KRX', 'KRW', '엔씨소프트',          'Internet',      'KR', 1);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('066570', 'KRX', 'KRW', 'LG전자',              'Electronics',   'KR', 1);

-- ===== 미국 30종목 (NASDAQ/NYSE) =====
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('AAPL',  'NASDAQ', 'USD', 'Apple Inc.',              'Technology',    'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('MSFT',  'NASDAQ', 'USD', 'Microsoft Corp.',         'Technology',    'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('NVDA',  'NASDAQ', 'USD', 'NVIDIA Corp.',            'Technology',    'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('AMZN',  'NASDAQ', 'USD', 'Amazon.com Inc.',         'Consumer',      'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('GOOGL', 'NASDAQ', 'USD', 'Alphabet Inc.',           'Technology',    'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('META',  'NASDAQ', 'USD', 'Meta Platforms Inc.',     'Technology',    'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('TSLA',  'NASDAQ', 'USD', 'Tesla Inc.',              'Automobile',    'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('LLY',   'NYSE',   'USD', 'Eli Lilly and Co.',       'Healthcare',    'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('JPM',   'NYSE',   'USD', 'JPMorgan Chase & Co.',    'Financial',     'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('V',     'NYSE',   'USD', 'Visa Inc.',               'Financial',     'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('UNH',   'NYSE',   'USD', 'UnitedHealth Group',      'Healthcare',    'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('JNJ',   'NYSE',   'USD', q'[Johnson & Johnson]',    'Healthcare',    'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('MA',    'NYSE',   'USD', 'Mastercard Inc.',         'Financial',     'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('XOM',   'NYSE',   'USD', 'Exxon Mobil Corp.',       'Energy',        'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('PG',    'NYSE',   'USD', q'[Procter & Gamble]',     'Consumer',      'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('AVGO',  'NASDAQ', 'USD', 'Broadcom Inc.',           'Technology',    'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('HD',    'NYSE',   'USD', 'Home Depot Inc.',         'Consumer',      'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('COST',  'NASDAQ', 'USD', 'Costco Wholesale',        'Consumer',      'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('MRK',   'NYSE',   'USD', q'[Merck & Co.]',          'Healthcare',    'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('ABBV',  'NYSE',   'USD', 'AbbVie Inc.',             'Healthcare',    'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('WMT',   'NYSE',   'USD', 'Walmart Inc.',            'Consumer',      'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('KO',    'NYSE',   'USD', 'Coca-Cola Co.',           'Consumer',      'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('PEP',   'NASDAQ', 'USD', 'PepsiCo Inc.',            'Consumer',      'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('DIS',   'NYSE',   'USD', 'Walt Disney Co.',         'Entertainment', 'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('NFLX',  'NASDAQ', 'USD', 'Netflix Inc.',            'Entertainment', 'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('AMD',   'NASDAQ', 'USD', 'Advanced Micro Devices',  'Technology',    'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('INTC',  'NASDAQ', 'USD', 'Intel Corp.',             'Technology',    'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('CSCO',  'NASDAQ', 'USD', 'Cisco Systems',           'Technology',    'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('ADBE',  'NASDAQ', 'USD', 'Adobe Inc.',              'Technology',    'US', 0.01);
INSERT INTO STOCKS (ticker, market, currency, company_name, sector, region, tick_size) VALUES ('CRM',   'NYSE',   'USD', 'Salesforce Inc.',         'Technology',    'US', 0.01);
