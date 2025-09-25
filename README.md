## 환율 Open API 실시간 호출 프로젝트

### 사용자 문제 인식
- 증권 커뮤니티에서 '서비스마다 환율이 다르다'는 의견 파악
- 대부분 서비스는 일정 주기로 환율을 캐싱하는 구조 -> 시차로 인해 값 차이 발생
- 해당 프로젝트에서 '**사용자 요청 시 실시간 업데이트 방식**'으로 설계해 장단점 파악

## Single-Flight 패턴 기반 설계

![](/img/exchange-rate-api-design.png)

### Leader만 Open API 호출 가능
- 동일 시점에 여러 요청이 Open API로 USD/KRW 환율 요청 -> 같은 데이터를 가져올 가능성 높음
- Open API 다수 호출 시 호출 제한 걸려 Forbidden 에러 응답받으면, 일정 시간 동안 실시간 환율 데이터 응답 불가 -> 호출 제한 필요

### Follower 스레드 대기 방식
- ReentrantLock을 획득하지 못한 스레드는 로컬 캐시 업데이트를 기다려야 함
- 로컬 캐시 업데이트까지 대기 방식
  - Spin Lock: 대기 시간 예측이 불가능한 외부 API 호출 로직과 연관되어 불필요한 CPU 선점으로 이어질 수 있음
  - Condition.await: Follower 스레드는 공용 데이터 읽기 작업만 수행해 Lock 획득하면서까지 대기할 필요 없음
  - Object.wait: synchronized 블록 사용 시 내부에서 모니터 락 자동 사용하므로 세심한 제어 불가능
  - ✔️ CompletableFuture.get(orTimeout): 락 없이 대기 가능 & 대기 상태까지 CPU 선점하지 않음

### 단계적 호출 및 CompletableFuture 기반 Open API 병렬 호출
- 호출 제한에 대비하여 단계적 호출 설계
- 1차 호출 (sync): [naver Open API](https://m.search.naver.com/p/csearch/content/qapirender.nhn?key=calculator&pkid=141&q=%ED%99%98%EC%9C%A8&where=m&u1=keb&u6=standardUnit&u7=0&u3=USD&u4=KRW&u8=down&u2=1)
- 2차 호출 (async): [manana Open API](https://api.manana.kr/exchange) & [구글 web scraping](https://www.google.com/finance/quote/USD-KRW) 병렬 수행 
- 1차 호출에서 이미 시간 소요되었기에, 2차 호출에서는 동시 호출해 먼저 오는 응답으로 로컬 캐시 업데이트 (CompletableFuture.anyOf)