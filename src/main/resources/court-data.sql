-- Court 샘플 데이터 (중복 시 업데이트)
INSERT INTO court (court_id, court_name, location, latitude, longitude, img_url) VALUES
(1, '서울숲 테니스장', '서울특별시 성동구 뚝섬로 273', 37.5447, 127.0397, 'https://img.kr.gcp-karroter.net/business/bizPlatform/profile/55472241/1726494505504/MThiNjIzY2MyNWZiYzk1ODUyNWFkZDM5ZTVlNmFiZTIzNWJjMGRmMDMxMzdmNjg4ZDg0MTRlOTQ2ZmY3ZDBlM18wLmpwZWc=.jpeg?q=95&s=1200x630&t=cover'),
(2, '올림픽공원 테니스장', '서울특별시 송파구 올림픽로 424', 37.5219, 127.1231, 'https://images.khan.co.kr/article/2024/09/25/l_2024092502000112300062631.jpg'),
(3, '양재시민의숲 테니스장', '서울특별시 서초구 매헌로 99', 37.4712, 127.0358, 'https://sports.seoul.go.kr/file/view/FID00000322/1.do'),
(4, '월드컵공원 테니스장', '서울특별시 마포구 월드컵로 243', 37.5682, 126.8973, 'https://lh3.googleusercontent.com/proxy/96v5anJ83eU1kD82OnzcHCj8RZQJ1aoxvgTkXc4lTw0a49YdWvd0zArr1AEQE4ohPj34u87gCoPFU2eDP4QlDYXxNUaSk04M6oK4itH57sqf_7l8Ejuz'),
(5, '탄천 테니스장', '경기도 성남시 분당구 야탑동', 37.4109, 127.1284, 'https://asset.smaxh.com/ennisCourt/2022/06/12/F0B7FD09-C8E1-4788-9DD9-86B5E24AD322.jpeg')
ON DUPLICATE KEY UPDATE
court_name = VALUES(court_name),
location = VALUES(location),
latitude = VALUES(latitude),
longitude = VALUES(longitude),
img_url = VALUES(img_url);