## 환율 Open API 실시간 호출 프로젝트

### 사용자 문제 인식
- 증권 커뮤니티에서 '서비스마다 환율이 다르다'는 의견 파악
- 대부분 서비스는 일정 주기로 환율을 캐싱하는 구조 -> 시차로 인해 값 차이 발생
- 해당 프로젝트에서 '**사용자 요청 시 실시간 OPEN API 호출**'방식으로 설계해 장단점 파악
- 결론은 실시간성 대신 서버 안정성을 위해 스케줄링 기반 캐싱이 더 나은 선택지일 수 있음을 파악함

## Single-Flight 패턴 기반 설계

![](/img/exchange-rate-api-design.png)

### Leader만 Open API 호출 가능
- 동일 시점에 여러 요청이 Open API로 USD/KRW 환율 요청 -> 같은 데이터를 가져올 가능성 높음
- Open API 다수 호출 시 호출 제한 걸려 Forbidden 에러 응답받으면, 일정 시간 동안 실시간 환율 데이터 응답 불가 -> 호출 제한 필요

### Follower 스레드 대기 방식
- ReentrantLock을 획득하지 못한 스레드는 로컬 캐시 업데이트를 기다려야 함
- 로컬 캐시 업데이트까지 대기 방식
  - Spin Lock: 데이터를 기다리는 상태에서 CPU 선점할 필요 없음
  - Condition.await: 락을 획득해야만 블록 내에서 대기 가능하므로, 결국 다수의 스레드는 데이터를 받기 위해 대기하는 것이 아니라 락 획득을 위해 대기하는 상태가 됨
  - Object.wait: synchronized 블록 사용 시 내부에서 모니터 락 자동 사용하므로 세심한 제어 불가능
  - ✔️ CompletableFuture.get(orTimeout): 락 없이 대기 가능 & 대기 상태까지 CPU 선점하지 않음

### 설계 결론
- 락 사용을 피할 수 있는 대기 상태 전환 방식을 찾았지만...
- 다수 서블릿 스레드가 대기 상태로 전환하는 것이 빈번히 발생 → Context Switching 비용 증가
- ~~외부 API 응답 timeout 잘못 설정 시~~ 대기 상태 스레드 누적 시 Tomcat 신규 요청 차단으로 이어질 수 있음
- 실시간 환율 제공보다 일정 주기로 캐싱되는 방식이 현실적인 대안임을 파악

## 단계적 호출 및 CompletableFuture 기반 Open API 병렬 호출
- 호출 제한에 대비하여 단계적 호출 설계
- 1차 호출 (sync): [naver Open API](https://m.search.naver.com/p/csearch/content/qapirender.nhn?key=calculator&pkid=141&q=%ED%99%98%EC%9C%A8&where=m&u1=keb&u6=standardUnit&u7=0&u3=USD&u4=KRW&u8=down&u2=1)
- 2차 호출 (async): [manana Open API](https://api.manana.kr/exchange) & [구글 web scraping](https://www.google.com/finance/quote/USD-KRW) 병렬 수행 
- 1차 호출에서 이미 시간 소요되었기에, 2차 호출에서는 동시 호출해 먼저 오는 응답으로 로컬 캐시 업데이트 (CompletableFuture.anyOf)